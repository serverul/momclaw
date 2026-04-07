package com.loa.momclaw.util

import android.util.Log

/**
 * Simple logger wrapper for MOMCLAW
 */
object MomClawLogger {
    
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    fun w(tag: String, message: String, cause: Throwable) {
        Log.w(tag, message, cause)
    }
    
    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }
    
    fun e(tag: String, message: String, cause: Throwable) {
        Log.e(tag, message, cause)
    }
    
    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }
}
