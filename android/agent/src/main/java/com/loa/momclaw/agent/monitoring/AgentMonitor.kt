package com.loa.momclaw.agent.monitoring

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


/**
 * Monitoring and Diagnostics for NullClaw Agent
 *
 * Features:
 * - Process lifecycle monitoring
 * - Health status tracking
 * - Performance metrics
 * - Diagnostic logging
 */
class AgentMonitor(private val context: Context) {

    private val startTime = AtomicLong(0)
    private val requestCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val lastError = ConcurrentHashMap<String, String>()

    /**
     * Agent health status
     */
    data class AgentHealth(
        val status: Status,
        val process: ProcessStatus,
        val bridge: BridgeStatus,
        val uptime: Long,
        val metrics: AgentMetrics
    )

    enum class Status {
        RUNNING, STOPPED, ERROR, STARTING, STOPPING
    }

    data class ProcessStatus(
        val isAlive: Boolean,
        val pid: Long?,
        val exitCode: Int?
    )

    data class BridgeStatus(
        val connected: Boolean,
        val endpoint: String,
        val latencyMs: Long?
    )

    data class AgentMetrics(
        val requestsTotal: Long,
        val errorsTotal: Long,
        val errorRate: Double,
        val avgLatencyMs: Double
    )

    /**
     * Record agent start
     */
    fun recordStart() {
        startTime.set(System.currentTimeMillis())
        // TODO: Add logging
    }

    /**
     * Record agent stop
     */
    fun recordStop() {
        startTime.set(0)
        // TODO: Add logging
    }

    /**
     * Record request
     */
    fun recordRequest() {
        requestCount.incrementAndGet()
    }

    /**
     * Record error
     */
    fun recordError(type: String, message: String) {
        errorCount.incrementAndGet()
        lastError["type"] = type
        lastError["message"] = message
        lastError["time"] = System.currentTimeMillis().toString()

        // TODO: Add logging
    }

    /**
     * Check agent health
     */
    suspend fun checkHealth(
        processAlive: Boolean,
        pid: Long?,
        exitCode: Int?,
        bridgeConnected: Boolean,
        bridgeEndpoint: String
    ): AgentHealth = withContext(Dispatchers.IO) {

        val processStatus = ProcessStatus(
            isAlive = processAlive,
            pid = pid,
            exitCode = if (!processAlive) exitCode else null
        )

        // Measure bridge latency
        val bridgeLatency = if (bridgeConnected) {
            measureBridgeLatency(bridgeEndpoint)
        } else null

        val bridgeStatus = BridgeStatus(
            connected = bridgeConnected,
            endpoint = bridgeEndpoint,
            latencyMs = bridgeLatency
        )

        val requests = requestCount.get()
        val errors = errorCount.get()

        val metrics = AgentMetrics(
            requestsTotal = requests,
            errorsTotal = errors,
            errorRate = if (requests > 0) errors.toDouble() / requests else 0.0,
            avgLatencyMs = 0.0 // TODO: Track actual latencies
        )

        val status = when {
            !processAlive && exitCode != null -> Status.ERROR
            !processAlive -> Status.STOPPED
            !bridgeConnected -> Status.STARTING
            else -> Status.RUNNING
        }

        val uptime = if (startTime.get() > 0) {
            System.currentTimeMillis() - startTime.get()
        } else 0

        AgentHealth(
            status = status,
            process = processStatus,
            bridge = bridgeStatus,
            uptime = uptime,
            metrics = metrics
        )
    }

    /**
     * Measure latency to LiteRT bridge
     */
    private suspend fun measureBridgeLatency(endpoint: String): Long? = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val url = java.net.URL("$endpoint/health")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()

            if (code == 200) {
                System.currentTimeMillis() - start
            } else null
        } catch (e: Exception) {
            // TODO: Add logging
            null
        }
    }

    /**
     * Get diagnostic info
     */
    fun getDiagnostics(): Diagnostics {
        return Diagnostics(
            platform = "Android ${android.os.Build.VERSION.SDK_INT}",
            device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            startTime = startTime.get(),
            lastError = lastError.toMap(),
            memoryInfo = getMemoryInfo()
        )
    }

    private fun getMemoryInfo(): Map<String, Long> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "total_mb" to runtime.totalMemory() / (1024 * 1024),
            "free_mb" to runtime.freeMemory() / (1024 * 1024),
            "max_mb" to runtime.maxMemory() / (1024 * 1024),
            "used_mb" to (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        )
    }

    /**
     * Reset metrics
     */
    fun reset() {
        requestCount.set(0)
        errorCount.set(0)
        lastError.clear()
    }

    data class Diagnostics(
        val platform: String,
        val device: String,
        val abi: String,
        val startTime: Long,
        val lastError: Map<String, String>,
        val memoryInfo: Map<String, Long>
    )
}

/**
 * Process lifecycle listener interface
 */
interface ProcessLifecycleListener {
    fun onProcessStarted(pid: Long)
    fun onProcessStopped(exitCode: Int)
    fun onProcessError(error: Throwable)
}

/**
 * Default lifecycle listener implementation
 */
class DefaultLifecycleListener : ProcessLifecycleListener {

    override fun onProcessStarted(pid: Long) {
        // TODO: Add logging
    }
    
    override fun onProcessStopped(exitCode: Int) {
        // TODO: Add logging
    }
    
    override fun onProcessError(error: Throwable) {
        // TODO: Add logging
    }
}
