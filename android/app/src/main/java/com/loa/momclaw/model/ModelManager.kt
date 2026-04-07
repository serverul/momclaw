package com.loa.momclaw.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.loa.momclaw.data.download.ModelMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.MessageDigest

/**
 * Singleton Model Manager for LiteRT-LM models.
 * 
 * Provides centralized model management including:
 * - Listing available models (local + bundled)
 * - Downloading models from remote sources
 * - Deleting downloaded models
 * - Integrity verification via checksums
 * - Progress tracking for downloads
 * - Active model persistence
 * 
 * Usage:
 * ```
 * val modelManager = ModelManager.getInstance(context)
 * 
 * // List models
 * val models = modelManager.getAvailableModels()
 * 
 * // Download with progress
 * modelManager.downloadModel("gemma-4-e4b-lt").collect { progress ->
 *     updateUI(progress)
 * }
 * 
 * // Set active model
 * modelManager.setActiveModel("gemma-4-e4b-lt")
 * ```
 */
class ModelManager private constructor(private val context: Context) {
    
    private val downloader = ModelDownloader(context)
    private val modelsDir = File(context.filesDir, "models")
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()
    
    private val _models = MutableStateFlow<List<LiteRModel>>(emptyList())
    val models: StateFlow<List<LiteRModel>> = _models.asStateFlow()
    
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "ModelManager"
        private const val PREF_ACTIVE_MODEL = "active_model_id"
        private const val DEFAULT_MODEL_ID = LiteRModel.DEFAULT_MODEL_ID
        
        @Volatile
        private var instance: ModelManager? = null
        
