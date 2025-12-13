package com.example.launch.common

import android.util.Log

object PerformanceLogger {

    private const val TAG = "LaunchPerformance"

    fun log(appType: String, loadType: LoadSimulator.LoadType, startType: String, durationMs: Long) {
        val message = "[AppType: $appType] [Load: ${loadType.name.lowercase()}] [StartType: $startType] [Duration: ${durationMs}ms]"
        Log.i(TAG, message)
        // Also print to System.out for CLI capture convenience
        println(message)
    }
}
