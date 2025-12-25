package com.example.switch_common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Trace
import android.util.Log
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.switch_common.views.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 真实负载执行器
 *
 * 执行真实的操作而不是模拟：
 * - 真实的 XML 布局 inflate
 * - 真实的自定义 View 创建和绑定
 * - 真实的 Binder IPC 调用
 * - 真实的文件 IO 操作
 * - 真实的数据处理
 *
 * 特点：
 * - 负载不仅在 Activity 生命周期内，还延伸到第一帧之后
 * - 任务时长可变（10-50ms 长任务，1-10ms 短任务）
 * - 使用固定种子确保每次测试结果一致（伪随机）
 * - 防止编译器优化（volatile blackHole + keepAlive）
 */
object RealLoadExecutor {

    private const val TAG = "RealLoadExecutor"
    private const val SEED = 0xDEADBEEFL

    // 防止编译器优化：使用 volatile 确保值不会被优化掉
    @Volatile
    var blackHole: Double = 0.0

    @Volatile
    var blackHoleLong: Long = 0L

    @Volatile
    var blackHoleInt: Int = 0

    @Volatile
    var keepAlive: Any? = null

    // 存储弱引用防止内存泄漏
    private var currentContextRef: WeakReference<Context>? = null
    private val isPostFrameLoadRunning = AtomicBoolean(false)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newCachedThreadPool()

    // 布局资源 ID 缓存
    private var lightLayoutIds: IntArray? = null
    private var mediumLayoutIds: IntArray? = null
    private var heavyLayoutIds: IntArray? = null

    /**
     * 初始化布局资源 ID
     * 需要在应用启动时调用，传入正确的包名以解析资源 ID
     */
    fun initLayoutIds(context: Context) {
        Trace.beginSection("RealLoad_InitLayoutIds")
        try {
            val resources = context.resources
            val packageName = context.packageName

            // 获取 light 布局 ID (1-20)
            lightLayoutIds = IntArray(20) { i ->
                resources.getIdentifier("layout_light_${i + 1}", "layout", packageName)
            }.filter { it != 0 }.toIntArray()

            // 获取 medium 布局 ID (1-100)
            mediumLayoutIds = IntArray(100) { i ->
                resources.getIdentifier("layout_medium_${i + 1}", "layout", packageName)
            }.filter { it != 0 }.toIntArray()

            // 获取 heavy 布局 ID (1-180)
            heavyLayoutIds = IntArray(180) { i ->
                resources.getIdentifier("layout_heavy_${i + 1}", "layout", packageName)
            }.filter { it != 0 }.toIntArray()

            Log.d(TAG, "Initialized layout IDs: light=${lightLayoutIds?.size}, " +
                    "medium=${mediumLayoutIds?.size}, heavy=${heavyLayoutIds?.size}")
        } finally {
            Trace.endSection()
        }
    }

    /**
     * 执行真实的自身负载
     *
     * @param context Context
     * @param level 负载级别
     * @param container 可选的 ViewGroup 容器，用于添加 inflate 的 View
     */
    fun executeSelfLoad(context: Context, level: SelfLoadLevel, container: ViewGroup? = null) {
        if (level == SelfLoadLevel.NONE) return

        Trace.beginSection("RealLoad_Self_${level.name}")
        val random = Random(SEED)

        try {
            when (level) {
                SelfLoadLevel.NONE -> { /* 无操作 */ }
                SelfLoadLevel.LIGHT -> executeLightLoad(context, random, container)
                SelfLoadLevel.MEDIUM -> executeMediumLoad(context, random, container)
                SelfLoadLevel.HEAVY -> executeHeavyLoad(context, random, container)
            }
        } finally {
            Trace.endSection()
        }
    }

    // ==================== 轻负载 ====================

