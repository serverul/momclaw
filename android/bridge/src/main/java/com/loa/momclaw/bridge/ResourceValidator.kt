package com.loa.momclaw.bridge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Resource Validator for MOMCLAW
 * 
 * Validates critical resources at startup:
 * - NullClaw binary existence and validity
 * - LiteRT model existence and validity
 * 
 * Provides user-friendly alerts and download instructions when resources are missing.
 */
class ResourceValidator(private val context: Context) {
    
    /**
     * Result of resource validation
     */
    sealed class ValidationResult {
        data class Success(
            val binaryStatus: BinaryStatus,
            val modelStatus: ModelValidationStatus
        ) : ValidationResult()
        
        data class Warning(
            val binaryStatus: BinaryStatus,
            val modelStatus: ModelValidationStatus,
            val warnings: List<String>
        ) : ValidationResult()
        
        data class Error(
            val message: String,
            val missingResources: List<MissingResource>,
            val recoverySteps: List<String>
        ) : ValidationResult()
    }
    
    /**
     * Binary validation status
     */
    sealed class BinaryStatus {
        data class Available(val path: String, val abi: String) : BinaryStatus()
        data class Missing(val expectedPath: String, val supportedAbis: List<String>) : BinaryStatus()
        data class StubMode(val reason: String) : BinaryStatus()
    }
    
    /**
     * Model validation status
     */
    sealed class ModelValidationStatus {
        data class Available(val path: String, val sizeGB: Double) : ModelValidationStatus()
        data class Missing(val expectedPath: String, val downloadUrl: String) : ModelValidationStatus()
        data class Corrupted(val path: String, val expectedSizeGB: Double, val actualSizeGB: Double) : ModelValidationStatus()
        data class SimulationMode(val reason: String) : ModelValidationStatus()
    }
    
    /**
     * Missing resource information
     */
    data class MissingResource(
        val name: String,
        val type: ResourceType,
        val description: String,
        val downloadUrl: String?,
        val size: String?
    )
    
    enum class ResourceType {
        BINARY,
        MODEL,
        CONFIG
    }
    
    /**
     * Validate all critical resources
     */
    suspend fun validateAll(): ValidationResult = withContext(Dispatchers.IO) {
        val binaryStatus = validateBinary()
        val modelStatus = validateModel()
        val warnings = mutableListOf<String>()
        val missingResources = mutableListOf<MissingResource>()
        val recoverySteps = mutableListOf<String>()
        
        // Check binary status
        when (binaryStatus) {
            is BinaryStatus.Missing -> {
                missingResources.add(
                    MissingResource(
                        name = "NullClaw Binary",
                        type = ResourceType.BINARY,
                        description = "Native agent binary for ${binaryStatus.supportedAbis.joinToString()}",
                        downloadUrl = "https://github.com/loa-momclaw/nullclaw/releases",
                        size = "~15MB per ABI"
                    )
                )
                recoverySteps.add("Build NullClaw from source: https://github.com/loa-momclaw/nullclaw")
                recoverySteps.add("Place binary in android/app/src/main/assets/ as nullclaw-arm64")
            }
            is BinaryStatus.StubMode -> {
                warnings.add("Running in stub mode: ${binaryStatus.reason}")
                warnings.add("Agent features will be limited")
            }
            is BinaryStatus.Available -> {
                // Binary is available
            }
        }
        
        // Check model status
        when (modelStatus) {
            is ModelValidationStatus.Missing -> {
                missingResources.add(
                    MissingResource(
                        name = "LiteRT Model",
                        type = ResourceType.MODEL,
                        description = "Gemma 4 E4B IT model for on-device inference",
                        downloadUrl = modelStatus.downloadUrl,
                        size = "~3.5GB"
                    )
                )
                recoverySteps.add("Download model: huggingface-cli download litert-community/gemma-4-E4B-it-litertlm")
                recoverySteps.add("Place model at: ${modelStatus.expectedPath}")
            }
            is ModelValidationStatus.Corrupted -> {
                missingResources.add(
                    MissingResource(
                        name = "LiteRT Model",
                        type = ResourceType.MODEL,
                        description = "Model file appears corrupted (${modelStatus.actualSizeGB}GB vs expected ${modelStatus.expectedSizeGB}GB)",
                        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litertlm",
                        size = "~3.5GB"
                    )
                )
                recoverySteps.add("Re-download the model - current file is incomplete")
            }
            is ModelValidationStatus.SimulationMode -> {
                warnings.add("Running in simulation mode: ${modelStatus.reason}")
                warnings.add("Inference will use simulated responses")
            }
            is ModelValidationStatus.Available -> {
                // Model is available
            }
        }
        
        // Determine result
        return@withContext when {
            missingResources.isNotEmpty() -> {
                ValidationResult.Error(
                    message = "Missing ${missingResources.size} critical resource(s)",
                    missingResources = missingResources,
                    recoverySteps = recoverySteps
                )
            }
            warnings.isNotEmpty() -> {
                ValidationResult.Warning(
                    binaryStatus = binaryStatus,
                    modelStatus = modelStatus,
                    warnings = warnings
                )
            }
            else -> {
                ValidationResult.Success(
                    binaryStatus = binaryStatus,
                    modelStatus = modelStatus
                )
            }
        }
    }
    
