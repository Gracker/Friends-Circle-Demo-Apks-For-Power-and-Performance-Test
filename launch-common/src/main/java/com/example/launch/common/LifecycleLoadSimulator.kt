package com.example.launch.common

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LifecycleLoadSimulator(private val loadType: LoadSimulator.LoadType) {

    private var configLatch: CountDownLatch? = null
    
    fun onCreate(context: Context) {
        // 1. Trigger Async Config Load (Simulates reading heavy config from disk/network that is needed for UI)
        configLatch = CountDownLatch(1)
        thread(start = true, name = "BackgroundConfigLoader") {
            // Simulate work
            val sleepTime = when(loadType) {
                LoadSimulator.LoadType.LIGHT -> 125L
                LoadSimulator.LoadType.MEDIUM -> 250L
                LoadSimulator.LoadType.HEAVY -> 500L
            }
            try { Thread.sleep(sleepTime) } catch (e: Exception) {}
            
            // Do some "Parsing" - Ratio 1:2:4
            val xmlItems = when(loadType) {
                LoadSimulator.LoadType.LIGHT -> 250
                LoadSimulator.LoadType.MEDIUM -> 500
                LoadSimulator.LoadType.HEAVY -> 1000
            }
            LoadSimulator.simulateXmlParse(xmlItems)
            
            configLatch?.countDown()
        }

        // 2. Main Thread "Scattered" Work
        // SharedPreferences (IO/Lock contention potential)
        // SharedPreferences - Ratio 1:2:4
        val prefItems = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 25
            LoadSimulator.LoadType.MEDIUM -> 50
            LoadSimulator.LoadType.HEAVY -> 100
        }
        if (prefItems > 0) {
            LoadSimulator.readSharedPreferences(context, prefItems)
        }
    }

    fun onStart(context: Context) {
        // 1. Wait for Critical Config (Thread Synchronization Block)
        // This simulates the UI thread waiting for a background result needed to render the first frame.
        try {
            // Safer timeout: 300ms max for heavy
            val timeout = if (loadType == LoadSimulator.LoadType.HEAVY) 300L else 50L
            configLatch?.await(timeout, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) { }

        // 2. Resource Decoding (Bitmap) - needed for UI
        // Simulates decoding a large header image
        if (loadType != LoadSimulator.LoadType.LIGHT) {
            LoadSimulator.simulateBitmapDecode(context, if (loadType == LoadSimulator.LoadType.HEAVY) 3 else 1)
        }
    }

    fun onResume(context: Context) {
        // 1. Final check / IPC before interaction
        // e.g., Checking clipboard or checking intent flags via Binder
        // Ratio 1:2:4
        val binderCalls = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 5
            LoadSimulator.LoadType.MEDIUM -> 10
            LoadSimulator.LoadType.HEAVY -> 20
        }
        // We reuse the binder load from LoadSimulator (need to make it accessible or copy logic)
        // It's private in LoadSimulator. I'll make it public? 
        // Or just use a simpler check here.
        // Let's assume we do some string/cpu work representing "Last minute layout adjustment"
        val resumeXmlItems = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 75
            LoadSimulator.LoadType.MEDIUM -> 150
            LoadSimulator.LoadType.HEAVY -> 300
        }
        LoadSimulator.simulateXmlParse(resumeXmlItems)
    }
}
