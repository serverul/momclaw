package com.loa.momclaw.agent

import android.content.Context
import android.content.pm.ApplicationInfo
import com.loa.momclaw.agent.model.AgentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * NullClaw Bridge — Binary wrapper for NullClaw agent process
 * 
 * Manages the lifecycle of the NullClaw Zig binary:
 * - Extract binary from assets (nullclaw-arm64, nullclaw-arm32, nullclaw-x86_64)
 * - Generate configuration file (nullclaw-config.json)
 * - Start/stop the agent process
 * - Monitor process health
 * - Capture process output for debugging
 * 
 * Architecture:
 *   Android App → NullClawBridge → NullClaw Binary (Zig) → LiteRT Bridge (localhost:8080)
 * 
 * The NullClaw binary provides:
 * - Tool execution (shell, file operations)
 * - Memory management (SQLite)
 * - System prompt handling
 * - Conversation context
 */
class NullClawBridge(private val context: Context) {
    
    private val processRef = AtomicReference<Process?>(null)
    private val configPath = AtomicReference<String?>(null)
    private val isSetup = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    
    // Supported ABIs
    private val abiMapping = mapOf(
        "arm64-v8a" to "nullclaw-arm64",
        "armeabi-v7a" to "nullclaw-arm32",
        "x86_64" to "nullclaw-x86_64",
        "x86" to "nullclaw-x86"
    )
    
    /**
     * Setup the NullClaw binary and configuration.
     * Must be called before start().
     * 
     * @param config Agent configuration
     * @return Result with setup status or error
     */
    suspend fun setup(config: AgentConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Setting up NullClaw bridge..." }
            
            // 1. Extract binary from assets
            val binaryFile = extractBinary()
            if (!binaryFile.exists()) {
                return@withContext Result.failure(IOException("Failed to extract NullClaw binary"))
            }
            
            // Make executable
            binaryFile.setExecutable(true, false)
            binaryFile.setReadable(true, false)
            
            logger.info { "Binary extracted and made executable: ${binaryFile.absolutePath}" }
            
            // 2. Ensure database directory exists
            val dbDir = File(config.memoryPath).parentFile
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs()
            }
            
            // 3. Generate configuration file
            val configFile = generateConfig(config)
            configPath.set(configFile.absolutePath)
            
            logger.info { "Config generated at: ${configFile.absolutePath}" }
            
