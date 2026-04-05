package com.loa.momclaw.agent

import android.content.Context
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bridge to manage the NullClaw binary process
 * Handles binary extraction, process lifecycle, and health monitoring
 */
class NullClawBridge(private val context: Context) {
    
    companion object {
        private const val BINARY_NAME = "nullclaw"
        private const val CONFIG_NAME = "agent_config.json"
        private const val TAG = "NullClawBridge"
    }
    
    private var process: Process? = null
    private val isRunningFlag = AtomicBoolean(false)
    private var configPath: String? = null
    private var binaryPath: String? = null
    
    /**
     * Setup the NullClaw environment
     * - Extracts binary from assets to internal storage
     * - Sets executable permissions
     * - Generates config file
     * 
     * @param config The agent configuration
     * @return Result containing the binary path or error
     */
    fun setup(config: AgentConfig): Result<String> {
        return try {
            // Extract binary from assets
            val binaryFile = extractBinary()
            binaryPath = binaryFile.absolutePath
            
            // Generate config file
            val configFile = File(context.filesDir, CONFIG_NAME)
            val configResult = ConfigGenerator.generateConfigFile(config, configFile)
            if (configResult.isFailure) {
                return Result.failure(configResult.exceptionOrNull()!!)
            }
            configPath = configFile.absolutePath
            
            Result.success(binaryPath!!)
        } catch (e: Exception) {
            Result.failure(NullClawException("Setup failed: ${e.message}", e))
        }
    }
    
    /**
     * Start the NullClaw process
     * @return Result success or failure
     */
    fun start(): Result<Unit> {
        if (isRunningFlag.get()) {
            return Result.failure(NullClawException("NullClaw is already running"))
        }
        
        val binary = binaryPath ?: return Result.failure(NullClawException("Not set up. Call setup() first."))
        val config = configPath ?: return Result.failure(NullClawException("Config not generated. Call setup() first."))
        
        return try {
            val processBuilder = ProcessBuilder(binary, "--config", config)
                .directory(context.filesDir)
                .redirectErrorStream(true)
            
            process = processBuilder.start()
            isRunningFlag.set(true)
            
            // Start output monitor thread
            startOutputMonitor()
            
            // Start health check thread
            startHealthMonitor()
            
            Result.success(Unit)
        } catch (e: Exception) {
            isRunningFlag.set(false)
            Result.failure(NullClawException("Failed to start NullClaw: ${e.message}", e))
        }
    }
    
    /**
     * Stop the NullClaw process gracefully
     */
    fun stop() {
        if (!isRunningFlag.get()) return
        
        try {
            process?.destroy()
            
            // Wait up to 5 seconds for graceful shutdown
            val exited = process?.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) ?: true
            
            if (!exited) {
                // Force kill if graceful shutdown didn't work
                process?.destroyForcibly()
                process?.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
            // Log but don't throw - best effort cleanup
        } finally {
            process = null
            isRunningFlag.set(false)
        }
    }
    
    /**
     * Check if the NullClaw process is running
     */
    fun isRunning(): Boolean {
        if (!isRunningFlag.get()) return false
        
        // Double-check process is actually alive
        val alive = process?.isAlive ?: false
        if (!alive && isRunningFlag.get()) {
            isRunningFlag.set(false)
        }
        
        return isRunningFlag.get()
    }
    
    /**
     * Get the process PID (for debugging)
     */
    fun getPid(): Long? {
        return if (isRunning()) {
            process?.pid()
        } else {
            null
        }
    }
    
    private fun extractBinary(): File {
        val outFile = File(context.filesDir, BINARY_NAME)
        
        // Check if already extracted
        if (outFile.exists()) {
            return outFile
        }
        
        // Copy from assets
        context.assets.open(BINARY_NAME).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // Set executable permissions
        outFile.setExecutable(true, false)
        outFile.setReadable(true, false)
        
        return outFile
    }
    
    private fun startOutputMonitor() {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Log output (in production, route to proper logger)
                    println("[$TAG] $line")
                }
            } catch (e: Exception) {
                if (isRunningFlag.get()) {
                    println("[$TAG] Output monitor error: ${e.message}")
                }
            }
        }.apply {
            name = "NullClaw-OutputMonitor"
            isDaemon = true
            start()
        }
    }
    
    private fun startHealthMonitor() {
        Thread {
            while (isRunningFlag.get()) {
                try {
                    Thread.sleep(5000) // Check every 5 seconds
                    
                    if (process?.isAlive == false && isRunningFlag.get()) {
                        println("[$TAG] Process died unexpectedly")
                        isRunningFlag.set(false)
                        // Could trigger restart or callback here
                        break
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("[$TAG] Health monitor error: ${e.message}")
                }
            }
        }.apply {
            name = "NullClaw-HealthMonitor"
            isDaemon = true
            start()
        }
    }
}

class NullClawException(message: String, cause: Throwable? = null) : Exception(message, cause)