        /**
         * Get singleton instance of ModelManager.
         */
        fun getInstance(context: Context): ModelManager {
            return instance ?: synchronized(this) {
                instance ?: ModelManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        // Ensure models directory exists
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        // Load active model from preferences
        _activeModelId.value = prefs.getString(PREF_ACTIVE_MODEL, null)
        
        // Initialize models list
        refreshModels()
    }
    
    /**
     * Get list of all available models (local + bundled).
     */
    fun getAvailableModels(): List<LiteRModel> {
        return _models.value
    }
    
    /**
     * Refresh the models list from disk and bundled resources.
     */
    fun refreshModels() {
        managerScope.launch {
            val models = mutableListOf<LiteRModel>()
            
            // Add predefined models from HuggingFace metadata
            ModelMetadata.AVAILABLE_MODELS.forEach { metadata ->
                val modelId = metadata.filename.removeSuffix(".litertlm")
                val localFile = File(modelsDir, metadata.filename)
                val isActive = _activeModelId.value == modelId
                
                models.add(LiteRModel(
                    id = modelId,
                    name = formatModelName(metadata),
                    size = metadata.sizeBytes,
                    downloadUrl = metadata.downloadUrl,
                    checksum = metadata.sha256 ?: "",
                    isDownloaded = localFile.exists() && localFile.length() > 10_000_000,
                    isActive = isActive && localFile.exists(),
                    downloadedSize = if (localFile.exists()) localFile.length() else 0L,
                    progress = if (localFile.exists() && localFile.length() > 10_000_000) 1f else 0f
                ))
            }
            
            // Also scan for any additional local models not in predefined list
            scanLocalModels(models)
            
            _models.value = models.sortedByDescending { it.isDownloaded }
            Log.d(TAG, "Refreshed models: ${models.size} total, ${models.count { it.isDownloaded }} downloaded")
        }
    }
    
    /**
     * Scan local models directory for additional models.
     */
    private fun scanLocalModels(models: MutableList<LiteRModel>) {
        if (!modelsDir.exists()) return
        
        modelsDir.listFiles()?.filter { 
            it.extension == "litertlm" && it.length() > 10_000_000 
        }?.forEach { file ->
            val modelId = file.nameWithoutExtension
            
            // Skip if already in list
            if (models.any { it.id == modelId }) return@forEach
            
            val isActive = _activeModelId.value == modelId
            
            models.add(LiteRModel(
                id = modelId,
                name = formatLocalModelName(modelId),
                size = file.length(),
                downloadUrl = null, // Local only
                checksum = "", // Unknown
                isDownloaded = true,
                isActive = isActive,
                downloadedSize = file.length(),
                progress = 1f
            ))
        }
    }
    
    /**
     * Get a specific model by ID.
     */
    fun getModel(modelId: String): LiteRModel? {
        return _models.value.find { it.id == modelId }
    }
    
    /**
     * Get the currently active model.
     */
    fun getActiveModel(): LiteRModel? {
        return _activeModelId.value?.let { getModel(it) }
    }
    
    /**
     * Set the active model.
     */
    fun setActiveModel(modelId: String) {
        val model = getModel(modelId)
        if (model == null) {
            Log.w(TAG, "Cannot set active model: not found - $modelId")
            return
        }
        
        if (!model.isDownloaded) {
            Log.w(TAG, "Cannot set active model: not downloaded - $modelId")
            return
        }
        
        prefs.edit().putString(PREF_ACTIVE_MODEL, modelId).apply()
        _activeModelId.value = modelId
        
        // Update models list to reflect active status
        _models.value = _models.value.map { 
            it.copy(isActive = it.id == modelId)
        }
        
        Log.i(TAG, "Active model set to: $modelId")
    }
    
    /**
     * Download a model with progress tracking.
     * 
     * @param modelId The model ID to download
     * @return Flow of download progress
     */
    fun downloadModel(modelId: String): Flow<ModelDownloader.DownloadProgress> = flow {
        val model = getModel(modelId)
        if (model == null) {
            emit(ModelDownloader.DownloadProgress(
                modelId = modelId,
                state = ModelDownloader.DownloadState.FAILED,
                errorMessage = "Model not found: $modelId"
            ))
            return@flow
        }
        
        if (model.isDownloaded) {
            emit(ModelDownloader.DownloadProgress(
                modelId = modelId,
                state = ModelDownloader.DownloadState.COMPLETED,
                bytesDownloaded = model.size,
                totalBytes = model.size,
                percentComplete = 100
            ))
            return@flow
        }
        
        // Emit initial progress
        emit(ModelDownloader.DownloadProgress(
            modelId = modelId,
            state = ModelDownloader.DownloadState.QUEUED,
            totalBytes = model.size
        ))
        
        // Start download
        downloader.download(model, modelsDir).collect { progress ->
            emit(progress)
            
            // Update model list on completion
            if (progress.isComplete) {
                withContext(Dispatchers.Main) {
                    refreshModels()
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Cancel an ongoing download.
     */
    fun cancelDownload(modelId: String) {
        downloader.cancel(modelId)
    }
    
    /**
     * Delete a downloaded model.
     * 
     * @param modelId The model ID to delete
     * @return Result with success or failure
     */
    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val model = getModel(modelId)
            if (model == null) {
                return@withContext Result.failure(IllegalArgumentException("Model not found: $modelId"))
            }
            
            if (!model.isDownloaded) {
                return@withContext Result.failure(IllegalStateException("Model not downloaded: $modelId"))
            }
            
            // Clear active model if this is it
            if (_activeModelId.value == modelId) {
                prefs.edit().remove(PREF_ACTIVE_MODEL).apply()
                _activeModelId.value = null
            }
            
            // Delete file
            val file = File(modelsDir, "${modelId}.litertlm")
            val altFile = File(modelsDir, "${modelId}.LiteRModel")
            
            var deleted = false
            if (file.exists()) {
                deleted = file.delete()
            }
            if (altFile.exists()) {
                deleted = altFile.delete() || deleted
            }
            
            if (!deleted && (file.exists() || altFile.exists())) {
                return@withContext Result.failure(IOException("Failed to delete model file"))
            }
            
            // Refresh models list
            refreshModels()
            
            Log.i(TAG, "Model deleted: $modelId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete model: $modelId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify model integrity using checksum.
     * 
     * @param modelId The model ID to verify
     * @return Result with verification status
     */
    suspend fun verifyModel(modelId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val model = getModel(modelId)
            if (model == null) {
                return@withContext Result.failure(IllegalArgumentException("Model not found: $modelId"))
            }
            
            if (!model.isDownloaded) {
                return@withContext Result.failure(IllegalStateException("Model not downloaded: $modelId"))
            }
            
            // If no checksum available, verify file exists and has reasonable size
            if (model.checksum.isEmpty()) {
                val file = File(modelsDir, "${modelId}.litertlm")
                val valid = file.exists() && file.length() > 10_000_000 // Min 10MB
                return@withContext Result.success(valid)
            }
            
            // Verify checksum
            val file = File(modelsDir, "${modelId}.litertlm")
            val actualChecksum = calculateChecksum(file)
            val valid = actualChecksum.equals(model.checksum, ignoreCase = true)
            
            Log.d(TAG, "Model verification for $modelId: $valid")
            Result.success(valid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify model: $modelId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get download progress for all active downloads.
     */
    fun getDownloadProgress(): StateFlow<Map<String, ModelDownloader.DownloadProgress>> {
        return downloader.getDownloadProgress()
    }
    
    /**
     * Get download progress for a specific model.
     */
    fun getDownloadProgress(modelId: String): ModelDownloader.DownloadProgress? {
        return downloader.getDownloadProgress(modelId)
    }
    
    /**
     * Get local file path for a model.
     */
    fun getModelPath(modelId: String): String? {
        val file = File(modelsDir, "${modelId}.litertlm")
        return if (file.exists()) file.absolutePath else null
    }
    
    /**
     * Get storage information.
     */
    fun getStorageInfo(): StorageInfo {
        val totalSpace = modelsDir.totalSpace
        val freeSpace = modelsDir.freeSpace
        val usedByModels = _models.value
            .filter { it.isDownloaded }
            .sumOf { it.size }
        
        return StorageInfo(
            totalSpaceBytes = totalSpace,
            freeSpaceBytes = freeSpace,
            usedByModelsBytes = usedByModels,
            modelCount = _models.value.count { it.isDownloaded }
        )
    }
    
    /**
     * Check if a model is currently being downloaded.
     */
    fun isDownloading(modelId: String): Boolean {
        return downloader.isDownloading(modelId)
    }
    
    // Private helper methods
    
    private fun formatModelName(metadata: ModelMetadata): String {
        return metadata.filename
            .removeSuffix(".litertlm")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
    
    private fun formatLocalModelName(modelId: String): String {
        return modelId
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
    
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

/**
 * Storage information data class.
 */
data class StorageInfo(
    val totalSpaceBytes: Long,
    val freeSpaceBytes: Long,
    val usedByModelsBytes: Long,
    val modelCount: Int
) {
    val totalSpaceGB: Double get() = totalSpaceBytes / (1024.0 * 1024.0 * 1024.0)
    val freeSpaceGB: Double get() = freeSpaceBytes / (1024.0 * 1024.0 * 1024.0)
    val usedByModelsGB: Double get() = usedByModelsBytes / (1024.0 * 1024.0 * 1024.0)
    val usedPercentage: Int get() = if (totalSpaceBytes > 0) 
        ((usedByModelsBytes * 100) / totalSpaceBytes).toInt() else 0
    
    fun formatInfo(): String {
        return "$modelCount models, ${String.format("%.2f", usedByModelsGB)} GB used, ${String.format("%.2f", freeSpaceGB)} GB free"
    }
}
