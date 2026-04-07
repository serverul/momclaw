// Real TensorFlow Lite Implementation of LlmEngine
// Supports .tflite and .litertlm model files
package com.google.ai.edge.litertlm

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * LiteRT Engine for loading and managing LLM models using TensorFlow Lite.
 * 
 * This is a real implementation using TensorFlow Lite as the backend runtime.
 * Supports both .tflite and .litertlm model files (litertlm files are treated as TFLite).
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
class LlmEngine private constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var modelBuffer: MappedByteBuffer? = null
    private var currentModel: Model? = null
    
    companion object {
        private const val TAG = "LlmEngine"
        
        @Volatile
        private var instance: LlmEngine? = null
        
        /**
         * Get singleton instance of LlmEngine
         */
        fun getInstance(context: Context): LlmEngine {
            return instance ?: synchronized(this) {
                instance ?: LlmEngine(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Model representation wrapping a .litertlm or .tflite file
     */
    class Model(file: File) {
        val path: String = file.absolutePath
        val name: String = file.nameWithoutExtension
        val extension: String = file.extension.lowercase()
        
        init {
            require(extension == "tflite" || extension == "litertlm") {
                "Model must be .tflite or .litertlm format, got: .$extension"
            }
        }
    }
    
    /**
     * Load a model file and initialize the interpreter
     */
    fun loadModel(model: Model, settings: LlmGenerationSettings): Result<Unit> {
        return try {
            // Close existing model if any
            close()
            
            Log.i(TAG, "Loading model: ${model.name} from ${model.path}")
            
            // Load model file into memory
            modelBuffer = loadModelFile(File(model.path))
            
            // Configure interpreter options
            val options = Interpreter.Options().apply {
                // Try to use GPU if available
                try {
                    val compatList = CompatibilityList()
                    if (compatList.isDelegateSupportedOnThisDevice) {
                        Log.i(TAG, "GPU acceleration available, enabling GPU delegate")
                        // Use default GPU delegate options for compatibility
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate!!)
                    } else {
                        Log.w(TAG, "GPU not available, using CPU with ${Runtime.getRuntime().availableProcessors()} threads")
                        setNumThreads(Runtime.getRuntime().availableProcessors())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "GPU delegate initialization failed, falling back to CPU", e)
                    setNumThreads(Runtime.getRuntime().availableProcessors())
                }
                
                // Enable XNNPACK for optimized CPU inference
                setUseXNNPACK(true)
            }
            
            // Create interpreter
            interpreter = Interpreter(modelBuffer!!, options)
            
            currentModel = model
            
            Log.i(TAG, "Model loaded successfully: ${model.name}")
            Log.d(TAG, "Input tensors: ${interpreter?.inputTensorCount}")
            Log.d(TAG, "Output tensors: ${interpreter?.outputTensorCount}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${model.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the underlying TensorFlow Lite interpreter
     */
    fun getInterpreter(): Interpreter? = interpreter
    
    /**
     * Check if a model is loaded
     */
    fun isLoaded(): Boolean = interpreter != null && modelBuffer != null
    
    /**
     * Get the currently loaded model
     */
    fun getCurrentModel(): Model? = currentModel
    
    /**
     * Close and release resources
     */
    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            
            gpuDelegate?.close()
            gpuDelegate = null
            
            modelBuffer = null
            currentModel = null
            
            Log.d(TAG, "Engine resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing engine", e)
        }
    }
    
    /**
     * Load model file into MappedByteBuffer
     */
    private fun loadModelFile(file: File): MappedByteBuffer {
        val inputStream = file.inputStream()
        val fileChannel = inputStream.channel
        val startOffset = 0L
        val declaredLength = file.length()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
