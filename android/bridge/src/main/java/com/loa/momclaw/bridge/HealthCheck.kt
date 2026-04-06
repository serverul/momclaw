package com.loa.momclaw.bridge

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger

/**
 * Health Check System for LiteRT Bridge
 * 
 * Monitors:
 * - Server status
 * - Model status
 * - Memory usage
 * - Disk space
 * - Request metrics
 */
class HealthMonitor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var requestCount = 0L
    private var errorCount = 0L
    private var lastRequestTime: Long = 0
    private var startTime: Long = 0
    
    /**
     * Complete health check result
     */
    data class HealthStatus(
        val status: Status,
        val server: ServerHealth,
        val model: ModelHealth,
        val memory: MemoryHealth,
        val disk: DiskHealth,
        val metrics: MetricsHealth
    )
    
    enum class Status {
        HEALTHY, DEGRADED, UNHEALTHY
    }
    
    data class ServerHealth(
        val isRunning: Boolean,
        val uptimeMs: Long,
        val port: Int?
    )
    
    data class ModelHealth(
        val isLoaded: Boolean,
        val modelName: String?,
        val modelPath: String?,
        val loadTimeMs: Long?
    )
    
    data class MemoryHealth(
        val usedMB: Long,
        val availableMB: Long,
        val totalMB: Long,
        val lowMemory: Boolean
    )
    
    data class DiskHealth(
        val modelsDir: String,
        val availableMB: Long,
        val totalMB: Long
    )
    
    data class MetricsHealth(
        val totalRequests: Long,
        val errorCount: Long,
        val errorRate: Double,
        val lastRequestAgoMs: Long
    )
    
    /**
     * Record server start
     */
    fun recordStart(port: Int) {
        startTime = System.currentTimeMillis()
        logger.info { "Health monitor started (port: $port)" }
    }
    
    /**
     * Record server stop
     */
    fun recordStop() {
        startTime = 0
        logger.info { "Health monitor stopped" }
    }
    
    /**
     * Record a request
     */
    fun recordRequest() {
        requestCount++
        lastRequestTime = System.currentTimeMillis()
    }
    
    /**
    * Record an error
    */
    fun recordError() {
        errorCount++
    }
    
    /**
     * Perform complete health check
     */
    suspend fun checkHealth(
        serverRunning: Boolean,
        port: Int?,
        modelLoaded: Boolean,
        modelName: String?,
        modelPath: String?,
        modelLoadTime: Long?
    ): HealthStatus = withContext(Dispatchers.IO) {
        
        // Server health
        val serverHealth = ServerHealth(
            isRunning = serverRunning,
            uptimeMs = if (startTime > 0) System.currentTimeMillis() - startTime else 0,
            port = port
        )
        
        // Model health
        val modelHealth = ModelHealth(
            isLoaded = modelLoaded,
            modelName = modelName,
            modelPath = modelPath,
            loadTimeMs = modelLoadTime
        )
        
        // Memory health
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val memoryHealth = MemoryHealth(
            usedMB = (activityManager.memoryClass * 1024 * 1024L) / (1024 * 1024),
            availableMB = memoryInfo.availMem / (1024 * 1024),
            totalMB = memoryInfo.totalMem / (1024 * 1024),
            lowMemory = memoryInfo.lowMemory
        )
        
        // Disk health
        val modelsDir = File(context.filesDir, "models")
        val stat = android.os.StatFs(modelsDir.absolutePath)
        val diskHealth = DiskHealth(
            modelsDir = modelsDir.absolutePath,
            availableMB = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024),
            totalMB = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
        )
        
        // Metrics
        val metricsHealth = MetricsHealth(
            totalRequests = requestCount,
            errorCount = errorCount,
            errorRate = if (requestCount > 0) errorCount.toDouble() / requestCount else 0.0,
            lastRequestAgoMs = if (lastRequestTime > 0) System.currentTimeMillis() - lastRequestTime else -1
        )
        
        // Determine overall status
        val status = when {
            !serverRunning -> Status.UNHEALTHY
            !modelLoaded -> Status.DEGRADED
            memoryHealth.lowMemory -> Status.DEGRADED
            metricsHealth.errorRate > 0.5 -> Status.DEGRADED
            else -> Status.HEALTHY
        }
        
        HealthStatus(
            status = status,
            server = serverHealth,
            model = modelHealth,
            memory = memoryHealth,
            disk = diskHealth,
            metrics = metricsHealth
        )
    }
    
    /**
     * Quick health check (just server + model)
     */
    fun quickCheck(serverRunning: Boolean, modelLoaded: Boolean): Status {
        return when {
            !serverRunning -> Status.UNHEALTHY
            !modelLoaded -> Status.DEGRADED
            else -> Status.HEALTHY
        }
    }
    
    /**
     * Get memory info for model loading
     */
    fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryInfo(
            availableMB = memoryInfo.availMem / (1024 * 1024),
            totalMB = memoryInfo.totalMem / (1024 * 1024),
            lowMemory = memoryInfo.lowMemory,
            thresholdMB = memoryInfo.threshold / (1024 * 1024)
        )
    }
    
    /**
     * Check if there's enough memory for model
     */
    fun canLoadModel(requiredMB: Long): Boolean {
        val mem = getMemoryInfo()
        return mem.availableMB >= requiredMB && !mem.lowMemory
    }
    
    /**
     * Reset metrics
     */
    fun resetMetrics() {
        requestCount = 0
        errorCount = 0
        lastRequestTime = 0
    }
    
    data class MemoryInfo(
        val availableMB: Long,
        val totalMB: Long,
        val lowMemory: Boolean,
        val thresholdMB: Long
    )
}

/**
 * Bridge health data class for API responses
 */
data class BridgeHealthResponse(
    val status: String,
    val server: Map<String, Any?>,
    val model: Map<String, Any?>,
    val memory: Map<String, Any?>,
    val disk: Map<String, Any?>,
    val metrics: Map<String, Any?>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Convert HealthStatus to API response
 */
fun HealthMonitor.HealthStatus.toResponse(): BridgeHealthResponse = BridgeHealthResponse(
    status = status.name.lowercase(),
    server = mapOf(
        "running" to server.isRunning,
        "uptime_ms" to server.uptimeMs,
        "port" to server.port
    ),
    model = mapOf(
        "loaded" to model.isLoaded,
        "name" to model.modelName,
        "path" to model.modelPath,
        "load_time_ms" to model.loadTimeMs
    ),
    memory = mapOf(
        "used_mb" to memory.usedMB,
        "available_mb" to memory.availableMB,
        "total_mb" to memory.totalMB,
        "low_memory" to memory.lowMemory
    ),
    disk = mapOf(
        "models_dir" to disk.modelsDir,
        "available_mb" to disk.availableMB,
        "total_mb" to disk.totalMB
    ),
    metrics = mapOf(
        "total_requests" to metrics.totalRequests,
        "error_count" to metrics.errorCount,
        "error_rate" to metrics.errorRate,
        "last_request_ago_ms" to metrics.lastRequestAgoMs
    )
)
