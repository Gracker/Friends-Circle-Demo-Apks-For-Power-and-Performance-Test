package com.example.launch.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import com.example.launch.common.BuildConfig
import com.example.launch.common.LifecycleLoadSimulator
import com.example.launch.common.LoadSimulator
import com.example.launch.common.PerformanceLogger

class MainActivity : AppCompatActivity() {

    private var startTime: Long = 0
    private var isColdStart = true
    private lateinit var lifecycleSim: LifecycleLoadSimulator
    private var webView: WebView? = null
    private lateinit var statusText: TextView

    companion object {
        var isProcessCold = true
    }

    @SuppressLint("SetJavaScriptEnabled")
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

        // Phase 2: Activity Init Load (Blocking)
        LoadSimulator.onActivityCreate(this, loadType)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Header for status
        statusText = TextView(this).apply {
             text = "Loading..."
             textSize = 18f
             setTextColor(Color.DKGRAY)
             gravity = Gravity.CENTER
             setPadding(20, 20, 20, 20)
             setBackgroundColor(Color.LTGRAY)
        }
        mainLayout.addView(statusText)

        // Info Panel
        val infoText = TextView(this).apply {
            text = "Package: $packageName\nType: WebView\nLoad: $loadType\nStart: $startType"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setLineSpacing(8f, 1f)
            setPadding(20, 10, 20, 20)
            setBackgroundColor(Color.LTGRAY)
        }
        mainLayout.addView(infoText)

        val webViewContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(WebAppInterface(this@MainActivity, loadType, startType, startTime), "Android")
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        webViewContainer.addView(webView)
        mainLayout.addView(webViewContainer)
        
        // Native controls to restart
        // Controls removed
        
        setContentView(mainLayout)

        // Begin Hybrid Load: Load URL -> JS Network Sim -> Finish
        webView!!.loadUrl("file:///android_asset/load.html?type=${loadType.name}")
    }

    override fun onStart() {
        super.onStart()
        if (::lifecycleSim.isInitialized) lifecycleSim.onStart(this)
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        if (::lifecycleSim.isInitialized) lifecycleSim.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    class WebAppInterface(
        private val activity: MainActivity,
        private val loadType: LoadSimulator.LoadType,
        private val startType: String,
        private val startTime: Long
    ) {
        @JavascriptInterface
        fun onLoadFinished() {
            activity.runOnUiThread {
                // Phase 4: Final UI Freeze (Jank) - Simulate rendering the web content
                LoadSimulator.simulateFinalFreeze(loadType)

                val duration = System.currentTimeMillis() - startTime
                PerformanceLogger.log("WebView", loadType, startType, duration)
                
                activity.title = "Finished in ${duration}ms"
                activity.statusText.text = "Finished in ${duration}ms"
                activity.statusText.setTextColor(Color.WHITE)
                activity.statusText.setBackgroundColor(Color.parseColor("#4CAF50"))
                activity.statusText.textSize = 24f
                activity.statusText.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }
}