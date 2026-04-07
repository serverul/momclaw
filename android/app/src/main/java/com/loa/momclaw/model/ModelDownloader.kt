package com.loa.momclaw.model

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
 * Model Downloader with robust downloading capabilities.
 * 
 * Features:
 * - Resume support for partial downloads
 * - Real-time progress callbacks via Flow
 * - SHA-256 checksum integrity verification
 * - Automatic retry with exponential backoff
 * - Storage validation before download
 * - Cancellation support
 */
class ModelDownloader(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private val downloadJobs = mutableMapOf<String, Job>()
    private val downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    
    companion object {
        private const val TAG = "ModelDownloader"
        private const val BUFFER_SIZE = 8192
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val MIN_STORAGE_BUFFER_GB = 1L
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
        val isCancelled: Boolean get() = state == DownloadState.CANCELLED
        val isVerifying: Boolean get() = state == DownloadState.VERIFYING
        
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
        
        fun formatRemaining(): String {
            if (bytesPerSecond <= 0) return "Calculating..."
            val remainingBytes = totalBytes - bytesDownloaded
            val remainingSeconds = remainingBytes / bytesPerSecond
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return if (minutes > 0) "${minutes}m ${seconds}s remaining" else "${seconds}s remaining"
        }
    }
    
    enum class DownloadState {
        QUEUED, DOWNLOADING, PAUSED, VERIFYING, COMPLETED, FAILED, CANCELLED
    }
    
    /**
     * Callback interface for download progress updates.
     */
    interface ProgressCallback {
        fun onProgress(progress: DownloadProgress)
        fun onComplete(filePath: String)
        fun onError(error: Throwable)
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
     * Check if a download is in progress.
     */
    fun isDownloading(modelId: String): Boolean {
        val progress = downloadProgress.value[modelId] ?: return false
        return progress.isDownloading
    }
    
    /**
     * Download a model with progress tracking.
     * 
     * @param model The LiteRModel to download
     * @param targetDirectory Directory to save the model
     * @param verifyChecksum Whether to verify SHA-256 checksum after download
     * @return Flow of download progress
     */
    fun download(
        model: LiteRModel,
        targetDirectory: File = File(context.filesDir, "models"),
        verifyChecksum: Boolean = true
    ): Flow<DownloadProgress> = flow {
        val modelId = model.id
        
        // Check if already downloading
        if (downloadJobs[modelId]?.isActive == true) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.FAILED,
                errorMessage = "Download already in progress"
            ))
            return@flow
        }
        
        // Validate download URL
        if (model.downloadUrl.isNullOrEmpty()) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.FAILED,
                errorMessage = "No download URL available (bundled model?)"
            ))
            return@flow
        }
        
        // Validate storage space
        val storageCheck = validateStorage(model.size, targetDirectory)
        if (!storageCheck.hasSufficientSpace) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.FAILED,
                errorMessage = storageCheck.errorMessage
            ))
            return@flow
        }
        
        // Ensure directory exists
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        
        val targetFile = File(targetDirectory, "${modelId}.litertlm")
        val tempFile = File(targetDirectory, "${modelId}.litertlm.tmp")
        
        // Resume support: check for partial download
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        
        emit(DownloadProgress(
            modelId = modelId,
            state = DownloadState.QUEUED,
            bytesDownloaded = existingBytes,
            totalBytes = model.size
        ))
        
        // Start download with retry logic
        var retryCount = 0
        var lastException: Exception? = null
        
        while (retryCount < MAX_RETRIES) {
            try {
                downloadWithProgress(
                    url = model.downloadUrl,
                    targetFile = tempFile,
                    existingBytes = existingBytes,
                    modelId = modelId,
                    totalBytes = model.size
                ).collect { progress ->
                    emit(progress)
                    downloadProgress.value = downloadProgress.value + (modelId to progress)
                }
                
                // Download successful, break retry loop
                break
            } catch (e: CancellationException) {
                // Download was cancelled
                val cancelProgress = DownloadProgress(
                    modelId = modelId,
                    state = DownloadState.CANCELLED,
                    errorMessage = "Download cancelled"
                )
                emit(cancelProgress)
                downloadProgress.value = downloadProgress.value + (modelId to cancelProgress)
                return@flow
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
        
        // Verify checksum if required and available
        if (verifyChecksum && model.checksum.isNotEmpty()) {
            emit(DownloadProgress(
                modelId = modelId,
                state = DownloadState.VERIFYING,
                bytesDownloaded = tempFile.length(),
                totalBytes = model.size,
                percentComplete = 100
            ))
            
            val actualChecksum = calculateChecksum(tempFile)
            if (actualChecksum != model.checksum) {
                cleanupDownload(tempFile)
                val errorProgress = DownloadProgress(
                    modelId = modelId,
                    state = DownloadState.FAILED,
                    errorMessage = "Checksum verification failed. Expected ${model.checksum}, got $actualChecksum"
                )
                emit(errorProgress)
                downloadProgress.value = downloadProgress.value + (modelId to errorProgress)
                return@flow
            }
        }
        
        // Move temp file to final location
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val renamed = tempFile.renameTo(targetFile)
        if (!renamed) {
            // Fallback: copy instead of rename
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        
        val successProgress = DownloadProgress(
            modelId = modelId,
            state = DownloadState.COMPLETED,
            bytesDownloaded = targetFile.length(),
            totalBytes = model.size,
            percentComplete = 100,
            filePath = targetFile.absolutePath
        )
        
        emit(successProgress)
        downloadProgress.value = downloadProgress.value + (modelId to successProgress)
        
        Log.i(TAG, "Model downloaded successfully: $modelId to ${targetFile.absolutePath}")
    }.flowOn(Dispatchers.IO)
    
    /**
     * Download with callback interface instead of Flow.
     */
    fun downloadWithCallback(
        model: LiteRModel,
        callback: ProgressCallback,
        targetDirectory: File = File(context.filesDir, "models")
    ) {
        val modelId = model.id
        
        downloadJobs[modelId] = CoroutineScope(Dispatchers.IO).launch {
            download(model, targetDirectory).collect { progress ->
                callback.onProgress(progress)
                
                when {
                    progress.isComplete -> {
                        progress.filePath?.let { callback.onComplete(it) }
                    }
                    progress.isFailed -> {
                        callback.onError(Exception(progress.errorMessage ?: "Download failed"))
                    }
                }
            }
        }
    }
    
    /**
     * Cancel an ongoing download.
     */
    fun cancel(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        
        val currentProgress = downloadProgress.value[modelId]
        if (currentProgress != null && currentProgress.isDownloading) {
            downloadProgress.value = downloadProgress.value + (modelId to currentProgress.copy(
                state = DownloadState.CANCELLED,
                errorMessage = "Download cancelled by user"
            ))
        }
        
        // Clean up temp file
        val tempFile = File(context.filesDir, "models/${modelId}.litertlm.tmp")
        cleanupDownload(tempFile)
        
        Log.i(TAG, "Download cancelled: $modelId")
    }
    
    /**
     * Pause an ongoing download (cancels but keeps partial file for resume).
     */
    fun pause(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        
        val currentProgress = downloadProgress.value[modelId]
        if (currentProgress != null && currentProgress.isDownloading) {
            downloadProgress.value = downloadProgress.value + (modelId to currentProgress.copy(
                state = DownloadState.PAUSED
            ))
        }
        
        Log.i(TAG, "Download paused: $modelId")
    }
    
    /**
     * Resume a paused download.
     */
    fun resume(
        model: LiteRModel,
        targetDirectory: File = File(context.filesDir, "models")
    ): Flow<DownloadProgress> {
        return download(model, targetDirectory)
    }
    
    /**
     * Internal download implementation with progress tracking.
     */
    private suspend fun downloadWithProgress(
        url: String,
        targetFile: File,
        existingBytes: Long,
        modelId: String,
        totalBytes: Long
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
        
        val contentLength = response.body?.contentLength() ?: 0L
        val finalTotalBytes = if (contentLength > 0) existingBytes + contentLength else totalBytes
        
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
                
                // Calculate speed every 500ms for more responsive updates
                val now = System.currentTimeMillis()
                if (now - lastSpeedUpdate >= 500) {
                    val bytesPerSecond = bytesInLastSecond * 1000 / (now - lastSpeedUpdate)
                    
                    val progress = DownloadProgress(
                        modelId = modelId,
                        state = DownloadState.DOWNLOADING,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = finalTotalBytes,
                        percentComplete = if (finalTotalBytes > 0) 
                            ((bytesDownloaded * 100) / finalTotalBytes).toInt() else 0,
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
     * Validate storage availability.
     */
    fun validateStorage(requiredBytes: Long, directory: File = File(context.filesDir, "models")): StorageValidation {
        val availableBytes = directory.freeSpace
        val totalBytes = directory.totalSpace
        val requiredWithBuffer = requiredBytes + (MIN_STORAGE_BUFFER_GB * 1024 * 1024 * 1024)
        
        return StorageValidation(
            availableBytes = availableBytes,
            requiredBytes = requiredBytes,
            hasSufficientSpace = availableBytes >= requiredWithBuffer,
            availableGB = availableBytes / (1024.0 * 1024.0 * 1024.0),
            requiredGB = requiredBytes / (1024.0 * 1024.0 * 1024.0)
        )
    }
    
    data class StorageValidation(
        val availableBytes: Long,
        val requiredBytes: Long,
        val hasSufficientSpace: Boolean,
        val availableGB: Double,
        val requiredGB: Double
    ) {
        val errorMessage: String?
            get() = if (!hasSufficientSpace) {
                "Insufficient storage. Need ${String.format("%.2f", requiredGB)}GB, have ${String.format("%.2f", availableGB)}GB"
            } else null
    }
    
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
}
