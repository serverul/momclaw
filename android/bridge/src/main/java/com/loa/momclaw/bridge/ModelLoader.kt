package com.loa.momclaw.bridge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream


/**
 * Model Loader for LiteRT models from HuggingFace
 * 
 * Handles:
 * - Model path verification
 * - Model file validation
 * - Checksum verification
 * - Model extraction from archives
 * 
 * HuggingFace model: litert-community/gemma-3-E4B-it-litertlm
 * Format: .litertlm (single file) or .zip archive
 */
class ModelLoader(private val context: Context) {
    
    data class ModelInfo(
        val name: String,
        val path: String,
        val sizeBytes: Long,
        val checksum: String?,
        val isReady: Boolean
    )
    
    sealed class LoadResult {
        data class Success(val info: ModelInfo) : LoadResult()
        data class Error(val message: String, val cause: Throwable? = null) : LoadResult()
    }
    
    /**
     * Verify and prepare a model for loading.
     * 
     * @param modelPath Path to .litertlm file or directory containing models
     * @return LoadResult with model info or error
     */
    suspend fun verifyModel(modelPath: String): LoadResult = withContext(Dispatchers.IO) {
        try {
            // TODO: Add logging
            
            val modelFile = File(modelPath)
            
            // Check if path exists
            if (!modelFile.exists()) {
                // TODO: Add logging
                return@withContext LoadResult.Error(
                    "Model file not found: $modelPath",
                    ModelNotFoundException(modelPath)
                )
            }
            
            // Validate file type
            if (!isValidModelFile(modelFile)) {
                return@withContext LoadResult.Error(
                    "Invalid model file format. Expected .litertlm or .zip",
                    InvalidModelFormatException(modelFile.extension)
                )
            }
            
            // If it's a zip, extract it
            val actualModelFile = if (modelFile.extension == "zip") {
                extractModelArchive(modelFile)
            } else {
                modelFile
            }
            
            // Validate model size (should be at least 100MB for Gemma 4E4B)
            val sizeBytes = actualModelFile.length()
            if (sizeBytes < MIN_MODEL_SIZE_BYTES) {
                // TODO: Add logging
                return@withContext LoadResult.Error(
                    "Model file too small (expected >${MIN_MODEL_SIZE_BYTES / (1024 * 1024)}MB, got ${sizeBytes / (1024 * 1024)}MB)",
                    ModelTooSmallException(sizeBytes)
                )
            }
            
            // Calculate checksum (optional, for verification)
            val checksum = calculateChecksum(actualModelFile)
            // TODO: Add logging
            
            LoadResult.Success(
                ModelInfo(
                    name = actualModelFile.nameWithoutExtension,
                    path = actualModelFile.absolutePath,
                    sizeBytes = sizeBytes,
                    checksum = checksum,
                    isReady = true
                )
            )
        } catch (e: Exception) {
            // TODO: Add logging
            LoadResult.Error("Model verification failed: ${e.message}", e)
        }
    }
    
    /**
     * Download model from HuggingFace (placeholder for future implementation)
     * 
     * Note: Actual download should be done via:
     * - huggingface-cli: `huggingface-cli download litert-community/gemma-3-E4B-it-litertlm`
     * - Direct URL: https://huggingface.co/litert-community/gemma-3-E4B-it-litertlm/resolve/main/gemma-3-E4B-it.litertlm
     */
    suspend fun downloadFromHuggingFace(
        modelId: String = DEFAULT_MODEL_ID,
        targetPath: String? = null
    ): LoadResult = withContext(Dispatchers.IO) {
        // This is a placeholder - actual implementation would use:
        // 1. HuggingFace Hub API
        // 2. OkHttp/Ktor for download
        // 3. Progress tracking
        // 4. Resume capability
        
        val actualTargetPath = targetPath ?: getDefaultModelPath()
        // TODO: Add logging
            "HuggingFace download not implemented. Please download manually:\n" +
            "  huggingface-cli download $modelId\n" +
            "  or visit: https://huggingface.co/$modelId\n" +
            "Target path: $actualTargetPath"
        }
        
        LoadResult.Error(
            "Automatic HuggingFace download not implemented. " +
            "Please download $modelId manually to $actualTargetPath"
        )
    }
    
    /**
     * Get default model path for Gemma 4E4B
     */
    fun getDefaultModelPath(): String {
        return File(context.filesDir, "models/gemma-3-E4B-it.litertlm").absolutePath
    }
    
    /**
     * Ensure model directory exists
     */
    fun ensureModelDirectory(): File {
        val modelDir = File(context.filesDir, "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
            // TODO: Add logging
        }
        return modelDir
    }
    
    /**
     * List available models in the models directory
     */
    fun listAvailableModels(): List<File> {
        val modelDir = File(context.filesDir, "models")
        if (!modelDir.exists()) return emptyList()
        
        return modelDir.listFiles { file ->
            file.extension in listOf("litertlm", "zip")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Get model storage info
     */
    fun getStorageInfo(): StorageInfo {
        val modelDir = ensureModelDirectory()
        val models = listAvailableModels()
        val totalSize = models.sumOf { it.length() }
        val availableSpace = modelDir.freeSpace
        
        return StorageInfo(
            modelsDirectory = modelDir.absolutePath,
            totalModelsSize = totalSize,
            availableSpace = availableSpace,
            modelCount = models.size
        )
    }
    
    // Private methods
    
    private fun isValidModelFile(file: File): Boolean {
        return file.extension in listOf("litertlm", "zip") && file.isFile
    }
    
    private fun extractModelArchive(zipFile: File): File {
        val targetDir = zipFile.parentFile ?: File(context.filesDir, "models")
        var extractedFile: File? = null
        
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".litertlm")) {
                    val outFile = File(targetDir, entry.name.substringAfterLast("/"))
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    extractedFile = outFile
                    // TODO: Add logging
                    break
                }
                entry = zis.nextEntry
            }
        }
        
        return extractedFile ?: throw ModelExtractionException("No .litertlm file found in archive")
    }
    
    private fun calculateChecksum(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    
    data class StorageInfo(
        val modelsDirectory: String,
        val totalModelsSize: Long,
        val availableSpace: Long,
        val modelCount: Int
    )
    
    companion object {
        const val DEFAULT_MODEL_ID = "litert-community/gemma-3-E4B-it-litertlm"
        const val MIN_MODEL_SIZE_BYTES = 100L * 1024 * 1024 // 100MB minimum
    }
}

// Custom exceptions
class ModelNotFoundException(path: String) : Exception("Model not found: $path")
class InvalidModelFormatException(format: String) : Exception("Invalid model format: $format")
class ModelTooSmallException(size: Long) : Exception("Model too small: $size bytes")
class ModelExtractionException(message: String) : Exception(message)