    /**
     * Validate NullClaw binary
     */
    private fun validateBinary(): BinaryStatus {
        val supportedAbis = getSupportedAbis()
        val abiMapping = mapOf(
            "arm64-v8a" to "nullclaw-arm64",
            "armeabi-v7a" to "nullclaw-arm32",
            "x86_64" to "nullclaw-x86_64",
            "x86" to "nullclaw-x86"
        )
        
        // Check for binary in assets
        for (abi in supportedAbis) {
            val assetName = abiMapping[abi] ?: continue
            try {
                val inputStream = context.assets.open(assetName)
                inputStream.close()
                
                // Binary exists
                val outputPath = File(context.filesDir, "nullclaw")
                return BinaryStatus.Available(
                    path = outputPath.absolutePath,
                    abi = abi
                )
            } catch (e: Exception) {
                // Try next ABI or generic name
            }
        }
        
        // Try generic "nullclaw" asset
        try {
            val inputStream = context.assets.open("nullclaw")
            inputStream.close()
            return BinaryStatus.Available(
                path = File(context.filesDir, "nullclaw").absolutePath,
                abi = "generic"
            )
        } catch (e: Exception) {
            // No binary in assets
        }
        
        // Check if already extracted
        val extractedBinary = File(context.filesDir, "nullclaw")
        if (extractedBinary.exists() && extractedBinary.canExecute()) {
            // Binary already extracted (might be stub)
            val size = extractedBinary.length()
            if (size < 1000) {
                return BinaryStatus.StubMode("Binary file too small ($size bytes) - likely a stub script")
            }
            return BinaryStatus.Available(
                path = extractedBinary.absolutePath,
                abi = "extracted"
            )
        }
        
        // Check if stub mode is acceptable
        val stubFile = File(context.filesDir, "nullclaw.stub")
        if (stubFile.exists()) {
            return BinaryStatus.StubMode("Stub mode enabled by user")
        }
        
        return BinaryStatus.Missing(
            expectedPath = "android/app/src/main/assets/nullclaw-{abi}",
            supportedAbis = supportedAbis
        )
    }
    
    /**
     * Validate LiteRT model
     */
    private fun validateModel(): ModelValidationStatus {
        val modelPath = File(context.filesDir, "models/gemma-4-E4B-it.litertlm").absolutePath
        val modelFile = File(modelPath)
        
        if (!modelFile.exists()) {
            // Try alternate locations
            val alternatePath = File(context.filesDir, "models/gemma.litertlm")
            if (alternatePath.exists()) {
                return checkModelFile(alternatePath)
            }
            
            return ModelValidationStatus.Missing(
                expectedPath = modelPath,
                downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litertlm"
            )
        }
        
        return checkModelFile(modelFile)
    }
    
    /**
     * Check model file validity
     */
    private fun checkModelFile(file: File): ModelValidationStatus {
        val sizeBytes = file.length()
        val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
        
        // Expected size for Gemma 4 E4B IT is ~3.9GB
        val expectedSizeGB = 3.5
        
        when {
            sizeGB < 0.5 -> {
                return ModelValidationStatus.Corrupted(
                    path = file.absolutePath,
                    expectedSizeGB = expectedSizeGB,
                    actualSizeGB = sizeGB
                )
            }
            sizeGB < expectedSizeGB * 0.9 -> {
                // File might be partially downloaded
                return ModelValidationStatus.Corrupted(
                    path = file.absolutePath,
                    expectedSizeGB = expectedSizeGB,
                    actualSizeGB = sizeGB
                )
            }
            else -> {
                return ModelValidationStatus.Available(
                    path = file.absolutePath,
                    sizeGB = sizeGB
                )
            }
        }
    }
    
    /**
     * Get supported ABIs for this device
     */
    private fun getSupportedAbis(): List<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.os.Build.SUPPORTED_ABIS.toList()
        } else {
            @Suppress("DEPRECATION")
            listOf(android.os.Build.CPU_ABI)
        }
    }
    
    /**
     * Get user-friendly alert message for validation result
     */
    fun getAlertMessage(result: ValidationResult): String {
        return when (result) {
            is ValidationResult.Success -> {
                "All resources available. Ready to start."
            }
            is ValidationResult.Warning -> {
                val warnings = result.warnings.joinToString("\n• ", "• ", "")
                "Running with limitations:\n$warnings\n\nApp will function in reduced mode."
            }
            is ValidationResult.Error -> {
                val missing = result.missingResources.joinToString("\n• ", "• ", "") { 
                    "${it.name}: ${it.description}" 
                }
                val steps = result.recoverySteps.joinToString("\n• ", "• ", "")
                "Missing resources:\n$missing\n\nRecovery steps:\n$steps"
            }
        }
    }
    
    /**
     * Check if app can run in current state
     */
    fun canRunInCurrentState(result: ValidationResult): Boolean {
        return when (result) {
            is ValidationResult.Success -> true
            is ValidationResult.Warning -> true // Can run with limitations
            is ValidationResult.Error -> result.missingResources.all { 
                // Can run if only binary is missing (will use stub/simulation)
                it.type == ResourceType.BINARY 
            }
        }
    }
    
    companion object {
        const val MODEL_DOWNLOAD_URL = "https://huggingface.co/litert-community/gemma-4-E4B-it-litertlm"
        const val BINARY_DOWNLOAD_URL = "https://github.com/loa-momclaw/nullclaw/releases"
        const val EXPECTED_MODEL_SIZE_GB = 3.5
    }
}
