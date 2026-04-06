package com.loa.momclaw.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Buffer for streaming tokens with backpressure handling.
 * Batches tokens to reduce UI recomposition frequency.
 */
class StreamBuffer(
    private val scope: CoroutineScope,
    private val batchIntervalMs: Long = 50,  // Update UI every 50ms max
    private val minBatchSize: Int = 3         // Or every 3 tokens, whichever comes first
) {
    private val buffer = StringBuilder()
    private var tokenCount = 0
    private val isBuffering = AtomicBoolean(false)
    private var flushJob: Job? = null
    
    private val _batchFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val batchFlow: SharedFlow<String> = _batchFlow.asSharedFlow()
    
    /**
     * Add a token to the buffer. Will flush if batch size threshold is reached.
     */
    fun append(token: String) {
        synchronized(buffer) {
            buffer.append(token)
            tokenCount++
            
            // Immediate flush if batch size reached
            if (tokenCount >= minBatchSize && isBuffering.compareAndSet(false, true)) {
                flushNow()
            } else if (flushJob == null && isBuffering.compareAndSet(false, true)) {
                // Start timed flush
                startTimedFlush()
            }
        }
    }
    
    /**
     * Get current accumulated content
     */
    fun getCurrentContent(): String {
        synchronized(buffer) {
            return buffer.toString()
        }
    }
    
    /**
     * Force flush all buffered content immediately
     */
    fun flush() {
        flushNow()
    }
    
    /**
     * Clear buffer and reset state
     */
    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            tokenCount = 0
            flushJob?.cancel()
            flushJob = null
            isBuffering.set(false)
        }
    }
    
    private fun flushNow() {
        synchronized(buffer) {
            if (buffer.isEmpty()) {
                isBuffering.set(false)
                return
            }
            
            val content = buffer.toString()
            scope.launch {
                _batchFlow.emit(content)
            }
            
            tokenCount = 0
            isBuffering.set(false)
        }
    }
    
    private fun startTimedFlush() {
        flushJob = scope.launch(Dispatchers.Default) {
            delay(batchIntervalMs)
            flushNow()
            flushJob = null
        }
    }
}
