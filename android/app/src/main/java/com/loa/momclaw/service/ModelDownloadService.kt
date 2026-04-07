package com.loa.momclaw.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.loa.momclaw.util.MomClawLogger
import androidx.core.app.NotificationCompat
import com.loa.momclaw.MainActivity
import com.loa.momclaw.data.download.ModelDownloadManager
import com.loa.momclaw.data.download.ModelMetadata
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Background Service for model downloads.
 * 
 * Features:
 * - Continues downloads in background
 * - Shows notification with progress
 * - Handles app closure gracefully
 * - Can pause/resume downloads
 */
@AndroidEntryPoint
class ModelDownloadService : Service() {
    
    @Inject
    lateinit var downloadManager: ModelDownloadManager
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val binder = LocalBinder()
    private val activeDownloads = mutableMapOf<String, Job>()
    
    private lateinit var notificationManager: NotificationManager
    
    companion object {
        private const val TAG = "ModelDownloadService"
    }
    
    private val logger = MomClawLogger
        private const val CHANNEL_ID = "model_download_channel"
        private const val CHANNEL_NAME = "Model Downloads"
        private const val NOTIFICATION_ID_BASE = 1000
        
        const val ACTION_START_DOWNLOAD = "com.loa.momclaw.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.loa.momclaw.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.loa.momclaw.CANCEL_DOWNLOAD"
        const val ACTION_STOP_SERVICE = "com.loa.momclaw.STOP_SERVICE"
        
        const val EXTRA_MODEL_NAMESPACE = "model_namespace"
        const val EXTRA_MODEL_REPO_ID = "model_repo_id"
        const val EXTRA_MODEL_FILENAME = "model_filename"
        const val EXTRA_MODEL_SIZE = "model_size"
        
        /**
         * Start download service.
         */
        fun startDownload(context: Context, metadata: ModelMetadata): Intent {
            return Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_MODEL_NAMESPACE, metadata.namespace)
                putExtra(EXTRA_MODEL_REPO_ID, metadata.repoId)
                putExtra(EXTRA_MODEL_FILENAME, metadata.filename)
                putExtra(EXTRA_MODEL_SIZE, metadata.sizeBytes)
            }
        }
        
        /**
         * Cancel a download.
         */
        fun cancelDownload(context: Context, modelId: String): Intent {
            return Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra("model_id", modelId)
            }
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): ModelDownloadService = this@ModelDownloadService
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        logger.d(TAG, "Service created")
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val metadata = ModelMetadata(
                    namespace = intent.getStringExtra(EXTRA_MODEL_NAMESPACE) ?: "",
                    repoId = intent.getStringExtra(EXTRA_MODEL_REPO_ID) ?: "",
                    filename = intent.getStringExtra(EXTRA_MODEL_FILENAME) ?: "",
                    sizeBytes = intent.getLongExtra(EXTRA_MODEL_SIZE, 0L),
                    sha256 = null,
                    downloadUrl = ModelMetadata.getDownloadUrl(
                        intent.getStringExtra(EXTRA_MODEL_NAMESPACE) ?: "",
                        intent.getStringExtra(EXTRA_MODEL_REPO_ID) ?: "",
                        intent.getStringExtra(EXTRA_MODEL_FILENAME) ?: ""
                    ),
                    huggingFaceUrl = ""
                )
                startModelDownload(metadata)
            }
            
            ACTION_PAUSE_DOWNLOAD -> {
                val modelId = intent.getStringExtra("model_id")
                modelId?.let { downloadManager.pauseDownload(it) }
            }
            
            ACTION_CANCEL_DOWNLOAD -> {
                val modelId = intent.getStringExtra("model_id")
                modelId?.let { cancelDownload(it) }
            }
            
            ACTION_STOP_SERVICE -> {
                stopAllDownloads()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    /**
     * Start downloading a model.
     */
    private fun startModelDownload(metadata: ModelMetadata) {
        val modelId = "${metadata.namespace}/${metadata.repoId}"
        
        // Check if already downloading
        if (activeDownloads.containsKey(modelId)) {
            logger.w(TAG, "Download already active: $modelId")
            return
        }
        
        // Start foreground service with initial notification
        startForeground(getNotificationId(modelId), createDownloadNotification(modelId, 0, metadata.sizeBytes))
        
        // Start download
        val job = serviceScope.launch {
            downloadManager.downloadModel(metadata).collect { progress ->
                updateDownloadNotification(modelId, progress)
                
                if (progress.isComplete || progress.isFailed) {
                    activeDownloads.remove(modelId)
                    
                    // Stop service if no more downloads
                    if (activeDownloads.isEmpty()) {
                        delay(2000) // Show completion notification briefly
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        
        activeDownloads[modelId] = job
    }
    
    /**
     * Cancel a specific download.
     */
    private fun cancelDownload(modelId: String) {
        activeDownloads[modelId]?.cancel()
        activeDownloads.remove(modelId)
        downloadManager.cancelDownload(modelId)
        
        notificationManager.cancel(getNotificationId(modelId))
        
        if (activeDownloads.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    /**
     * Stop all active downloads.
     */
    private fun stopAllDownloads() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        
        activeDownloads.keys.forEach { modelId ->
            downloadManager.cancelDownload(modelId)
            notificationManager.cancel(getNotificationId(modelId))
        }
    }
    
    /**
     * Create notification channel for Android O+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Model download progress notifications"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create download progress notification.
     */
    private fun createDownloadNotification(
        modelId: String,
        bytesDownloaded: Long,
        totalBytes: Long
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(this, ModelDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra("model_id", modelId)
        }
        
        val cancelPendingIntent = PendingIntent.getService(
            this,
            modelId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val percentComplete = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading Model")
            .setContentText("$percentComplete% complete")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )
            .setProgress(100, percentComplete, false)
            .build()
    }
    
    /**
     * Update download notification with progress.
     */
    private fun updateDownloadNotification(modelId: String, progress: ModelDownloadManager.DownloadProgress) {
        val notification = when {
            progress.isComplete -> {
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Download Complete")
                    .setContentText("${progress.modelId} downloaded successfully")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .build()
            }
            
            progress.isFailed -> {
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Download Failed")
                    .setContentText(progress.errorMessage ?: "Unknown error")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setOngoing(false)
                    .build()
            }
            
            else -> {
                createDownloadNotification(
                    modelId,
                    progress.bytesDownloaded,
                    progress.totalBytes
                )
            }
        }
        
        notificationManager.notify(getNotificationId(modelId), notification)
    }
    
    /**
     * Get unique notification ID for a model.
     */
    private fun getNotificationId(modelId: String): Int {
        return NOTIFICATION_ID_BASE + modelId.hashCode()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopAllDownloads()
        logger.d(TAG, "Service destroyed")
    }
}
