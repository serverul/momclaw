package com.loa.momclaw.data.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Model Download Manager with robust downloading capabilities.
 * 
 * Features:
 * - Progress tracking with Flow
 * - Resume capability (partial downloads)
 * - SHA-256 checksum verification
 * - Storage validation
 * - Retry logic with exponential backoff
 * - Automatic cleanup on failure
 */
class ModelDownloadManager(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private val downloadJobs = mutableMapOf<String, Job>()
    private val downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    
    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val BUFFER_SIZE = 8192
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val MIN_STORAGE_SPACE_GB = 5L
    }
    
    /**
     * Download progress information.
     */
    data class DownloadProgress(
        val modelId: String,
        val state: DownloadState,
        val bytesDownloaded: Long = 0,
        val totalBytes: Long = 0,
        val percentComplete: Int = 0,
        val bytesPerSecond: Long = 0,
        val errorMessage: String? = null,
        val filePath: String? = null
    ) {
        val isComplete: Boolean get() = state == DownloadState.COMPLETED
        val isFailed: Boolean get() = state == DownloadState.FAILED
        val isDownloading: Boolean get() = state == DownloadState.DOWNLOADING
        val isPaused: Boolean get() = state == DownloadState.PAUSED
        
        fun formatProgress(): String {
            val downloadedMB = bytesDownloaded / (1024 * 1024)
            val totalMB = totalBytes / (1024 * 1024)
            val speedMBps = bytesPerSecond / (1024 * 1024)
            
            return if (totalBytes > 0) {
                "$downloadedMB MB / $totalMB MB ($percentComplete%) - $speedMBps MB/s"
            } else {
                "$downloadedMB MB downloaded - $speedMBps MB/s"
            }
        }
    }
    
    enum class DownloadState {
        QUEUED, DOWNLOADING, PAUSED, VERIFYING, COMPLETED, FAILED, CANCELLED
    }
    
    /**
     * Get current download progress for all active downloads.
     */
    fun getDownloadProgress(): StateFlow<Map<String, DownloadProgress>> = downloadProgress.asStateFlow()
    
    /**
     * Get download progress for a specific model.
     */
    fun getDownloadProgress(modelId: String): DownloadProgress? {
        return downloadProgress.value[modelId]
    }
    
    /**
     * Start downloading a model.
     * 
     * @param metadata Model metadata with download URL
     * @param targetDirectory Directory to save the model (defaults to app's files dir)
     * @param verifyChecksum Whether to verify SHA-256 checksum after download
     * @return Flow of download progress
     */
    fun downloadModel(
        metadata: ModelMetadata,
        targetDirectory: File = File(context.filesDir, "models"),
        verifyChecksum: Boolean = true
    ): Flow<DownloadProgress> = flow {
        val modelId = "${metadata.namespace}/${metadata.repoId}"
        
        // Check if already downloading
        if (downloadJobs[modelId]?.isActive == true) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.FAILED,
                errorMessage = "Download already in progress"
            ))
            return@flow
        }
        
        // Validate storage space
        val availableSpace = targetDirectory.freeSpace
        val requiredSpace = metadata.sizeBytes + (1024 * 1024 * 1024) // 1GB buffer
        if (availableSpace < requiredSpace) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.FAILED,
                errorMessage = "Insufficient storage. Need ${(requiredSpace / (1024 * 1024 * 1024))}GB, have ${(availableSpace / (1024 * 1024 * 1024))}GB"
            ))
            return@flow
        }
        
        // Ensure directory exists
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        
        val targetFile = File(targetDirectory, metadata.filename)
        val tempFile = File(targetDirectory, "${metadata.filename}.tmp")
        
        // Resume support: check for partial download
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        
        emit(DownloadProgress(
            modelId = modelId,
            state = DownloadState.QUEUED,
            bytesDownloaded = existingBytes,
            totalBytes = metadata.sizeBytes
        ))
        
        // Start download with retry logic
        var retryCount = 0
        var lastException: Exception? = null
        
        while (retryCount < MAX_RETRIES) {
            try {
                val result = downloadWithProgress(
                    url = metadata.downloadUrl,
                    targetFile = tempFile,
                    existingBytes = existingBytes,
                    modelId = modelId
                ).collect { progress ->
                    emit(progress)
                    downloadProgress.value = downloadProgress.value + (modelId to progress)
                }
                
                // Download successful
                break
            } catch (e: Exception) {
                lastException = e
                retryCount++
                
                if (retryCount < MAX_RETRIES) {
                    Log.w(TAG, "Download failed (attempt $retryCount/$MAX_RETRIES), retrying...", e)
                    delay(RETRY_DELAY_MS * retryCount) // Exponential backoff
                } else {
                    Log.e(TAG, "Download failed after $MAX_RETRIES attempts", e)
                    cleanupDownload(tempFile)
                    
                    val errorProgress = DownloadProgress(
                        modelId = modelId,
                        state = DownloadState.FAILED,
                        errorMessage = "Download failed after $MAX_RETRIES attempts: ${e.message}"
                    )
                    emit(errorProgress)
                    downloadProgress.value = downloadProgress.value + (modelId to errorProgress)
                    return@flow
                }
            }
        }
        
        // Verify checksum if required
        if (verifyChecksum && metadata.sha256 != null) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.VERIFYING,
                bytesDownloaded = tempFile.length(),
                totalBytes = metadata.sizeBytes,
                percentComplete = 100
            ))
            
            val actualChecksum = calculateChecksum(tempFile)
            if (actualChecksum != metadata.sha256) {
                cleanupDownload(tempFile)
                val errorProgress = DownloadProgress(
                    modelId = modelId,
                    state = DownloadState.FAILED,
                    errorMessage = "Checksum verification failed. Expected ${metadata.sha256}, got $actualChecksum"
                )
                emit(errorProgress)
                downloadProgress.value = downloadProgress.value + (modelId to errorProgress)
                return@flow
            }
        }
        
        // Move temp file to final location
        tempFile.renameTo(targetFile)
        
        val successProgress = DownloadProgress(
            modelId = modelId,
            state = DownloadState.COMPLETED,
            bytesDownloaded = targetFile.length(),
            totalBytes = metadata.sizeBytes,
            percentComplete = 100,
            filePath = targetFile.absolutePath
        )
        
        emit(successProgress)
        downloadProgress.value = downloadProgress.value + (modelId to successProgress)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cancel an ongoing download.
     */
    fun cancelDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        
        val currentProgress = downloadProgress.value[modelId]
        if (currentProgress != null && currentProgress.isDownloading) {
            downloadProgress.value = downloadProgress.value + (modelId to currentProgress.copy(
                state = DownloadState.CANCELLED,
                errorMessage = "Download cancelled by user"
            ))
        }
    }
    
    /**
     * Pause an ongoing download.
     */
    fun pauseDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        
        val currentProgress = downloadProgress.value[modelId]
        if (currentProgress != null && currentProgress.isDownloading) {
            downloadProgress.value = downloadProgress.value + (modelId to currentProgress.copy(
                state = DownloadState.PAUSED
            ))
        }
    }
    
    /**
     * Internal download implementation with progress tracking.
     */
    private suspend fun downloadWithProgress(
        url: String,
        targetFile: File,
        existingBytes: Long,
        modelId: String
    ): Flow<DownloadProgress> = flow {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (existingBytes > 0) {
                    header("Range", "bytes=$existingBytes-")
                }
            }
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }
        
        val totalBytes = response.body?.contentLength() ?: 0L
        val inputStream = response.body?.byteStream() ?: throw IOException("Empty response body")
        
        var bytesDownloaded = existingBytes
        var startTime = System.currentTimeMillis()
        var bytesInLastSecond = 0L
        var lastSpeedUpdate = startTime
        
        FileOutputStream(targetFile, existingBytes > 0).use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Check for cancellation
                ensureActive()
                
                output.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                bytesInLastSecond += bytesRead
                
                // Calculate speed every second
                val now = System.currentTimeMillis()
                if (now - lastSpeedUpdate >= 1000) {
                    val bytesPerSecond = bytesInLastSecond * 1000 / (now - lastSpeedUpdate)
                    
                    val progress = DownloadProgress(
                        modelId = modelId,
                        state = DownloadState.DOWNLOADING,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        percentComplete = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0,
                        bytesPerSecond = bytesPerSecond
                    )
                    
                    emit(progress)
                    
                    bytesInLastSecond = 0
                    lastSpeedUpdate = now
                }
            }
        }
        
        inputStream.close()
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clean up failed download.
     */
    private fun cleanupDownload(tempFile: File) {
        try {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup temp file", e)
        }
    }
    
    /**
     * Calculate SHA-256 checksum of a file.
     */
    private suspend fun calculateChecksum(file: File): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check storage availability.
     */
    fun checkStorageAvailable(requiredBytes: Long): StorageCheckResult {
        val filesDir = context.filesDir
        val availableBytes = filesDir.freeSpace
        val totalBytes = filesDir.totalSpace
        
        return StorageCheckResult(
            availableBytes = availableBytes,
            requiredBytes = requiredBytes,
            hasSufficientSpace = availableBytes >= requiredBytes,
            availableGB = availableBytes / (1024.0 * 1024.0 * 1024.0),
            requiredGB = requiredBytes / (1024.0 * 1024.0 * 1024.0)
        )
    }
    
    data class StorageCheckResult(
        val availableBytes: Long,
        val requiredBytes: Long,
        val hasSufficientSpace: Boolean,
        val availableGB: Double,
        val requiredGB: Double
    ) {
        val warningMessage: String?
            get() = if (!hasSufficientSpace) {
                "Insufficient storage. Need ${String.format("%.2f", requiredGB)}GB, have ${String.format("%.2f", availableGB)}GB"
            } else if (availableGB < requiredGB + MIN_STORAGE_SPACE_GB) {
                "Low storage warning. Consider freeing up space."
            } else {
                null
            }
    }
}
