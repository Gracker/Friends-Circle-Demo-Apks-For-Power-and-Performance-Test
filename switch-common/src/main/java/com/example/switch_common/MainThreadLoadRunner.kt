package com.example.switch_common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Trace
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.Executors
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 主线程负载模拟器
 *
 * 模拟目标 Activity 自身的复杂性：
 * - XML 解析和 View 创建
 * - 自定义 View 的 measure/layout/draw
 * - Binder 调用（获取系统服务等）
 * - IO 操作（读取配置、缓存等）
 * - 数据处理（解析 JSON、数据绑定等）
 *
 * 这些操作混合执行，模拟真实 App 的 Activity 启动流程
 */
object MainThreadLoadRunner {

    private const val TAG = "MainThreadLoadRunner"
    private const val SEED = 123456789L

    @Volatile
    var blackHole: Double = 0.0

    @Volatile
    var keepAlive: Any? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newCachedThreadPool()

    /**
     * 执行自身负载
     * 在目标 Activity 的 onCreate 中调用
     */
    fun executeSelfLoad(context: Context, level: SelfLoadLevel) {
        if (level == SelfLoadLevel.NONE) return

        Trace.beginSection("SwitchLoad_Self_${level.name}")
        try {
            when (level) {
                SelfLoadLevel.NONE -> { /* 无操作 */ }
                SelfLoadLevel.LIGHT -> executeLightLoad(context)
                SelfLoadLevel.MEDIUM -> executeMediumLoad(context)
                SelfLoadLevel.HEAVY -> executeHeavyLoad(context)
            }
        } finally {
            Trace.endSection()
        }
    }

    /**
     * 启动背景线程负载
     * 模拟 App 启动时的后台初始化任务
     */
    fun startBackgroundThreadLoad(context: Context, level: SelfLoadLevel) {
        if (level == SelfLoadLevel.NONE) return

        val threadCount = when (level) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 2
            SelfLoadLevel.MEDIUM -> 4
            SelfLoadLevel.HEAVY -> 8
        }

