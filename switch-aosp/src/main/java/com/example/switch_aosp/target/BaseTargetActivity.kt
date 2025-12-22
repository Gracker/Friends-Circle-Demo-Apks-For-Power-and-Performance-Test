package com.example.switch_aosp.target

import android.os.Bundle
import android.os.Trace
import android.view.Choreographer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.switch_aosp.R
import com.example.switch_common.SwitchLoadManager
import com.example.switch_common.SwitchLoadType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 目标 Activity 基类
 *
 * 所有负载类型的目标 Activity 都继承此类
 * 在 onCreate 中执行真实的自身负载：
 * - 真实的 XML 布局 inflate
 * - 真实的自定义 View 创建
 * - 真实的 Binder IPC 调用
 * - 真实的文件 IO 操作
 *
 * 第一帧渲染完成后通知 Switch 完成，后台负载将在 1 秒后自动停止
 */
abstract class BaseTargetActivity : AppCompatActivity() {

    protected abstract val loadType: SwitchLoadType

    private val startTime = System.currentTimeMillis()
    private lateinit var viewContainer: LinearLayout
    private var switchCompleteNotified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("TargetActivity_onCreate_${loadType.name}")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target)

        // 获取 View 容器（用于真实 inflate）
        viewContainer = findViewById(R.id.viewContainer)

        // 执行真实的自身负载，传入容器用于真实 inflate
        SwitchLoadManager.executeSelfLoad(this, loadType, viewContainer)

        // 设置 UI 信息
        setupUI()

        // 在第一帧渲染完成后通知 Switch 完成
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
                android.util.Log.d("SwitchPerf", "Switch complete in ${duration}ms, " +
                        "scheduling background load stop")
                SwitchLoadManager.notifySwitchComplete(this)
                Trace.endSection()
            }
        }
    }

    private fun setupUI() {
        val tvLoadType = findViewById<TextView>(R.id.tvLoadType)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvTimestamp = findViewById<TextView>(R.id.tvTimestamp)
        val btnBack = findViewById<Button>(R.id.btnBack)

        tvLoadType.text = loadType.displayName
        tvDescription.text = SwitchLoadManager.getLoadDescription(loadType)

        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val loadDuration = System.currentTimeMillis() - startTime
        val viewCount = viewContainer.childCount
        tvTimestamp.text = "Time: ${dateFormat.format(Date(startTime))} | " +
                "Load: ${loadDuration}ms | Views: $viewCount"

        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止所有负载
        SwitchLoadManager.stopAllLoads(this)
    }
}
