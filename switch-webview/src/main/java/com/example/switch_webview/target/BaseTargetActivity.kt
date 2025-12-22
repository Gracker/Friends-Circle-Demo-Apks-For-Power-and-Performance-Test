package com.example.switch_webview.target

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Trace
import android.view.Choreographer
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.switch_webview.R
import com.example.switch_common.SelfLoadLevel
import com.example.switch_common.SwitchLoadManager
import com.example.switch_common.SwitchLoadType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WebView 目标 Activity 基类
 *
 * 执行真实负载：
 * - 真实的 XML 布局 inflate
 * - 真实的自定义 View 创建
 * - 真实的 Binder/IO 操作
 * - WebView 加载 HTML
 *
 * 第一帧渲染完成后通知 Switch 完成，后台负载将在 1 秒后自动停止
 */
abstract class BaseTargetActivity : AppCompatActivity() {

    protected abstract val loadType: SwitchLoadType

    private val startTime = System.currentTimeMillis()
    private lateinit var webView: WebView
    private lateinit var viewContainer: LinearLayout
    private var switchCompleteNotified = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("WebViewTargetActivity_onCreate_${loadType.name}")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target)

        // 获取容器
        viewContainer = findViewById(R.id.viewContainer)

        // 1. 先执行真实的 Native 层自身负载
        SwitchLoadManager.executeSelfLoad(this, loadType, viewContainer)

        // 2. 设置 WebView
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // 3. 加载对应复杂度的 HTML
        val htmlFile = getHtmlFile()
        webView.loadUrl("file:///android_asset/$htmlFile")

        // 4. 设置 UI 信息
        setupUI()

        // 5. 返回按钮
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 6. 在第一帧渲染完成后通知 Switch 完成
        scheduleNotifySwitchComplete()

        Trace.endSection()
    }

    /**
     * 使用 Choreographer 在第一帧渲染完成后通知 Switch 完成
     */
    private fun scheduleNotifySwitchComplete() {
        Choreographer.getInstance().postFrameCallback { frameTimeNanos ->
            if (!switchCompleteNotified) {
                switchCompleteNotified = true
                val duration = System.currentTimeMillis() - startTime
                Trace.beginSection("NotifySwitchComplete")
                android.util.Log.d("SwitchPerf", "WebView switch complete in ${duration}ms, " +
                        "scheduling background load stop")
                SwitchLoadManager.notifySwitchComplete(this)
                Trace.endSection()
            }
        }
    }

    private fun setupUI() {
        val tvLoadType = findViewById<TextView>(R.id.tvLoadType)
        val tvTimestamp = findViewById<TextView>(R.id.tvTimestamp)

        tvLoadType.text = "WebView: ${loadType.displayName}"

        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val loadDuration = System.currentTimeMillis() - startTime
        val viewCount = viewContainer.childCount
        tvTimestamp.text = "Time: ${dateFormat.format(Date(startTime))} | " +
                "Load: ${loadDuration}ms | Native Views: $viewCount"
    }

    private fun getHtmlFile(): String {
        return when (loadType.selfLoad) {
            SelfLoadLevel.NONE -> "pure.html"
            SelfLoadLevel.LIGHT -> "light.html"
            SelfLoadLevel.MEDIUM -> "medium.html"
            SelfLoadLevel.HEAVY -> "heavy.html"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        SwitchLoadManager.stopAllLoads(this)
    }
}