        repeat(threadCount) { threadId ->
            backgroundExecutor.submit {
                runBackgroundTask(context, threadId, level)
            }
        }
    }

    /**
     * 注入延迟任务
     * 模拟 Activity 启动后的异步回调、数据加载等
     */
    fun injectDelayedTasks(context: Context, level: SelfLoadLevel) {
        if (level == SelfLoadLevel.NONE) return

        val random = Random(SEED + 999)
        val taskCount = when (level) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 3
            SelfLoadLevel.MEDIUM -> 8
            SelfLoadLevel.HEAVY -> 15
        }

        repeat(taskCount) { i ->
            val delay = (random.nextDouble() * 500).toLong() // 0-500ms 延迟
            mainHandler.postDelayed({
                Trace.beginSection("DelayedTask_$i")
                try {
                    when (random.nextInt(4)) {
                        0 -> runCpuLoad(random, 10_000 + random.nextInt(20_000))
                        1 -> runBinderLoad(context, 1)
                        2 -> runMemoryLoad(1)
                        3 -> runStringProcessing(random, 50)
                    }
                    // 模拟真实 App 的间歇性
                    if (random.nextBoolean()) {
                        Thread.sleep(random.nextInt(2).toLong())
                    }
                } finally {
                    Trace.endSection()
                }
            }, delay)
        }
    }

    // ==================== 不同级别的负载实现 ====================

    /**
     * 轻负载 - 简单 Activity
     * 约 20 个 View，少量操作
     */
    private fun executeLightLoad(context: Context) {
        val random = Random(SEED)

        // 1. 模拟简单 XML 解析 (20 个 View)
        Trace.beginSection("Light_XMLParse")
        simulateViewInflation(20)
        Trace.endSection()

        // 2. 少量 Binder 调用
        Trace.beginSection("Light_Binder")
        runBinderLoad(context, 2)
        Trace.endSection()

        // 3. 简单数据处理
        Trace.beginSection("Light_DataProcess")
        runCpuLoad(random, 30_000)
        Trace.endSection()

        // 随机间隙
        if (random.nextBoolean()) {
            Thread.sleep(1)
        }
    }

    /**
     * 中等负载 - 中等复杂 Activity
     * 约 100 个 View，自定义 View，Binder 调用
     */
    private fun executeMediumLoad(context: Context) {
        val random = Random(SEED)

        // 1. 模拟中等 XML 解析 (100 个 View)
        Trace.beginSection("Medium_XMLParse")
        simulateViewInflation(100)
        Trace.endSection()

        // 2. 自定义 View 模拟
        Trace.beginSection("Medium_CustomView")
        simulateCustomViewOperations(random, 5)
        Trace.endSection()

        // 3. Binder 调用
        Trace.beginSection("Medium_Binder")
        runBinderLoad(context, 5)
        Trace.endSection()

        // 4. IO 操作 (读取配置)
        Trace.beginSection("Medium_IO")
        runIoLoad(context, 256)
        Trace.endSection()

        // 5. 数据处理
        Trace.beginSection("Medium_DataProcess")
        runCpuLoad(random, 80_000)
        runStringProcessing(random, 100)
        Trace.endSection()

        // 混合一些随机操作
        runMixedChaosLoop(context, random, 30)
    }

    /**
     * 重负载 - 复杂 Activity
     * 约 300+ View，大量自定义 View，Binder、IO、数据处理
     */
    private fun executeHeavyLoad(context: Context) {
        val random = Random(SEED)

        // 1. 大量 XML 解析 (300+ View)
        Trace.beginSection("Heavy_XMLParse")
        simulateViewInflation(300)
        Trace.endSection()

        // 2. 大量自定义 View 模拟
        Trace.beginSection("Heavy_CustomView")
        simulateCustomViewOperations(random, 20)
        Trace.endSection()

        // 3. 大量 Binder 调用
        Trace.beginSection("Heavy_Binder")
        runBinderLoad(context, 15)
        Trace.endSection()

        // 4. 大量 IO 操作
        Trace.beginSection("Heavy_IO")
        runIoLoad(context, 1024)
        runIoLoad(context, 512)
        Trace.endSection()

        // 5. 数据库操作
        Trace.beginSection("Heavy_SQLite")
        runSqliteLoad(context)
        Trace.endSection()

        // 6. 大量数据处理
        Trace.beginSection("Heavy_DataProcess")
        runCpuLoad(random, 200_000)
        runStringProcessing(random, 300)
        runCryptoLoad(random)
        Trace.endSection()

        // 7. 内存分配
        Trace.beginSection("Heavy_Memory")
        runMemoryLoad(3)
        Trace.endSection()

        // 8. 混合 Chaos 操作
        runMixedChaosLoop(context, random, 80)

        // 9. 模拟复杂数据绑定
        Trace.beginSection("Heavy_DataBinding")
        simulateDataBinding(500)
        Trace.endSection()
    }

    // ==================== 具体操作实现 ====================

    /**
     * 模拟 View 创建和解析
     */
    private fun simulateViewInflation(count: Int) {
        val clazz = try { Class.forName("java.lang.String") } catch (e: Exception) { return }
        repeat(count) {
            try {
                clazz.methods
                clazz.declaredFields
            } catch (e: Exception) { }
        }
    }

    /**
     * 模拟自定义 View 的 measure/layout/draw 操作
     */
    private fun simulateCustomViewOperations(random: Random, count: Int) {
        repeat(count) {
            // 模拟 onMeasure
            var measureResult = 0.0
            repeat(100) { i ->
                measureResult += sin(i * 0.1 + random.nextDouble()) * sqrt(i + 1.0)
            }
            blackHole += measureResult

            // 模拟 onLayout
            var layoutResult = 0.0
            repeat(50) { i ->
                layoutResult += random.nextDouble() * i
            }
            blackHole += layoutResult

            // 模拟 onDraw - Canvas 操作
            var drawResult = 0
            repeat(200) {
                drawResult += random.nextInt(255)
            }
            blackHole += drawResult.toDouble()
        }
    }

    /**
     * 模拟数据绑定（大量对象创建和处理）
     */
    private fun simulateDataBinding(iterations: Int) {
        val dataList = ArrayList<Any>(iterations)

        repeat(iterations) { i ->
            val item = HashMap<String, Any>()
            item["id"] = i
            item["title"] = "Item Title $i"
            item["description"] = "This is the description for item $i"
            item["timestamp"] = System.currentTimeMillis()
            item["enabled"] = i % 2 == 0

            val subList = ArrayList<Int>(5)
            repeat(5) { j -> subList.add(j * i) }
            item["children"] = subList

            dataList.add(item)
            blackHole += item.hashCode().toDouble()
        }

        // 模拟数据访问
        for (item in dataList) {
            blackHole += item.hashCode().toDouble()
        }

        keepAlive = dataList
        dataList.clear()
        keepAlive = null
    }

    /**
     * 混合 Chaos 循环
     * 模拟真实 App 的各种混合操作
     */
    private fun runMixedChaosLoop(context: Context, random: Random, iterations: Int) {
        repeat(iterations) {
            Trace.beginSection("ChaosTask")

            val taskType = random.nextInt(20)
            when {
                taskType < 3 -> runCpuLoad(random, 30_000)
                taskType < 5 -> runCryptoLoad(random)
                taskType < 10 -> runBinderLoad(context, 1)
                taskType < 13 -> runMemoryLoad(1)
                taskType < 16 -> runIoLoad(context, 128)
                else -> runStringProcessing(random, 30)
            }

            Trace.endSection()

            // 随机间隙 (50% 概率，0-2ms)
            if (random.nextBoolean()) {
                try {
                    Thread.sleep(random.nextInt(3).toLong())
                } catch (e: Exception) { }
            }
        }
    }

    /**
     * 后台任务
     */
    private fun runBackgroundTask(context: Context, taskId: Int, level: SelfLoadLevel) {
        val random = Random(SEED + taskId)
        val iterations = when (level) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 20
            SelfLoadLevel.MEDIUM -> 50
            SelfLoadLevel.HEAVY -> 100
        }

        repeat(iterations) {
            Trace.beginSection("BgTask_$taskId")
            when (random.nextInt(4)) {
                0 -> runCpuLoad(random, 20_000)
                1 -> runIoLoad(context, 256)
                2 -> runMemoryLoad(1)
                3 -> runBinderLoad(context, 1)
            }
            Trace.endSection()

            try {
                Thread.sleep((random.nextInt(10) + 1).toLong())
            } catch (e: Exception) { }
        }
    }

    // ==================== 基础操作 ====================

    private fun runCpuLoad(random: Random, iterations: Int) {
        var result = 0.0
        repeat(iterations) { i ->
            val a = random.nextDouble()
            val b = random.nextDouble()
            result += sin(a) * sqrt(b)
        }
        blackHole += result
    }

    private fun runBinderLoad(context: Context, calls: Int) {
        val pm = context.packageManager
        val packageName = context.packageName
        repeat(calls) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }
            } catch (e: Exception) { }
        }
    }

    private fun runIoLoad(context: Context, sizeBytes: Int) {
        val file = File(context.cacheDir, "switch_load_${Thread.currentThread().id}.tmp")
        try {
            val data = ByteArray(sizeBytes)
            data[0] = 1
            data[data.lastIndex] = 2
            FileOutputStream(file).use { it.write(data) }
            val readData = file.inputStream().use { it.readBytes() }
            blackHole += readData.size.toDouble()
        } catch (e: Exception) {
            Log.w(TAG, "IO load failed", e)
        } finally {
            file.delete()
        }
    }

    private fun runMemoryLoad(mb: Int) {
        try {
            val size = 1024 * 1024 * mb
            val array = ByteArray(size)
            array[0] = 1
            blackHole += array[0].toDouble()
        } catch (e: OutOfMemoryError) { }
    }

    private fun runCryptoLoad(random: Random) {
        try {
            val data = ByteArray(4096)
            random.nextBytes(data)
            val digest = MessageDigest.getInstance("SHA-256")
            repeat(30) {
                digest.reset()
                val hash = digest.digest(data)
                blackHole += hash[0].toDouble()
                data[0] = hash[0]
            }
        } catch (e: Exception) { }
    }

    private fun runSqliteLoad(context: Context) {
        val dbName = "switch_load.db"
        var db: android.database.sqlite.SQLiteDatabase? = null
        try {
            db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS temp_load (id INTEGER PRIMARY KEY, content TEXT)")
            repeat(10) { i ->
                db.execSQL("INSERT OR REPLACE INTO temp_load (id, content) VALUES ($i, 'Content $i')")
            }
            val cursor = db.rawQuery("SELECT * FROM temp_load", null)
            while (cursor.moveToNext()) {
                blackHole += cursor.getString(1).length.toDouble()
            }
            cursor.close()
        } catch (e: Exception) { }
        finally {
            db?.close()
        }
    }

    private fun runStringProcessing(random: Random, count: Int) {
        val sb = StringBuilder()
        repeat(count) { i ->
            sb.append("Task_").append(i).append("_")
                .append(random.nextInt(1000)).append("_")
        }
        val result = sb.toString().uppercase().replace("_", "-")
        blackHole += result.length.toDouble()
    }
}
