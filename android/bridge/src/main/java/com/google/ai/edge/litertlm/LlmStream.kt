// Streaming callback interface for LiteRT generation
package com.google.ai.edge.litertlm

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * Streaming callback interface for LiteRT generation.
 * 
 * Supports both callback-based and Flow-based streaming.
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
abstract class LlmStream {
    /**
     * Called for each partial result token/chunk.
     * Override this method to receive streaming tokens.
     * 
     * @param result The partial text token or chunk.
     */
    open fun onResult(result: String?) {}
    
    /**
     * Called when streaming generation is complete.
     * Override this method to handle completion.
     */
    open fun onComplete() {}
    
    /**
     * Called when an error occurs during streaming.
     * Override this method to handle errors.
     * 
     * @param error The error that occurred.
     */
    open fun onError(error: Throwable?) {}
    
    companion object {
        /**
         * Convert a callback-based stream to a Flow
         */
        fun toFlow(
            session: LlmSession,
            prompt: String,
            context: CoroutineContext = Dispatchers.Default
        ): Flow<String> = callbackFlow {
            val stream = object : LlmStream() {
                override fun onResult(result: String?) {
                    if (result != null) {
                        trySend(result)
                    }
                }
                
                override fun onComplete() {
                    close()
                }
                
                override fun onError(error: Throwable?) {
                    close(error ?: Exception("Unknown streaming error"))
                }
            }
            
            session.generateStream(prompt, stream)
            
            awaitClose { }
        }.flowOn(context)
    }
}
