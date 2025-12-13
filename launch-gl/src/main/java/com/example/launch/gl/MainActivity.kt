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
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity

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

        // Phase 1.5: Inject Message Queue Blockers
        LoadSimulator.injectMessageQueueBlockers(this, loadType)

        // Phase 2: Activity Init Load
        LoadSimulator.onActivityCreate(this, loadType)

        // UI Overlay - Centered
        val root = FrameLayout(this)

        val centerBucket = LinearLayout(this).apply {
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
            text = "Package: $packageName\nType: GL\nLoad: $loadType\nStart: $startType"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setLineSpacing(10f, 1f)
            setPadding(0, 0, 0, 40)
        }
        
        val statusText = TextView(this).apply {
            text = "Initializing..."
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Setup GL
        glView = GLSurfaceView(this)
        glView.setEGLContextClientVersion(2)
        
        // Renderer callback to log final time
        renderer = MyGLRenderer(this, loadType) {
            val duration = System.currentTimeMillis() - startTime
            PerformanceLogger.log("GL", loadType, startType, duration)
            runOnUiThread {
                 statusText.text = "Finished\n${duration}ms"
                 statusText.setTextColor(Color.parseColor("#4CAF50"))
                 statusText.textSize = 40f
            }
        }
        glView.setRenderer(renderer)
        
        root.addView(glView)
        
        // Center Controls
        // Controls removed
        
        centerBucket.addView(infoText)
        centerBucket.addView(statusText)
        
        root.addView(centerBucket) // Add the centered stack
        // Controls removed

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
            
            // Phase 4: Final UI Freeze (Simulate Main Thread Work concurrent with GL Upload)
            LoadSimulator.simulateFinalFreeze(loadType)
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