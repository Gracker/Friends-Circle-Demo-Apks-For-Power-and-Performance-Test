package com.example.launch.gl

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
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

    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: MyGLRenderer
    private var startTime: Long = 0
    private lateinit var lifecycleSim: LifecycleLoadSimulator

    companion object {
        var isProcessCold = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        startTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)

        val loadTypeStr = BuildConfig.LOAD_TYPE
        val loadType = try {
            LoadSimulator.LoadType.valueOf(loadTypeStr)
        } catch (e: Exception) {
            LoadSimulator.LoadType.LIGHT
        }
        
        val startType = if (savedInstanceState == null && isProcessCold) "cold" else "warm"
        isProcessCold = false

        lifecycleSim = LifecycleLoadSimulator(loadType)
        lifecycleSim.onCreate(this)

        // Phase 2: Activity Init Load
        LoadSimulator.onActivityCreate(this, loadType)

        // Setup GL
        glView = GLSurfaceView(this)
        glView.setEGLContextClientVersion(2)
        
        // Renderer callback to log final time
        renderer = MyGLRenderer(this, loadType) {
            val duration = System.currentTimeMillis() - startTime
            PerformanceLogger.log("GL", loadType, startType, duration)
            runOnUiThread {
                 // Update UI to "Playing"
            }
        }
        glView.setRenderer(renderer)

        // UI Overlay
        val root = FrameLayout(this)
        root.addView(glView)
        
        val statusText = TextView(this).apply {
            text = "Initializing..."
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(20, 100, 0, 0)
        }
        
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 200, 0, 0)
        }
        controls.addView(Button(this).apply {
            text = "Kill Process"
            setOnClickListener { android.os.Process.killProcess(android.os.Process.myPid()) }
        })
        controls.addView(Button(this).apply {
            text = "Restart"
            setOnClickListener {
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
        
        root.addView(statusText)
        root.addView(controls)
        setContentView(root)
        
        // Phase 3: Async Network Load -> Then Trigger GL Assets
        lifecycleScope.launch {
            LoadSimulator.simulateAsyncNetworkLoad(this@MainActivity, loadType) { progress ->
                statusText.text = progress
            }
            
            // Network Done, trigger GL Upload
            statusText.text = "Uploading Assets to GPU..."
            glView.queueEvent {
                renderer.startAssetLoading()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::lifecycleSim.isInitialized) lifecycleSim.onStart(this)
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
        if (::lifecycleSim.isInitialized) lifecycleSim.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }
}