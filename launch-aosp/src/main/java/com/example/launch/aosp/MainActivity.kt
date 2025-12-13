package com.example.launch.aosp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import com.example.launch.common.BuildConfig
import com.example.launch.common.LifecycleLoadSimulator
import com.example.launch.common.LoadSimulator
import com.example.launch.common.PerformanceLogger
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var startTime: Long = 0
    private var isColdStart = true
    private lateinit var lifecycleSim: LifecycleLoadSimulator

    override fun onCreate(savedInstanceState: Bundle?) {
        startTime = System.currentTimeMillis() // Capture time ASAP
        super.onCreate(savedInstanceState)

        // Get Load Type
        val loadTypeStr = BuildConfig.LOAD_TYPE
        val loadType = try {
            LoadSimulator.LoadType.valueOf(loadTypeStr)
        } catch (e: Exception) {
            LoadSimulator.LoadType.LIGHT
        }
        
        val startType = if (savedInstanceState == null && isProcessCold) "cold" else "warm"
        isProcessCold = false

        // Init Lifecycle Simulator
        lifecycleSim = LifecycleLoadSimulator(loadType)
        lifecycleSim.onCreate(this)

        // Phase 2: Activity Init Load (Blocking) - Kept for legacy "Activity Create" load
        LoadSimulator.onActivityCreate(this, loadType)

        // UI Setup - Aesthetic & Centered
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#121212")) // Dark background

        val centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        
        val infoText = TextView(this).apply {
            text = "Package: $packageName\nType: AOSP\nLoad: $loadType\nStart: $startType"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setLineSpacing(10f, 1f)
            setPadding(0, 0, 0, 60)
        }

        val statusText = TextView(this).apply {
            text = "Initializing..."
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        
        // Controls removed

        // Helper and controls removed
        // Buttons removed as per request
        
        centerLayout.addView(infoText)
        centerLayout.addView(statusText)
        // Controls removed
        root.addView(centerLayout)
        
        setContentView(root)

        // Phase 3: Async Network / Progressive Load
        lifecycleScope.launch {
            LoadSimulator.simulateAsyncNetworkLoad(this@MainActivity, loadType) { progress ->
                statusText.text = progress
            }
            
            // Done
            val duration = System.currentTimeMillis() - startTime
            PerformanceLogger.log("AOSP", loadType, startType, duration)
            statusText.text = "Finished\n${duration}ms"
            statusText.setTextColor(Color.parseColor("#4CAF50")) // Green
            statusText.textSize = 40f
        }
    }

    override fun onStart() {
        super.onStart()
        if (::lifecycleSim.isInitialized) {
            lifecycleSim.onStart(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::lifecycleSim.isInitialized) {
            lifecycleSim.onResume(this)
        }
    }

    companion object {
        var isProcessCold = true
    }
}