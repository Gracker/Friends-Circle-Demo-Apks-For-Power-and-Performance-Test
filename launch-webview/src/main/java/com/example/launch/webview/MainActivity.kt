package com.example.launch.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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

        // Phase 2: Activity Init Load (Blocking)
        LoadSimulator.onActivityCreate(this, loadType)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(WebAppInterface(this@MainActivity, loadType, startType, startTime), "Android")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        // Native controls to restart
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
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

        layout.addView(webView)
        layout.addView(controls)
        setContentView(layout)

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
            val duration = System.currentTimeMillis() - startTime
            PerformanceLogger.log("WebView", loadType, startType, duration)
        }
    }
}