package com.example.switch_flutter.target

import android.os.Bundle
import android.os.Trace
import android.view.Choreographer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.switch_flutter.FlutterStyleView
import com.example.switch_flutter.R
import com.example.switch_common.SwitchLoadManager
import com.example.switch_common.SwitchLoadType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Flutter 风格目标 Activity 基类
 *
 * 执行真实负载：
 * - 真实的 XML 布局 inflate
 * - 真实的自定义 View 创建
 * - 真实的 Binder/IO 操作
 * - Flutter 风格的 Canvas 渲染
 *
 * 第一帧渲染完成后通知 Switch 完成，后台负载将在 1 秒后自动停止
 */
abstract class BaseTargetActivity : AppCompatActivity() {

    protected abstract val loadType: SwitchLoadType

    private val startTime = System.currentTimeMillis()
    private lateinit var flutterStyleView: FlutterStyleView
    private lateinit var viewContainer: LinearLayout
    private var switchCompleteNotified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("FlutterTargetActivity_onCreate_${loadType.name}")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target)

        // 获取容器
        viewContainer = findViewById(R.id.viewContainer)

        // 1. 执行真实的 Native 层自身负载
        SwitchLoadManager.executeSelfLoad(this, loadType, viewContainer)

        // 2. 设置 Flutter 风格 View
        flutterStyleView = findViewById(R.id.flutterStyleView)
        flutterStyleView.loadLevel = loadType.selfLoad
        flutterStyleView.loadTypeName = loadType.displayName

        // 3. 设置 UI 信息
        setupUI()

        // 4. 返回按钮
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 5. 在第一帧渲染完成后通知 Switch 完成
        scheduleNotifySwitchComplete()

        Trace.endSection()
    }

    /**
     * 使用 RealLoadExecutor 的完成回调来判断真正的完成时机
     * 所有延迟任务（包括第一帧后的任务）执行完毕后才通知完成
     */
    private fun scheduleNotifySwitchComplete() {
        com.example.switch_common.RealLoadExecutor.setCompletionCallback {
            if (!switchCompleteNotified) {
                switchCompleteNotified = true
                val duration = System.currentTimeMillis() - startTime
                Trace.beginSection("NotifySwitchComplete")
                android.util.Log.d("SwitchPerf", "Flutter switch complete in ${duration}ms (all tasks finished)")
                SwitchLoadManager.notifySwitchComplete(this)
                Trace.endSection()
            }
        }
    }

    private fun setupUI() {
        val tvLoadType = findViewById<TextView>(R.id.tvLoadType)
        val tvTimestamp = findViewById<TextView>(R.id.tvTimestamp)

        tvLoadType.text = "Flutter: ${loadType.displayName}"

        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val loadDuration = System.currentTimeMillis() - startTime
        val viewCount = viewContainer.childCount
        tvTimestamp.text = "Time: ${dateFormat.format(Date(startTime))} | " +
                "Load: ${loadDuration}ms | Views: $viewCount"
    }

    override fun onDestroy() {
        super.onDestroy()
        SwitchLoadManager.stopAllLoads(this)
    }
}
