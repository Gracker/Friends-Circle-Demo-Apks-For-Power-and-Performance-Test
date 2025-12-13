package com.example.launch.aosp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

        // UI Setup
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        val statusText = TextView(this).apply {
            text = "Initializing UI..."
            textSize = 18f
        }
        val infoText = TextView(this).apply {
            text = "Launch AOSP\nLoad: $loadType\nStart: $startType"
            textSize = 20f
        }
        
        val btnCold = Button(this).apply {
            text = "Kill Process"
            setOnClickListener { android.os.Process.killProcess(android.os.Process.myPid()) }
        }
        val btnWarm = Button(this).apply {
            text = "Restart Activity"
            setOnClickListener {
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        layout.addView(infoText)
        layout.addView(statusText)
        layout.addView(btnCold)
        layout.addView(btnWarm)
        setContentView(layout)

        // Phase 3: Async Network / Progressive Load
        lifecycleScope.launch {
            LoadSimulator.simulateAsyncNetworkLoad(this@MainActivity, loadType) { progress ->
                statusText.text = progress
            }
            
            // Done
            val duration = System.currentTimeMillis() - startTime
            PerformanceLogger.log("AOSP", loadType, startType, duration)
            statusText.text = "Finished in ${duration}ms"
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