            isSetup.set(true)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to setup NullClaw bridge" }
            Result.failure(e)
        }
    }
    
    /**
     * Start the NullClaw agent process.
     * setup() must be called first.
     * 
     * @return Result with start status or error
     */
    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isSetup.get()) {
            return@withContext Result.failure(IllegalStateException("NullClaw not set up. Call setup() first."))
        }
        
        if (isRunning.get()) {
            logger.warn { "NullClaw already running" }
            return@withContext Result.success(Unit)
        }
        
        try {
            logger.info { "Starting NullClaw agent process..." }
            
            val binaryPath = getBinaryPath()
            val config = configPath.get() ?: return@withContext Result.failure(
                IllegalStateException("Config path not set")
            )
            
            // Build process command
            val processBuilder = ProcessBuilder(
                binaryPath,
                "--config", config,
                "gateway",
                "--mode", "local",
                "--bind", "loopback",
                "--port", "9090"
            ).apply {
                redirectErrorStream(true)
                directory(context.filesDir)
                
                // Set environment variables
                environment()["NULLCLAW_LOG_LEVEL"] = if (isDebugBuild()) "debug" else "info"
                environment()["NULLCLAW_DATA_DIR"] = context.filesDir.absolutePath
            }
            
            val process = processBuilder.start()
            processRef.set(process)
            
            // Start output reader thread
            startOutputReader(process)
            
            // Wait for startup (give process time to initialize)
            Thread.sleep(STARTUP_DELAY_MS)
            
            if (process.isAlive) {
                isRunning.set(true)
                logger.info { "NullClaw agent started successfully (PID: ${getPid()})" }
                Result.success(Unit)
            } else {
                val exitCode = process.exitValue()
                logger.error { "NullClaw failed to start (exit code: $exitCode)" }
                Result.failure(IOException("NullClaw process exited immediately with code $exitCode"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to start NullClaw agent" }
            Result.failure(e)
        }
    }
    
    /**
     * Stop the NullClaw agent process gracefully.
     */
    fun stop() {
        val process = processRef.getAndSet(null)
        if (process != null) {
            logger.info { "Stopping NullClaw agent..." }
            
            try {
                // Try graceful shutdown first
                process.destroy()
                
                // Wait for process to terminate
                Thread.sleep(1000)
                
                // Force kill if still running
                if (process.isAlive) {
                    logger.warn { "NullClaw didn't stop gracefully, forcing..." }
                    process.destroyForcibly()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error stopping NullClaw" }
            }
        }
        
        isRunning.set(false)
        logger.info { "NullClaw agent stopped" }
    }
    
    /**
     * Check if the NullClaw process is running.
     */
    fun isRunning(): Boolean {
        val process = processRef.get() ?: return false
        val alive = process.isAlive
        if (!alive && isRunning.get()) {
            isRunning.set(false)
            logger.warn { "NullClaw process died unexpectedly" }
        }
        return alive
    }
    
    /**
     * Get the PID of the running process, or null if not running.
     */
    fun getPid(): Long? {
        val process = processRef.get() ?: return null
        return try {
            // Android Process doesn't expose pid() directly, use reflection
            val pidField = process.javaClass.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getLong(process)
        } catch (e: Exception) {
            // Fallback: return thread ID as placeholder
            Thread.currentThread().id
        }
    }
    
    /**
     * Get the HTTP endpoint for the NullClaw agent.
     */
    fun getEndpoint(): String = "http://localhost:9090"
    
    /**
     * Check health endpoint of the NullClaw agent.
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        if (!isRunning()) return@withContext false
        
        try {
            val url = java.net.URL("${getEndpoint()}/health")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            logger.warn { "Health check failed: ${e.message}" }
            false
        }
    }
    
    // ==================== Private Methods ====================
    
    /**
     * Extract the appropriate binary for the current device ABI.
     */
    private fun extractBinary(): File {
        val abi = getSupportedAbi()
        val assetName = abiMapping[abi] ?: "nullclaw-arm64"
        
        val outputFile = File(context.filesDir, "nullclaw")
        
        // Check if already extracted and up to date
        if (outputFile.exists()) {
            logger.debug { "Binary already exists: ${outputFile.absolutePath}" }
            return outputFile
        }
        
        logger.info { "Extracting binary for ABI: $abi (asset: $assetName)" }
        
        // Try to extract from assets
        try {
            context.assets.open(assetName).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile
        } catch (e: IOException) {
            // Asset not found, try generic name
            logger.warn { "Asset $assetName not found, trying 'nullclaw'" }
            
            try {
                context.assets.open("nullclaw").use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return outputFile
            } catch (e2: IOException) {
                // Create stub binary for testing
                logger.warn { "No binary found, creating stub for testing" }
                createStubBinary(outputFile)
                return outputFile
            }
        }
    }
    
    /**
     * Create a stub binary script for testing when actual binary is not available.
     */
    private fun createStubBinary(file: File) {
        file.writeText("""
            #!/system/bin/sh
            # NullClaw Stub for Testing
            echo "NullClaw Stub Mode - LiteRT-only operation"
            echo "Agent endpoint: http://localhost:9090"
            
            # Simple HTTP server stub using netcat (if available)
            while true; do
                sleep 60
            done
        """.trimIndent())
        file.setExecutable(true)
    }
    
    /**
     * Get the path to the extracted binary.
     */
    private fun getBinaryPath(): String {
        return File(context.filesDir, "nullclaw").absolutePath
    }
    
    /**
     * Get the primary ABI for this device.
     */
    private fun getSupportedAbi(): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        } else {
            @Suppress("DEPRECATION")
            android.os.Build.CPU_ABI
        }
    }
    
    /**
     * Check if this is a debug build.
     */
    private fun isDebugBuild(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Generate the NullClaw configuration file.
     */
    private fun generateConfig(config: AgentConfig): File {
        val configFile = File(context.filesDir, "nullclaw-config.json")
        configFile.writeText(generateConfigJson(config))
        return configFile
    }
    
    /**
     * Generate JSON configuration for NullClaw.
     */
    private fun generateConfigJson(config: AgentConfig): String {
        return """
        {
          "agents": {
            "defaults": {
              "model": {
                "primary": "${config.modelPrimary}"
              },
              "system_prompt": ${escapeJson(config.systemPrompt)}
            }
          },
          "models": {
            "providers": {
              "litert-bridge": {
                "type": "custom",
                "base_url": "${config.baseUrl}",
                "api_format": "openai"
              }
            }
          },
          "memory": {
            "backend": "${config.memoryBackend}",
            "path": "${config.memoryPath}"
          },
          "tools": {
            "enabled": ["shell", "file_read", "file_write"],
            "shell": {
              "allowed_commands": ["ls", "cat", "echo", "pwd", "date"],
              "timeout_ms": 5000
            }
          },
          "gateway": {
            "mode": "local",
            "bind": "loopback",
            "port": 9090
          },
          "inference": {
            "temperature": ${config.temperature},
            "max_tokens": ${config.maxTokens}
          }
        }
        """.trimIndent()
    }
    
    /**
     * Escape a string for JSON.
     */
    private fun escapeJson(str: String): String {
        return "\"" + str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
    
    /**
     * Start a thread to read process output for debugging.
     */
    private fun startOutputReader(process: Process) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    logger.debug { "[NullClaw] $line" }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    logger.error(e) { "Error reading NullClaw output" }
                }
            }
        }.apply {
            name = "NullClaw-OutputReader"
            isDaemon = true
            start()
        }
    }
    
    companion object {
        private const val STARTUP_DELAY_MS = 2000L
    }
}
