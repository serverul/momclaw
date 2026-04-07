package com.loa.momclaw.agent

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NullClaw Bridge - Manages the NullClaw binary lifecycle.
 * 
 * Handles:
 * - Copying binary from assets to internal storage
 * - Generating configuration files
 * - Starting/stopping the NullClaw process
 * - Monitoring process health
 */
@Singleton
class NullClawBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var process: Process? = null
    private var configPath: String? = null
    private var isRunning = false

    companion object {
        private const val TAG = "NullClawBridge"
        private const val BINARY_NAME = "nullclaw"
        private const val CONFIG_NAME = "nullclaw-config.json"
        private const val DEFAULT_PORT = 9090
        private const val STARTUP_DELAY_MS = 3000L
    }

    /**
     * Sets up the NullClaw binary and configuration.
     * 
     * @param config Agent configuration
     * @return Result with setup status
     */
    fun setup(config: AgentConfig): Result<String> {
        return try {
            // Copy binary from assets to storage
            val binaryFile = copyBinaryToStorage()
            binaryFile.setExecutable(true)
            
            // Generate config file
            val configWithDbPath = config.copy(
                databasePath = getDatabasePath()
            )
            val configFile = generateConfig(configWithDbPath)
            configPath = configFile.absolutePath
            
            Log.i(TAG, "NullClaw setup complete: binary=${binaryFile.path}, config=${configPath}")
            
            Result.success("Setup complete")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to setup NullClaw", e)
            Result.failure(e)
        }
    }

    /**
     * Starts the NullClaw agent process.
     * 
     * @param port Port to run on (default 9090)
     * @return Result with start status
     */
    fun start(port: Int = DEFAULT_PORT): Result<Unit> {
        if (isRunning) {
            return Result.success(Unit)
        }

        return try {
            val binaryPath = File(context.filesDir, BINARY_NAME).absolutePath
            val config = configPath ?: throw IllegalStateException("Config not generated. Call setup() first.")

            // Build process command
            val processBuilder = ProcessBuilder(
                binaryPath,
                "--config", config,
                "gateway",
                "--port", port.toString()
            ).apply {
                redirectErrorStream(true)
                directory(context.filesDir)
                environment()["HOME"] = context.filesDir.absolutePath
            }

            // Start process
            process = processBuilder.start()
            
            // Wait for startup
            Thread.sleep(STARTUP_DELAY_MS)

            // Check if process is alive
            if (process?.isAlive == true) {
                isRunning = true
                Log.i(TAG, "NullClaw started successfully on port $port")
                
                // Start output reader thread
                startOutputReader()
                
                Result.success(Unit)
            } else {
                val exitCode = process?.exitValue() ?: -1
                Log.e(TAG, "NullClaw failed to start. Exit code: $exitCode")
                Result.failure(IOException("Failed to start NullClaw (exit code: $exitCode)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NullClaw", e)
            Result.failure(e)
        }
    }

    /**
     * Stops the NullClaw agent process.
     */
    fun stop() {
        try {
            process?.destroy()
            
            // Wait for process to terminate
            val exited = process?.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) ?: true
            
            if (!exited) {
                process?.destroyForcibly()
            }
            
            process = null
            isRunning = false
            
            Log.i(TAG, "NullClaw stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping NullClaw", e)
        }
    }

    /**
     * Checks if the NullClaw process is running.
     */
    fun isRunning(): Boolean = isRunning && process?.isAlive == true

    /**
     * Gets the process ID if running.
     */
    fun getPid(): Int? = if (isRunning) process?.pid() else null

    /**
     * Restarts the NullClaw process.
     */
    fun restart(port: Int = DEFAULT_PORT): Result<Unit> {
        stop()
        Thread.sleep(1000) // Wait for cleanup
        return start(port)
    }

    // Private helper methods

    /**
     * Copies the NullClaw binary from assets to internal storage.
     */
    private fun copyBinaryToStorage(): File {
        val outputFile = File(context.filesDir, BINARY_NAME)
        
        if (!outputFile.exists()) {
            context.assets.open(BINARY_NAME).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Binary copied to ${outputFile.absolutePath}")
        }
        
        return outputFile
    }

    /**
     * Generates the configuration file for NullClaw.
     */
    private fun generateConfig(config: AgentConfig): File {
        val configFile = File(context.filesDir, CONFIG_NAME)
        configFile.writeText(config.toJson())
        Log.i(TAG, "Config generated at ${configFile.absolutePath}")
        return configFile
    }

    /**
     * Gets the database path for NullClaw's SQLite storage.
     */
    private fun getDatabasePath(): String {
        val dbDir = File(context.filesDir, "databases")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        return File(dbDir, "agent.db").absolutePath
    }

    /**
     * Starts a thread to read process output for logging.
     */
    private fun startOutputReader() {
        Thread {
            try {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        Log.d(TAG, "[NullClaw] $line")
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading NullClaw output", e)
            }
        }.apply {
            name = "NullClaw-Output-Reader"
            isDaemon = true
            start()
        }
    }

    /**
     * Cleanup resources when destroyed.
     */
    protected fun finalize() {
        stop()
    }
}
