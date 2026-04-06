package com.loa.momclaw.util

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MOMCLAW Logger - Structured logging utility with file output support
 * 
 * Features:
 * - Multiple log levels (VERBOSE, DEBUG, INFO, WARN, ERROR)
 * - File logging with rotation
 * - Tag-based filtering
 * - Thread-safe buffer for recent logs
 * 
 * Usage:
 *   MomClawLogger.i("MyTag", "Message")
 *   MomClawLogger.e("MyTag", "Error", exception)
 *   MomClawLogger.enableFileLogging(cacheDir)
 */
object MomClawLogger {
    
    private const val TAG = "MOMCLAW"
    private const val MAX_BUFFER_SIZE = 1000
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    
    private val isEnabled = AtomicBoolean(true)
    private val fileLoggingEnabled = AtomicBoolean(false)
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    data class LogEntry(
        val timestamp: Long,
        val level: Level,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    ) {
        override fun toString(): String {
            val time = dateFormat.format(Date(timestamp))
            val throwableStr = throwable?.let { " | ${it.stackTraceToString()}" } ?: ""
            return "[$time] ${level.name.first()}/$tag: $message$throwableStr"
        }
    }
    
    /**
     * Enable or disable logging
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
    }
    
    /**
     * Enable file logging to specified directory
     */
    fun enableFileLogging(directory: File) {
        try {
            logFile = File(directory, "MOMCLAW-${System.currentTimeMillis()}.log")
            fileWriter = FileWriter(logFile, true)
            fileLoggingEnabled.set(true)
            i(TAG, "File logging enabled: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable file logging", e)
        }
    }
    
    /**
     * Disable file logging
     */
    fun disableFileLogging() {
        fileLoggingEnabled.set(false)
        try {
            fileWriter?.close()
        } catch (_: Exception) {}
        fileWriter = null
        logFile = null
    }
    
    /**
     * Log verbose message
     */
    fun v(tag: String, message: String) {
        log(Level.VERBOSE, tag, message)
    }
    
    /**
     * Log debug message
     */
    fun d(tag: String, message: String) {
        log(Level.DEBUG, tag, message)
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String) {
        log(Level.INFO, tag, message)
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }
    
    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (!isEnabled.get()) return
        
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        // Add to buffer
        addToBuffer(entry)
        
        // Log to Android
        when (level) {
            Level.VERBOSE -> Log.v(tag, message)
            Level.DEBUG -> Log.d(tag, message)
            Level.INFO -> Log.i(tag, message)
            Level.WARN -> Log.w(tag, message, throwable)
            Level.ERROR -> Log.e(tag, message, throwable)
        }
        
        // Log to file
        if (fileLoggingEnabled.get()) {
            writeToFile(entry)
        }
    }
    
    private fun addToBuffer(entry: LogEntry) {
        if (logBuffer.size >= MAX_BUFFER_SIZE) {
            logBuffer.poll()
        }
        logBuffer.offer(entry)
    }
    
    private fun writeToFile(entry: LogEntry) {
        try {
            // Check file size and rotate if needed
            logFile?.let { file ->
                if (file.length() > MAX_FILE_SIZE) {
                    rotateLogFile(file)
                }
            }
            
            fileWriter?.apply {
                write(entry.toString())
                write("\n")
                flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    private fun rotateLogFile(oldFile: File) {
        try {
            fileWriter?.close()
            
            // Rename old file
            val backupFile = File(oldFile.parent, "${oldFile.name}.old")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            oldFile.renameTo(backupFile)
            
            // Create new file
            logFile = File(oldFile.parent, "MOMCLAW-${System.currentTimeMillis()}.log")
            fileWriter = FileWriter(logFile, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    /**
     * Get recent log entries
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logBuffer.toList().takeLast(count)
    }
    
    /**
     * Get logs filtered by tag
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return logBuffer.filter { it.tag == tag }
    }
    
    /**
     * Get logs filtered by level
     */
    fun getLogsByLevel(level: Level): List<LogEntry> {
        return logBuffer.filter { it.level == level }
    }
    
    /**
     * Clear log buffer
     */
    fun clearBuffer() {
        logBuffer.clear()
    }
    
    /**
     * Export logs to string
     */
    fun exportLogs(): String {
        return logBuffer.joinToString("\n") { it.toString() }
    }
    
    /**
     * Export logs to file
     */
    fun exportLogsToFile(file: File): Boolean {
        return try {
            file.writeText(exportLogs())
            true
        } catch (e: Exception) {
            e(TAG, "Failed to export logs to file", e)
            false
        }
    }
}

/**
 * Extension functions for easier logging
 */
fun Any.logV(message: String) = MomClawLogger.v(this::class.java.simpleName, message)
fun Any.logD(message: String) = MomClawLogger.d(this::class.java.simpleName, message)
fun Any.logI(message: String) = MomClawLogger.i(this::class.java.simpleName, message)
fun Any.logW(message: String, throwable: Throwable? = null) = MomClawLogger.w(this::class.java.simpleName, message, throwable)
fun Any.logE(message: String, throwable: Throwable? = null) = MomClawLogger.e(this::class.java.simpleName, message, throwable)