    private fun executeLightLoad(context: Context, random: Random, container: ViewGroup?) {
        val inflater = LayoutInflater.from(context)

        // 1. 真实 inflate 20 个轻量级布局
        Trace.beginSection("Light_RealInflate")
        val layoutIds = lightLayoutIds ?: return
        for (i in 0 until minOf(20, layoutIds.size)) {
            val layoutId = layoutIds[i % layoutIds.size]
            if (layoutId != 0) {
                try {
                    val view = inflater.inflate(layoutId, container, false)
                    container?.addView(view)
                    blackHole += view.hashCode().toDouble()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to inflate layout_light_${i + 1}", e)
                }
            }
        }
        Trace.endSection()

        // 2. 创建 5 个轻量级自定义 View
        Trace.beginSection("Light_CustomViews")
        for (i in 0 until 5) {
            val customView = LightCustomView(context)
            customView.viewIndex = i
            customView.measure(
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST)
            )
            container?.addView(customView)
            blackHole += customView.measuredWidth.toDouble()
        }
        Trace.endSection()

        // 3. 少量 Binder 调用
        Trace.beginSection("Light_Binder")
        executeBinderCalls(context, 3)
        Trace.endSection()

        // 4. 少量 IO
        Trace.beginSection("Light_IO")
        executeFileIO(context, 2, 1024) // 2 个文件，每个 1KB
        Trace.endSection()
    }

    // ==================== 中等负载 ====================

    private fun executeMediumLoad(context: Context, random: Random, container: ViewGroup?) {
        val inflater = LayoutInflater.from(context)

        // 1. 真实 inflate 100 个中等复杂度布局
        Trace.beginSection("Medium_RealInflate")
        val layoutIds = mediumLayoutIds ?: return
        for (i in 0 until minOf(100, layoutIds.size)) {
            val layoutId = layoutIds[i % layoutIds.size]
            if (layoutId != 0) {
                try {
                    val view = inflater.inflate(layoutId, container, false)
                    container?.addView(view)
                    blackHole += view.hashCode().toDouble()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to inflate layout_medium_${i + 1}", e)
                }
            }

            // 穿插一些其他操作，模拟真实 App
            if (i % 10 == 0) {
                executeBinderCalls(context, 1)
            }
        }
        Trace.endSection()

        // 2. 创建 20 个中等复杂度自定义 View
        Trace.beginSection("Medium_CustomViews")
        for (i in 0 until 20) {
            val customView = MediumCustomView(context)
            customView.viewIndex = i
            customView.measure(
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(150, View.MeasureSpec.AT_MOST)
            )
            container?.addView(customView)
            blackHole += customView.measuredWidth.toDouble()
        }
        Trace.endSection()

        // 3. 数据绑定 View
        Trace.beginSection("Medium_DataBinding")
        for (i in 0 until 5) {
            val dataView = DataBindingView(context)
            dataView.dataComplexity = 20
            dataView.viewIndex = i
            container?.addView(dataView)
            blackHole += dataView.hashCode().toDouble()
        }
        Trace.endSection()

        // 4. Binder 调用
        Trace.beginSection("Medium_Binder")
        executeBinderCalls(context, 10)
        Trace.endSection()

        // 5. IO 操作
        Trace.beginSection("Medium_IO")
        executeFileIO(context, 5, 4096) // 5 个文件，每个 4KB
        Trace.endSection()

        // 6. 混合操作
        executeMixedOperations(context, random, 30)
    }

    // ==================== 重负载 ====================

    private fun executeHeavyLoad(context: Context, random: Random, container: ViewGroup?) {
        val inflater = LayoutInflater.from(context)

        // 1. 真实 inflate 180+ 个复杂布局
        Trace.beginSection("Heavy_RealInflate")
        val heavyIds = heavyLayoutIds ?: emptyList<Int>().toIntArray()
        val mediumIds = mediumLayoutIds ?: emptyList<Int>().toIntArray()
        val lightIds = lightLayoutIds ?: emptyList<Int>().toIntArray()

        // Inflate heavy layouts
        for (i in 0 until minOf(180, heavyIds.size)) {
            val layoutId = heavyIds[i % heavyIds.size]
            if (layoutId != 0) {
                try {
                    val view = inflater.inflate(layoutId, container, false)
                    container?.addView(view)
                    blackHole += view.hashCode().toDouble()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to inflate layout_heavy_${i + 1}", e)
                }
            }

            // 穿插其他操作
            if (i % 20 == 0) {
                executeBinderCalls(context, 2)
                if (random.nextBoolean()) {
                    Thread.sleep(random.nextInt(2).toLong())
                }
            }
        }

        // 额外 inflate medium 和 light 布局
        for (i in 0 until minOf(50, mediumIds.size)) {
            val layoutId = mediumIds[i]
            if (layoutId != 0) {
                try {
                    val view = inflater.inflate(layoutId, container, false)
                    container?.addView(view)
                    blackHole += view.hashCode().toDouble()
                } catch (e: Exception) { }
            }
        }

        for (i in 0 until minOf(20, lightIds.size)) {
            val layoutId = lightIds[i]
            if (layoutId != 0) {
                try {
                    val view = inflater.inflate(layoutId, container, false)
                    container?.addView(view)
                    blackHole += view.hashCode().toDouble()
                } catch (e: Exception) { }
            }
        }
        Trace.endSection()

        // 2. 创建 50 个重量级自定义 View
        Trace.beginSection("Heavy_CustomViews")
        for (i in 0 until 50) {
            val customView = HeavyCustomView(context)
            customView.viewIndex = i
            customView.measure(
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST)
            )
            container?.addView(customView)
            blackHole += customView.measuredWidth.toDouble()
        }
        Trace.endSection()

        // 3. 复杂容器 View
        Trace.beginSection("Heavy_Containers")
        for (i in 0 until 10) {
            val containerView = ComplexContainerView(context)
            containerView.containerIndex = i

            // 添加子 View
            for (j in 0 until 5) {
                val child = MediumCustomView(context)
                child.viewIndex = i * 5 + j
                containerView.addView(child)
            }

            containerView.measure(
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            container?.addView(containerView)
            blackHole += containerView.measuredHeight.toDouble()
        }
        Trace.endSection()

        // 4. 数据绑定 View
        Trace.beginSection("Heavy_DataBinding")
        for (i in 0 until 15) {
            val dataView = DataBindingView(context)
            dataView.dataComplexity = 50
            dataView.viewIndex = i
            container?.addView(dataView)
            blackHole += dataView.hashCode().toDouble()
        }
        Trace.endSection()

        // 5. 大量 Binder 调用
        Trace.beginSection("Heavy_Binder")
        executeBinderCalls(context, 30)
        Trace.endSection()

        // 6. 大量 IO 操作
        Trace.beginSection("Heavy_IO")
        executeFileIO(context, 15, 8192) // 15 个文件，每个 8KB
        executeRandomAccessIO(context, 5, 16384) // 5 个文件随机读写，每个 16KB
        Trace.endSection()

        // 7. SQLite 操作
        Trace.beginSection("Heavy_SQLite")
        executeSqliteOperations(context, 100)
        Trace.endSection()

        // 8. 混合 Chaos 操作
        executeMixedOperations(context, random, 100)

        // 9. 复杂数据处理
        Trace.beginSection("Heavy_DataProcessing")
        executeDataProcessing(random, 500)
        Trace.endSection()
    }

    // ==================== 具体操作实现 ====================

    /**
     * 真实的 Binder 调用
     * 注意：不使用 getInstalledPackages，避免需要 QUERY_ALL_PACKAGES 敏感权限
     */
    private fun executeBinderCalls(context: Context, count: Int) {
        val pm = context.packageManager
        val packageName = context.packageName

        repeat(count) {
            try {
                // 真实的 Binder IPC 调用 - 获取当前应用信息（不需要权限）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }

                // 获取当前应用的 ApplicationInfo（不需要权限）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }

                // 获取当前应用的资源信息（不需要权限）
                try {
                    pm.getResourcesForApplication(packageName)
                } catch (e: Exception) {
                    // 忽略异常
                }

                // 系统设置查询（不需要权限）
                context.contentResolver.query(
                    android.provider.Settings.System.CONTENT_URI,
                    null, null, null, null
                )?.close()

            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    /**
     * 真实的文件 IO 操作
     */
    private fun executeFileIO(context: Context, fileCount: Int, sizeBytes: Int) {
        val cacheDir = context.cacheDir

        repeat(fileCount) { i ->
            val file = File(cacheDir, "real_io_test_$i.tmp")
            try {
                // 写入
                val data = ByteArray(sizeBytes)
                Random(SEED + i).nextBytes(data)
                FileOutputStream(file).use { fos ->
                    fos.write(data)
                    fos.flush()
                }

                // 读取
                val readData = FileInputStream(file).use { fis ->
                    fis.readBytes()
                }

                blackHole += readData.size.toDouble()

            } catch (e: Exception) {
                Log.w(TAG, "IO operation failed", e)
            } finally {
                file.delete()
            }
        }
    }

    /**
     * 随机访问 IO
     */
    private fun executeRandomAccessIO(context: Context, fileCount: Int, sizeBytes: Int) {
        val cacheDir = context.cacheDir
        val random = Random(SEED)

        repeat(fileCount) { i ->
            val file = File(cacheDir, "random_io_test_$i.tmp")
            try {
                // 创建文件
                RandomAccessFile(file, "rw").use { raf ->
                    raf.setLength(sizeBytes.toLong())

                    // 随机位置写入
                    repeat(20) {
                        val pos = random.nextInt(sizeBytes - 100).toLong()
                        raf.seek(pos)
                        raf.writeInt(random.nextInt())
                    }

                    // 随机位置读取
                    repeat(20) {
                        val pos = random.nextInt(sizeBytes - 100).toLong()
                        raf.seek(pos)
                        blackHole += raf.readInt().toDouble()
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "Random IO failed", e)
            } finally {
                file.delete()
            }
        }
    }

    /**
     * SQLite 操作
     */
    private fun executeSqliteOperations(context: Context, rowCount: Int) {
        val dbName = "real_load_test.db"
        var db: android.database.sqlite.SQLiteDatabase? = null

        try {
            db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)

            // 创建表
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS test_data (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    value REAL,
                    data BLOB,
                    created_at INTEGER
                )
            """)

            // 插入数据
            val random = Random(SEED)
            repeat(rowCount) { i ->
                db.execSQL("""
                    INSERT OR REPLACE INTO test_data (id, name, value, data, created_at)
                    VALUES ($i, 'Item_$i', ${random.nextDouble() * 1000}, X'${String.format("%08X", random.nextInt())}', ${System.currentTimeMillis()})
                """)
            }

            // 查询数据
            val cursor = db.rawQuery("SELECT * FROM test_data ORDER BY value DESC LIMIT 50", null)
            while (cursor.moveToNext()) {
                blackHole += cursor.getDouble(2)
            }
            cursor.close()

            // 聚合查询
            val aggCursor = db.rawQuery("SELECT COUNT(*), AVG(value), SUM(value) FROM test_data", null)
            if (aggCursor.moveToNext()) {
                blackHole += aggCursor.getDouble(1) + aggCursor.getDouble(2)
            }
            aggCursor.close()

            // 清理
            db.execSQL("DELETE FROM test_data")

        } catch (e: Exception) {
            Log.w(TAG, "SQLite operation failed", e)
        } finally {
            db?.close()
            context.deleteDatabase(dbName)
        }
    }

    /**
     * 混合操作（模拟真实 App 的碎片化负载）
     */
    private fun executeMixedOperations(context: Context, random: Random, iterations: Int) {
        Trace.beginSection("MixedOps")

        repeat(iterations) {
            val taskType = random.nextInt(20)

            when {
                taskType < 3 -> {
                    // CPU 计算
                    var result = 0.0
                    repeat(10000) { i ->
                        result += sin(i * 0.01 + random.nextDouble()) * sqrt(i + 1.0)
                    }
                    blackHole += result
                }
                taskType < 5 -> {
                    // Crypto
                    val data = ByteArray(2048)
                    random.nextBytes(data)
                    val digest = MessageDigest.getInstance("SHA-256")
                    repeat(10) {
                        val hash = digest.digest(data)
                        blackHole += hash[0].toDouble()
                        data[0] = hash[0]
                    }
                }
                taskType < 10 -> {
                    // Binder
                    executeBinderCalls(context, 1)
                }
                taskType < 13 -> {
                    // Memory
                    val size = 1024 * (random.nextInt(512) + 128)
                    val array = ByteArray(size)
                    array[0] = 1
                    array[size - 1] = 2
                    blackHole += array.hashCode().toDouble()
                }
                else -> {
                    // 字符串处理
                    val sb = StringBuilder()
                    repeat(50) { i ->
                        sb.append("Task_").append(i).append("_")
                            .append(random.nextInt(1000)).append("_")
                    }
                    blackHole += sb.toString().uppercase().length.toDouble()
                }
            }

            // 随机间隙（模拟真实 App 的间歇性负载）
            if (random.nextInt(100) < 30) {
                try {
                    Thread.sleep(random.nextInt(3).toLong())
                } catch (e: Exception) { }
            }
        }

        Trace.endSection()
    }

    /**
     * 数据处理
     */
    private fun executeDataProcessing(random: Random, itemCount: Int) {
        // 创建数据
        val dataList = ArrayList<HashMap<String, Any>>(itemCount)
        repeat(itemCount) { i ->
            val item = HashMap<String, Any>()
            item["id"] = i
            item["title"] = "Item Title $i"
            item["description"] = "Description for item $i with some extra text"
            item["value"] = sin(i * 0.1) * 100
            item["enabled"] = i % 3 != 0
            item["children"] = (0 until (i % 5 + 1)).map { "Child_${i}_$it" }
            dataList.add(item)
        }

        // 排序
        dataList.sortByDescending { (it["value"] as Double) }

        // 过滤
        val filtered = dataList.filter { it["enabled"] as Boolean }

        // 分组
        val grouped = filtered.groupBy { (it["id"] as Int) % 5 }

        // 聚合
        var sum = 0.0
        for (item in filtered) {
            sum += item["value"] as Double
        }

        keepAlive = Triple(dataList, grouped, sum)
        blackHole += sum

        // 清理
        dataList.clear()
        keepAlive = null
    }

    /**
     * 启动后台线程负载
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
                0 -> {
                    var result = 0.0
                    repeat(20000) { i ->
                        result += sin(i * 0.01) * sqrt(i + 1.0)
                    }
                    blackHole += result
                }
                1 -> executeFileIO(context, 1, 256)
                2 -> {
                    val size = 1024 * 1024
                    try {
                        val array = ByteArray(size)
                        array[0] = 1
                        blackHole += array[0].toDouble()
                    } catch (e: OutOfMemoryError) { }
                }
                3 -> executeBinderCalls(context, 1)
            }
            Trace.endSection()

            try {
                Thread.sleep((random.nextInt(10) + 1).toLong())
            } catch (e: Exception) { }
        }
    }

    /**
     * 注入延迟任务（包含第一帧后的负载）
     *
     * 模拟真实 App 在 Activity 生命周期之外的负载：
     * - 第一帧后继续加载 Layout
     * - 延迟加载 Bitmap
     * - 子线程传数据
     * - 读取 IO 等操作
     *
     * 任务时长可变：
     * - 长任务：10-50ms
     * - 短任务：1-10ms
     */
    fun injectDelayedTasks(context: Context, level: SelfLoadLevel) {
        if (level == SelfLoadLevel.NONE) return

        currentContextRef = WeakReference(context)
        val random = Random(SEED + 999)

        // 第一阶段：在第一帧前调度的延迟任务（0-500ms）
        val immediateTaskCount = when (level) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 3
            SelfLoadLevel.MEDIUM -> 8
            SelfLoadLevel.HEAVY -> 15
        }

        repeat(immediateTaskCount) { i ->
            val delay = (random.nextDouble() * 500).toLong()
            val taskDuration = getVariedTaskDuration(random, isLongTask = random.nextInt(100) < 30)
            mainHandler.postDelayed({
                executeVariedDurationTask(context, random, i, taskDuration, "ImmediateTask")
            }, delay)
        }

        // 第二阶段：使用 Choreographer 在第一帧后执行的任务
        schedulePostFrameLoads(context, level)
    }

    /**
     * 调度第一帧后的负载
     * 使用 Choreographer 确保在第一帧渲染完成后执行
     */
    private fun schedulePostFrameLoads(context: Context, level: SelfLoadLevel) {
        if (!isPostFrameLoadRunning.compareAndSet(false, true)) return

        val random = Random(SEED + 1000)

        // 在第一帧后执行
        Choreographer.getInstance().postFrameCallback { frameTimeNanos ->
            Log.d(TAG, "First frame completed, starting post-frame loads")

            val postFrameTaskCount = when (level) {
                SelfLoadLevel.NONE -> 0
                SelfLoadLevel.LIGHT -> 5
                SelfLoadLevel.MEDIUM -> 12
                SelfLoadLevel.HEAVY -> 25
            }

            // 第一帧后立即执行的任务
            repeat(postFrameTaskCount) { i ->
                // 延迟分布：100ms - 2000ms，模拟持续的后台加载
                val delay = 100L + (random.nextDouble() * 1900).toLong()
                val isLongTask = random.nextInt(100) < 40 // 40% 概率是长任务
                val taskDuration = getVariedTaskDuration(random, isLongTask)

                mainHandler.postDelayed({
                    currentContextRef?.get()?.let { ctx ->
                        executePostFrameTask(ctx, random, i, taskDuration, level)
                    }
                }, delay)
            }

            // 额外的后台线程负载（第一帧后）
            schedulePostFrameBackgroundTasks(context, level)

            isPostFrameLoadRunning.set(false)
        }
    }

    /**
     * 第一帧后的后台线程任务
     */
    private fun schedulePostFrameBackgroundTasks(context: Context, level: SelfLoadLevel) {
        val random = Random(SEED + 2000)

        val bgTaskCount = when (level) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 2
            SelfLoadLevel.MEDIUM -> 4
            SelfLoadLevel.HEAVY -> 8
        }

        repeat(bgTaskCount) { i ->
            val delay = 200L + (random.nextDouble() * 800).toLong()

            mainHandler.postDelayed({
                backgroundExecutor.submit {
                    currentContextRef?.get()?.let { ctx ->
                        runPostFrameBackgroundTask(ctx, i, level)
                    }
                }
            }, delay)
        }
    }

    /**
     * 执行第一帧后的主线程任务
     */
    private fun executePostFrameTask(
        context: Context,
        random: Random,
        taskId: Int,
        durationMs: Int,
        level: SelfLoadLevel
    ) {
        Trace.beginSection("PostFrameTask_$taskId")
        try {
            val startTime = System.nanoTime()
            val targetDurationNanos = durationMs * 1_000_000L

            // 根据任务类型执行不同操作
            when (random.nextInt(6)) {
                0 -> {
                    // 模拟延迟 Layout 加载
                    executeDelayedLayoutLoad(context, random, targetDurationNanos)
                }
                1 -> {
                    // 模拟 Bitmap 加载和处理
                    executeDelayedBitmapLoad(random, targetDurationNanos)
                }
                2 -> {
                    // Binder 调用 + 数据处理
                    executeBinderWithDataProcessing(context, random, targetDurationNanos)
                }
                3 -> {
                    // 模拟子线程传数据后的主线程更新
                    executeDataUpdateTask(random, targetDurationNanos)
                }
                4 -> {
                    // IO 读取后的数据处理
                    executeIoWithProcessing(context, random, targetDurationNanos)
                }
                else -> {
                    // 混合操作
                    executeMixedPostFrameTask(context, random, targetDurationNanos)
                }
            }

            // 确保任务执行时间达到目标
            val elapsed = System.nanoTime() - startTime
            val remaining = (targetDurationNanos - elapsed) / 1_000_000
            if (remaining > 0) {
                // 用计算填充剩余时间
                fillRemainingTime(random, remaining)
            }

        } finally {
            Trace.endSection()
        }
    }

    /**
     * 执行第一帧后的后台线程任务
     */
    private fun runPostFrameBackgroundTask(context: Context, taskId: Int, level: SelfLoadLevel) {
        val random = Random(SEED + 3000 + taskId)

        val iterations = when (level) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 10
            SelfLoadLevel.MEDIUM -> 25
            SelfLoadLevel.HEAVY -> 50
        }

        repeat(iterations) { i ->
            Trace.beginSection("PostFrameBg_${taskId}_$i")
            try {
                val taskDuration = getVariedTaskDuration(random, isLongTask = random.nextInt(100) < 25)

                when (random.nextInt(4)) {
                    0 -> {
                        // IO 操作
                        executeFileIO(context, 1, 512 + random.nextInt(1024))
                    }
                    1 -> {
                        // 数据处理
                        executeDataProcessingWithDuration(random, taskDuration)
                    }
                    2 -> {
                        // Binder
                        executeBinderCalls(context, 1 + random.nextInt(2))
                    }
                    else -> {
                        // CPU 计算
                        executeCpuTaskWithDuration(random, taskDuration)
                    }
                }

                // 随机间隔
                if (random.nextInt(100) < 60) {
                    Thread.sleep(random.nextInt(5).toLong())
                }
            } finally {
                Trace.endSection()
            }
        }
    }

    /**
     * 获取可变的任务时长
     * @param isLongTask true: 10-50ms, false: 1-10ms
     */
    private fun getVariedTaskDuration(random: Random, isLongTask: Boolean): Int {
        return if (isLongTask) {
            10 + random.nextInt(41) // 10-50ms
        } else {
            1 + random.nextInt(10) // 1-10ms
        }
    }

    /**
     * 执行可变时长的任务
     */
    private fun executeVariedDurationTask(
        context: Context,
        random: Random,
        taskId: Int,
        durationMs: Int,
        tag: String
    ) {
        Trace.beginSection("${tag}_$taskId")
        try {
            val startTime = System.nanoTime()
            val targetDurationNanos = durationMs * 1_000_000L

            when (random.nextInt(4)) {
                0 -> executeCpuTaskWithDuration(random, durationMs)
                1 -> executeBinderCalls(context, 1 + random.nextInt(3))
                2 -> executeMemoryTask(random, durationMs)
                3 -> executeStringTask(random, durationMs)
            }

            // 确保达到目标时长
            val elapsed = System.nanoTime() - startTime
            val remaining = (targetDurationNanos - elapsed) / 1_000_000
            if (remaining > 0) {
                fillRemainingTime(random, remaining)
            }
        } finally {
            Trace.endSection()
        }
    }

    /**
     * 延迟 Layout 加载（模拟懒加载的 View）
     */
    private fun executeDelayedLayoutLoad(context: Context, random: Random, targetNanos: Long) {
        val startTime = System.nanoTime()
        val inflater = LayoutInflater.from(context)

        val layoutIds = lightLayoutIds ?: return
        val count = 2 + random.nextInt(5)

        repeat(count) { i ->
            if (System.nanoTime() - startTime > targetNanos) return

            val layoutId = layoutIds[random.nextInt(layoutIds.size)]
            if (layoutId != 0) {
                try {
                    val view = inflater.inflate(layoutId, null)
                    view.measure(
                        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)
                    )
                    blackHole += view.measuredWidth.toDouble()
                    blackHoleInt = view.hashCode()
                } catch (e: Exception) { }
            }
        }
    }

    /**
     * 延迟 Bitmap 加载（模拟图片处理）
     */
    private fun executeDelayedBitmapLoad(random: Random, targetNanos: Long) {
        val startTime = System.nanoTime()

        // 模拟 Bitmap 解码和处理
        repeat(3) {
            if (System.nanoTime() - startTime > targetNanos) return

            // 模拟像素处理
            val width = 200 + random.nextInt(200)
            val height = 200 + random.nextInt(200)
            val pixels = IntArray(width * height)

            for (i in pixels.indices) {
                pixels[i] = random.nextInt()
            }

            // 模拟滤镜处理
            var sum = 0L
            for (i in pixels.indices) {
                val r = (pixels[i] shr 16) and 0xFF
                val g = (pixels[i] shr 8) and 0xFF
                val b = pixels[i] and 0xFF
                sum += (r + g + b)
            }

            blackHoleLong = sum
            keepAlive = pixels
        }

        keepAlive = null
    }

    /**
     * Binder 调用 + 数据处理
     */
    private fun executeBinderWithDataProcessing(context: Context, random: Random, targetNanos: Long) {
        val startTime = System.nanoTime()

        executeBinderCalls(context, 2)

        // 处理获取到的数据
        if (System.nanoTime() - startTime < targetNanos) {
            executeDataProcessingWithDuration(random, 10)
        }
    }

    /**
     * 数据更新任务
     */
    private fun executeDataUpdateTask(random: Random, targetNanos: Long) {
        val startTime = System.nanoTime()

        // 模拟解析 JSON 数据
        val dataList = ArrayList<Map<String, Any>>(100)
        repeat(100) { i ->
            if (System.nanoTime() - startTime > targetNanos) return

            val item = mapOf(
                "id" to i,
                "name" to "Item_$i",
                "value" to random.nextDouble() * 100,
                "tags" to listOf("tag1", "tag2", "tag3")
            )
            dataList.add(item)
        }

        // 模拟 diff 计算
        val sorted = dataList.sortedByDescending { it["value"] as Double }
        blackHole += sorted.size.toDouble()
        keepAlive = sorted
        keepAlive = null
    }

    /**
     * IO 读取后处理
     */
    private fun executeIoWithProcessing(context: Context, random: Random, targetNanos: Long) {
        val startTime = System.nanoTime()

        // 执行 IO
        executeFileIO(context, 1, 2048)

        // 处理读取的数据
        if (System.nanoTime() - startTime < targetNanos) {
            executeCpuTaskWithDuration(random, 5)
        }
    }

    /**
     * 混合的第一帧后任务
     */
    private fun executeMixedPostFrameTask(context: Context, random: Random, targetNanos: Long) {
        val startTime = System.nanoTime()

        // 交替执行不同类型的操作
        repeat(5) { i ->
            if (System.nanoTime() - startTime > targetNanos) return

            when (i % 4) {
                0 -> executeBinderCalls(context, 1)
                1 -> executeMemoryTask(random, 3)
                2 -> executeStringTask(random, 3)
                3 -> executeCpuTaskWithDuration(random, 3)
            }
        }
    }

    /**
     * 执行指定时长的 CPU 任务
     */
    private fun executeCpuTaskWithDuration(random: Random, durationMs: Int) {
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L
        var result = 0.0

        while (System.nanoTime() - startTime < targetNanos) {
            repeat(1000) { i ->
                result += sin(i * 0.01 + random.nextDouble()) * sqrt(i + 1.0)
            }
        }

        blackHole += result
    }

    /**
     * 执行指定时长的数据处理任务
     */
    private fun executeDataProcessingWithDuration(random: Random, durationMs: Int) {
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L

        val dataList = ArrayList<HashMap<String, Any>>()

        while (System.nanoTime() - startTime < targetNanos) {
            val item = HashMap<String, Any>()
            item["id"] = dataList.size
            item["value"] = random.nextDouble() * 100
            item["name"] = "Item_${dataList.size}"
            dataList.add(item)

            if (dataList.size > 1000) {
                dataList.sortByDescending { it["value"] as Double }
                blackHole += dataList.size.toDouble()
                dataList.clear()
            }
        }

        keepAlive = dataList
        blackHole += dataList.size.toDouble()
        keepAlive = null
    }

    /**
     * 内存任务
     */
    private fun executeMemoryTask(random: Random, durationMs: Int) {
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L

        while (System.nanoTime() - startTime < targetNanos) {
            val size = 1024 * (64 + random.nextInt(256))
            try {
                val array = ByteArray(size)
                array[0] = random.nextInt().toByte()
                array[size - 1] = random.nextInt().toByte()
                blackHoleInt = array[0] + array[size - 1]
            } catch (e: OutOfMemoryError) { }
        }
    }

    /**
     * 字符串任务
     */
    private fun executeStringTask(random: Random, durationMs: Int) {
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L

        while (System.nanoTime() - startTime < targetNanos) {
            val sb = StringBuilder()
            repeat(100) { i ->
                sb.append("Task_").append(i).append("_")
                    .append(random.nextInt(10000)).append("_")
            }
            val result = sb.toString().uppercase().replace("_", "-")
            blackHoleInt = result.length
        }
    }

    /**
     * 用计算填充剩余时间
     */
    private fun fillRemainingTime(random: Random, remainingMs: Long) {
        val startTime = System.nanoTime()
        val targetNanos = remainingMs * 1_000_000L
        var result = 0.0

        while (System.nanoTime() - startTime < targetNanos) {
            repeat(500) { i ->
                result += sin(i * 0.01 + random.nextDouble()) * cos(i * 0.02)
            }
        }

        blackHole += result
    }

    /**
     * 停止所有后台任务
     */
    fun stopBackgroundTasks() {
        isPostFrameLoadRunning.set(false)
        mainHandler.removeCallbacksAndMessages(null)
        currentContextRef = null
    }
}
