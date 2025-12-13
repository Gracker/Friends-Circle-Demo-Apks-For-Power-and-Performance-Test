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
                LoadSimulator.LoadType.LIGHT -> 10L
                LoadSimulator.LoadType.MEDIUM -> 100L
                LoadSimulator.LoadType.HEAVY -> 500L
            }
            try { Thread.sleep(sleepTime) } catch (e: Exception) {}
            
            // Do some "Parsing"
            LoadSimulator.simulateXmlParse(if (loadType == LoadSimulator.LoadType.HEAVY) 1000 else 100)
            
            configLatch?.countDown()
        }

        // 2. Main Thread "Scattered" Work
        // SharedPreferences (IO/Lock contention potential)
        val prefItems = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 5
            LoadSimulator.LoadType.MEDIUM -> 50
            LoadSimulator.LoadType.HEAVY -> 200
        }
        LoadSimulator.readSharedPreferences(context, prefItems)
    }

    fun onStart(context: Context) {
        // 1. Wait for Critical Config (Thread Synchronization Block)
        // This simulates the UI thread waiting for a background result needed to render the first frame.
        try {
            // If heavy load, we might wait up to 500ms here if the thread hasn't finished.
            configLatch?.await(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) { }

        // 2. Resource Decoding (Bitmap) - needed for UI
        // Simulates decoding a large header image
        if (loadType != LoadSimulator.LoadType.LIGHT) {
            LoadSimulator.simulateBitmapDecode(context, if (loadType == LoadSimulator.LoadType.HEAVY) 5 else 1)
        }
    }

    fun onResume(context: Context) {
        // 1. Final check / IPC before interaction
        // e.g., Checking clipboard or checking intent flags via Binder
        val binderCalls = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 1
            LoadSimulator.LoadType.MEDIUM -> 5
            LoadSimulator.LoadType.HEAVY -> 20
        }
        // We reuse the binder load from LoadSimulator (need to make it accessible or copy logic)
        // It's private in LoadSimulator. I'll make it public? 
        // Or just use a simpler check here.
        // Let's assume we do some string/cpu work representing "Last minute layout adjustment"
        LoadSimulator.simulateXmlParse(if (loadType == LoadSimulator.LoadType.HEAVY) 500 else 50)
    }
}
