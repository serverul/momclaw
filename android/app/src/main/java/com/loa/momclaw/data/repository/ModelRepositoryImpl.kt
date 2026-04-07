package com.loa.momclaw.data.repository

import android.content.Context
import com.loa.momclaw.bridge.*
import com.loa.momclaw.util.MomClawLogger
import com.loa.momclaw.data.download.ModelDownloadManager
import com.loa.momclaw.data.download.ModelMetadata
import com.loa.momclaw.domain.model.Model
import com.loa.momclaw.domain.repository.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ModelRepository with full download and management capabilities.
 */
@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: ModelDownloadManager,
    private val engineWrapper: LlmEngineWrapper,
    private val fallbackManager: ModelFallbackManager
) : ModelRepository {
    
    private val modelsDir = File(context.filesDir, "models")
    private var currentModel: String? = null
    private val modelStates = mutableMapOf<String, ModelState>()
    
    companion object {
        private const val TAG = "ModelRepository"
    }
    
    private val logger = MomClawLogger
    
    init {
        // Ensure models directory exists
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        // Initialize model states
        ModelMetadata.AVAILABLE_MODELS.forEach { metadata ->
            val modelId = "${metadata.namespace}/${metadata.repoId}"
            modelStates[modelId] = ModelState(
                metadata = metadata,
                downloaded = checkModelDownloaded(metadata),
                loaded = false,
                localPath = getModelPath(metadata)
            )
        }
    }
    
    /**
     * Get all available models with their current status.
     */
    override suspend fun getAvailableModels(): Result<List<Model>> {
        return try {
            val models = ModelMetadata.AVAILABLE_MODELS.map { metadata ->
                val modelId = "${metadata.namespace}/${metadata.repoId}"
                val state = modelStates[modelId]
                val localFile = File(getModelPath(metadata))
                
                Model(
                    id = modelId,
                    name = formatModelName(metadata),
                    description = generateModelDescription(metadata),
                    size = metadata.sizeDisplay,
                    downloaded = localFile.exists() && localFile.length() > 100_000_000, // Min 100MB
                    loaded = state?.loaded ?: false,
                    downloadUrl = metadata.downloadUrl,
                    localPath = if (localFile.exists()) localFile.absolutePath else null
                )
            }
            
            logger.i(TAG, "Available models retrieved: ${models.size}")
            Result.success(models)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get available models", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download a model with progress tracking.
     */
    override suspend fun downloadModel(modelId: String): Result<Unit> {
        return try {
            val metadata = findModelMetadata(modelId)
                ?: return Result.failure(IllegalArgumentException("Model not found: $modelId"))
            
            // Check storage
            val storageCheck = downloadManager.checkStorageAvailable(metadata.sizeBytes)
            if (!storageCheck.hasSufficientSpace) {
                return Result.failure(
                    InsufficientStorageException(storageCheck.warningMessage ?: "Insufficient storage")
                )
            }
            
            // Start download
            downloadManager.downloadModel(metadata).collect { progress ->
                when {
                    progress.isComplete -> {
                        // Update model state
                        modelStates[modelId] = modelStates[modelId]?.copy(
                            downloaded = true,
                            localPath = progress.filePath
                        ) ?: ModelState(
                            metadata = metadata,
                            downloaded = true,
                            localPath = progress.filePath
                        )
                        
                        logger.i(TAG, "Model downloaded successfully: $modelId")
                    }
                    
                    progress.isFailed -> {
                        logger.e(TAG, "Model download failed: ${progress.errorMessage}")
                        throw Exception(progress.errorMessage ?: "Download failed")
                    }
                }
            }
            
            logger.i(TAG, "Download completed for: $modelId")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to download model: $modelId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Load a model into memory with fallback support.
     */
    override suspend fun loadModel(modelId: String): Result<Unit> {
        return try {
            val state = modelStates[modelId]
                ?: return Result.failure(IllegalArgumentException("Model not found: $modelId"))
            
            if (!state.downloaded) {
                return Result.failure(ModelNotDownloadedException(modelId))
            }
            
            val localPath = state.localPath
                ?: return Result.failure(IllegalStateException("Model path not available"))
            
            // Use fallback manager for robust loading
            val loadResult = fallbackManager.loadWithFallback(
                modelPath = localPath,
                enableSimulation = false // Don't use simulation for explicit loads
            )
            
            when (loadResult) {
                is LoadResult.Success -> {
                    // Unload previous model if any
                    currentModel?.let { previousModelId ->
                        modelStates[previousModelId] = modelStates[previousModelId]?.copy(loaded = false)
                            ?: modelStates[previousModelId]!!
                    }
                    
                    // Mark new model as loaded
                    currentModel = modelId
                    modelStates[modelId] = state.copy(loaded = true)
                    
                    logger.i(TAG, "Model loaded successfully: $modelId (${loadResult.mode})")
                    Result.success(Unit)
                }
                
                is LoadResult.Failure -> {
                    logger.e(TAG, "Failed to load model: ${loadResult.error}")
                    Result.failure(ModelLoadException(loadResult.error, loadResult.suggestion))
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load model: $modelId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a downloaded model.
     */
    override suspend fun deleteModel(modelId: String): Result<Unit> {
        return try {
            val state = modelStates[modelId]
                ?: return Result.failure(IllegalArgumentException("Model not found: $modelId"))
            
            // Unload if currently loaded
            if (state.loaded) {
                engineWrapper.close()
                currentModel = null
            }
            
            // Delete file
            state.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            
            // Update state
            modelStates[modelId] = state.copy(
                downloaded = false,
                loaded = false,
                localPath = null
            )
            
            logger.i(TAG, "Model deleted: $modelId")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete model: $modelId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get currently loaded model ID.
     */
    override suspend fun getCurrentModel(): String? = currentModel
    
    /**
     * Get download progress for a specific model.
     */
    override fun getDownloadProgress(modelId: String): Flow<ModelDownloadManager.DownloadProgress> {
        return downloadManager.getDownloadProgress()
            .map { it[modelId] ?: ModelDownloadManager.DownloadProgress(
                modelId = modelId,
                state = ModelDownloadManager.DownloadState.QUEUED
            )}
    }
    
    /**
     * Get download progress for all active downloads.
     */
    override fun getAllDownloadProgress(): StateFlow<Map<String, ModelDownloadManager.DownloadProgress>> {
        return downloadManager.getDownloadProgress()
    }
    
    /**
     * Get storage information.
     */
    override suspend fun getStorageInfo(): StorageInfo {
        val availableSpace = modelsDir.freeSpace
        val totalSpace = modelsDir.totalSpace
        val usedByModels = calculateTotalModelsSize()
        
        return StorageInfo(
            availableSpaceBytes = availableSpace,
            totalSpaceBytes = totalSpace,
            usedByModelsBytes = usedByModels,
            modelCount = modelStates.values.count { it.downloaded }
        )
    }
    
    /**
     * Cancel an ongoing download.
     */
    override suspend fun cancelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
    }
    
    // Private helper methods
    
    private fun findModelMetadata(modelId: String): ModelMetadata? {
        return ModelMetadata.AVAILABLE_MODELS.find { metadata ->
            "${metadata.namespace}/${metadata.repoId}" == modelId
        }
    }
    
    private fun checkModelDownloaded(metadata: ModelMetadata): Boolean {
        val file = File(getModelPath(metadata))
        return file.exists() && file.length() > 100_000_000 // Min 100MB to be valid
    }
    
    private fun getModelPath(metadata: ModelMetadata): String {
        return File(modelsDir, metadata.filename).absolutePath
    }
    
    private fun formatModelName(metadata: ModelMetadata): String {
        return metadata.filename
            .removeSuffix(".litertlm")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }
    }
    
    private fun generateModelDescription(metadata: ModelMetadata): String {
        return buildString {
            append("Gemma 4 Efficient 4B model optimized for mobile devices. ")
            append("Size: ${metadata.sizeDisplay}. ")
            append("Quantization: Q4_K_M. ")
            append("Format: LiteRT-LM for optimal mobile inference.")
        }
    }
    
    private fun calculateTotalModelsSize(): Long {
        return modelStates.values
            .filter { it.downloaded }
            .mapNotNull { it.localPath }
            .map { File(it).length() }
            .sum()
    }
}

// Data classes

data class ModelState(
    val metadata: ModelMetadata,
    val downloaded: Boolean = false,
    val loaded: Boolean = false,
    val localPath: String? = null
)

data class StorageInfo(
    val availableSpaceBytes: Long,
    val totalSpaceBytes: Long,
    val usedByModelsBytes: Long,
    val modelCount: Int
) {
    val availableSpaceGB: Double get() = availableSpaceBytes / (1024.0 * 1024.0 * 1024.0)
    val usedSpaceGB: Double get() = usedByModelsBytes / (1024.0 * 1024.0 * 1024.0)
    val usedSpaceMB: Long get() = usedByModelsBytes / (1024 * 1024)
    
    fun formatInfo(): String {
        return "Models: $modelCount, Used: ${String.format("%.2f", usedSpaceGB)} GB, Available: ${String.format("%.2f", availableSpaceGB)} GB"
    }
}

// Custom exceptions

class InsufficientStorageException(message: String) : Exception(message)
class ModelNotDownloadedException(modelId: String) : Exception("Model not downloaded: $modelId")
class ModelLoadException(error: String, suggestion: String) : Exception("$error\nSuggestion: $suggestion")
