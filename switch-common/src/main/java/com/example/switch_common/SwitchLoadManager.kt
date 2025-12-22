package com.example.switch_common

import android.content.Context
import android.os.Trace
import android.util.Log
import android.view.ViewGroup

/**
 * Switch 负载管理器
 *
 * 统一管理自身负载和背景负载的启动和停止
 *
 * 使用真实负载执行器（RealLoadExecutor）执行：
 * - 真实的 XML 布局 inflate
 * - 真实的自定义 View 创建
 * - 真实的 Binder IPC 调用
 * - 真实的文件 IO 操作
 */
object SwitchLoadManager {

    private const val TAG = "SwitchLoadManager"
    private var initialized = false

    /**
     * 初始化负载管理器
     * 应在 Application.onCreate 或第一次使用前调用
     */
    fun init(context: Context) {
        if (!initialized) {
            RealLoadExecutor.initLayoutIds(context)
            initialized = true
            Log.d(TAG, "SwitchLoadManager initialized")
        }
    }

    /**
     * 在目标 Activity 创建前启动背景负载
     * 应在 MainActivity 点击按钮后、启动目标 Activity 前调用
     */
    fun startBackgroundLoad(context: Context, loadType: SwitchLoadType) {
        Trace.beginSection("SwitchLoad_StartBg")
        try {
            val bgLevel = loadType.bgLoad
            if (bgLevel != BackgroundLoadLevel.NONE) {
                Log.d(TAG, "Starting background load: $bgLevel")
                BackgroundLoadService.start(context, bgLevel)
            }
        } finally {
            Trace.endSection()
        }
    }

    /**
     * 在目标 Activity onCreate 中执行自身负载（使用真实负载）
     *
     * @param context Context
     * @param loadType 负载类型
     * @param container 可选的 ViewGroup 容器，用于添加真实 inflate 的 View
     */
    fun executeSelfLoad(context: Context, loadType: SwitchLoadType, container: ViewGroup? = null) {
        // 确保已初始化
        if (!initialized) {
            init(context)
        }

        Trace.beginSection("SwitchLoad_Self")
        try {
            val selfLevel = loadType.selfLoad
            if (selfLevel != SelfLoadLevel.NONE) {
                Log.d(TAG, "Executing REAL self load: $selfLevel")

                // 1. 真实的主线程同步负载（真实 inflate、真实自定义 View、真实 Binder、真实 IO）
                RealLoadExecutor.executeSelfLoad(context, selfLevel, container)

                // 2. 启动后台线程负载
                RealLoadExecutor.startBackgroundThreadLoad(context, selfLevel)

                // 3. 注入延迟任务
                RealLoadExecutor.injectDelayedTasks(context, selfLevel)
            }
        } finally {
            Trace.endSection()
        }
    }

    /**
     * 通知 Switch 完成
     * 调用此方法后，后台负载将在 1 秒后自动停止
     * 应在目标 Activity 的 onResume 或第一帧渲染完成后调用
     */
    fun notifySwitchComplete(context: Context) {
        Log.d(TAG, "Switch complete, background load will stop in 1 second")
        BackgroundLoadService.notifySwitchComplete(context)
        RealLoadExecutor.stopBackgroundTasks()
    }

    /**
     * 停止所有负载
     * 应在测试完成或返回主界面时调用
     */
    fun stopAllLoads(context: Context) {
        Log.d(TAG, "Stopping all loads")
        BackgroundLoadService.stop(context)
        RealLoadExecutor.stopBackgroundTasks()
    }

    /**
     * 获取负载描述信息
     */
    fun getLoadDescription(loadType: SwitchLoadType): String {
        val selfDesc = when (loadType.selfLoad) {
            SelfLoadLevel.NONE -> "No self load"
            SelfLoadLevel.LIGHT -> "Light: 20 XML + 5 CustomViews + 3 Binder + 2 IO"
            SelfLoadLevel.MEDIUM -> "Medium: 100 XML + 25 CustomViews + 10 Binder + 5 IO + SQLite"
            SelfLoadLevel.HEAVY -> "Heavy: 250+ XML + 75 CustomViews + 30 Binder + 20 IO + SQLite + Data"
        }

        val bgDesc = when (loadType.bgLoad) {
            BackgroundLoadLevel.NONE -> "No background load"
            BackgroundLoadLevel.MEDIUM -> "Bg: 4 threads + service(4 tasks) + 0-2ms sleep"
            BackgroundLoadLevel.HEAVY -> "Bg: 8 threads + service(6 tasks) + 0-2ms sleep"
        }

        return "$selfDesc\n$bgDesc"
    }
}
