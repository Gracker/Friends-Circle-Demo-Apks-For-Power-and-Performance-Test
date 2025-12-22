package com.example.switch_common

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Trace
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 背景负载 Service
 *
 * 运行在独立进程 (:bg_load) 中，模拟真实 App 的后台任务
 *
 * 特点：
 * - 包含多种混合负载：CPU、IO、Binder、Memory、SQLite 等
 * - 任务之间有 0-2ms 的随机 sleep，模拟真实 App 的间歇性负载
 * - 支持自动停止：在 Switch 完成后 1 秒自动停止
 * - 任务时长可变：10-50ms 长任务，1-10ms 短任务
 * - 使用固定种子确保每次测试结果一致（伪随机）
 * - 防止编译器优化（volatile blackHole）
 */
class BackgroundLoadService : Service() {

    companion object {
        private const val TAG = "BackgroundLoadService"
        private const val EXTRA_TASK_COUNT = "task_count"
        private const val EXTRA_LOAD_LEVEL = "load_level"
        private const val EXTRA_AUTO_STOP_DELAY = "auto_stop_delay"
        private const val SEED = 987654321L

        // 默认自动停止延迟：1000ms（Switch 完成后 1 秒）
        private const val DEFAULT_AUTO_STOP_DELAY_MS = 1000L

        // 防止编译器优化
        @Volatile
        var blackHole: Double = 0.0

        @Volatile
        var blackHoleLong: Long = 0L

        @Volatile
        var blackHoleInt: Int = 0

        /**
         * 启动背景负载 Service
         *
         * @param context Context
         * @param loadLevel 负载级别
         * @param autoStopDelayMs 自动停止延迟（毫秒），默认 1000ms
         */
        fun start(context: Context, loadLevel: BackgroundLoadLevel, autoStopDelayMs: Long = DEFAULT_AUTO_STOP_DELAY_MS) {
            if (loadLevel == BackgroundLoadLevel.NONE) return

            val taskCount = when (loadLevel) {
                BackgroundLoadLevel.NONE -> 0
                BackgroundLoadLevel.MEDIUM -> 4
                BackgroundLoadLevel.HEAVY -> 6
            }

            val intent = Intent(context, BackgroundLoadService::class.java).apply {
                putExtra(EXTRA_TASK_COUNT, taskCount)
                putExtra(EXTRA_LOAD_LEVEL, loadLevel.ordinal)
                putExtra(EXTRA_AUTO_STOP_DELAY, autoStopDelayMs)
            }
            context.startService(intent)
        }

        /**
         * 停止背景负载 Service
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundLoadService::class.java))
        }

        /**
         * 通知 Switch 已完成，开始自动停止倒计时
         * 调用此方法后，Service 将在指定延迟后自动停止
         */
        fun notifySwitchComplete(context: Context) {
            val intent = Intent(context, BackgroundLoadService::class.java).apply {
                action = ACTION_SWITCH_COMPLETE
            }
            context.startService(intent)
        }

        private const val ACTION_SWITCH_COMPLETE = "com.example.switch_common.ACTION_SWITCH_COMPLETE"
    }

    private val executor = Executors.newCachedThreadPool()
    private val isRunning = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val startTimeMs = AtomicLong(0)
    private var autoStopDelayMs: Long = DEFAULT_AUTO_STOP_DELAY_MS
    private var autoStopScheduled = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 检查是否是 Switch 完成通知
        if (intent?.action == ACTION_SWITCH_COMPLETE) {
            scheduleAutoStop()
            return START_NOT_STICKY
        }

        val taskCount = intent?.getIntExtra(EXTRA_TASK_COUNT, 4) ?: 4
        val loadLevel = BackgroundLoadLevel.entries.getOrElse(
            intent?.getIntExtra(EXTRA_LOAD_LEVEL, 1) ?: 1
        ) { BackgroundLoadLevel.MEDIUM }
        autoStopDelayMs = intent?.getLongExtra(EXTRA_AUTO_STOP_DELAY, DEFAULT_AUTO_STOP_DELAY_MS)
            ?: DEFAULT_AUTO_STOP_DELAY_MS

        Log.d(TAG, "Starting background load service with $taskCount tasks, level: $loadLevel, autoStop: ${autoStopDelayMs}ms")

        startTimeMs.set(System.currentTimeMillis())

        if (isRunning.compareAndSet(false, true)) {
            startBackgroundTasks(taskCount, loadLevel)
        }

        return START_NOT_STICKY
    }

    /**
     * 调度自动停止
     */
    private fun scheduleAutoStop() {
        if (!autoStopScheduled.compareAndSet(false, true)) return

        Log.d(TAG, "Switch complete, scheduling auto-stop in ${autoStopDelayMs}ms")

        mainHandler.postDelayed({
            Log.d(TAG, "Auto-stopping background load service")
            stopSelf()
        }, autoStopDelayMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        mainHandler.removeCallbacksAndMessages(null)
        executor.shutdown()
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            executor.shutdownNow()
        }
        val duration = System.currentTimeMillis() - startTimeMs.get()
        Log.d(TAG, "Background load service stopped after ${duration}ms")
    }

    private fun startBackgroundTasks(taskCount: Int, loadLevel: BackgroundLoadLevel) {
        repeat(taskCount) { taskId ->
            executor.submit {
                runMixedLoadTask(taskId, loadLevel)
            }
        }
    }

    /**
     * 混合负载任务
     * 模拟真实 App 的各种后台操作，不是单一类型的负载
     *
     * 任务时长可变：
     * - 长任务（30%概率）：10-50ms
     * - 短任务（70%概率）：1-10ms
     */
    private fun runMixedLoadTask(taskId: Int, loadLevel: BackgroundLoadLevel) {
        val random = Random(SEED + taskId)

        // 根据负载级别决定迭代次数
        val iterations = when (loadLevel) {
            BackgroundLoadLevel.NONE -> 0
            BackgroundLoadLevel.MEDIUM -> 50
            BackgroundLoadLevel.HEAVY -> 100
        }

        repeat(iterations) { iteration ->
            if (!isRunning.get()) return

            Trace.beginSection("BgService_Task${taskId}_$iteration")
            try {
                // 决定这是长任务还是短任务
                val isLongTask = random.nextInt(100) < 30 // 30% 概率是长任务
                val targetDurationMs = getVariedTaskDuration(random, isLongTask)

                // 随机选择任务类型，模拟真实 App 的混合负载
                when (random.nextInt(20)) {
                    // CPU 密集型任务 (25%)
                    in 0..4 -> runCpuLoadWithDuration(random, targetDurationMs)

                    // Crypto 任务 (10%)
                    in 5..6 -> runCryptoLoadWithDuration(random, targetDurationMs)

                    // Binder IPC 任务 (15%)
                    in 7..9 -> runBinderLoad(random.nextInt(3) + 1)

                    // Memory 任务 (15%)
                    in 10..12 -> runMemoryLoadWithDuration(random, targetDurationMs)

                    // IO 任务 (15%)
                    in 13..15 -> runIoLoad(256 + random.nextInt(512))

                    // SQLite 任务 (10%)
                    in 16..17 -> runSqliteLoad()

                    // 字符串处理 (10%)
                    else -> runStringProcessingWithDuration(random, targetDurationMs)
                }
            } finally {
                Trace.endSection()
            }

            // 关键：任务之间随机 sleep 0-2ms，模拟真实 App 的间歇性负载
            // 真实 App 不会一直满负荷运行，会有各种等待和间隙
            if (random.nextInt(100) < 70) { // 70% 概率 sleep
                try {
                    Thread.sleep(random.nextInt(3).toLong()) // 0-2ms
                } catch (e: InterruptedException) {
                    return
                }
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
     * 执行指定时长的 CPU 任务
     */
    private fun runCpuLoadWithDuration(random: Random, durationMs: Int) {
        Trace.beginSection("Bg_CPU_${durationMs}ms")
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L
        var result = 0.0

        while (System.nanoTime() - startTime < targetNanos) {
            repeat(1000) { i ->
                val a = random.nextDouble()
                val b = random.nextDouble()
                result += sin(a + i * 0.01) * sqrt(b + 1.0)
            }
        }

        blackHole += result
        Trace.endSection()
    }

    private fun runCpuLoad(random: Random, iterations: Int) {
        Trace.beginSection("Bg_CPU")
        var result = 0.0
        for (i in 0 until iterations) {
            val a = random.nextDouble()
            val b = random.nextDouble()
            result += sin(a) * sqrt(b)
        }
        blackHole += result
        Trace.endSection()
    }

    private fun runCryptoLoad(random: Random) {
        Trace.beginSection("Bg_Crypto")
        try {
            val data = ByteArray(4096)
            random.nextBytes(data)
            val digest = MessageDigest.getInstance("SHA-256")
            repeat(20) {
                digest.reset()
                val hash = digest.digest(data)
                blackHole += hash[0].toDouble()
                data[0] = hash[0]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Crypto load failed", e)
        }
        Trace.endSection()
    }

    /**
     * 执行指定时长的 Crypto 任务
     */
    private fun runCryptoLoadWithDuration(random: Random, durationMs: Int) {
        Trace.beginSection("Bg_Crypto_${durationMs}ms")
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L

        try {
            val data = ByteArray(4096)
            random.nextBytes(data)
            val digest = MessageDigest.getInstance("SHA-256")

            while (System.nanoTime() - startTime < targetNanos) {
                digest.reset()
                val hash = digest.digest(data)
                blackHole += hash[0].toDouble()
                blackHoleInt = hash[1].toInt()
                data[0] = hash[0]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Crypto load failed", e)
        }
        Trace.endSection()
    }

    /**
     * 执行指定时长的 Memory 任务
     */
    private fun runMemoryLoadWithDuration(random: Random, durationMs: Int) {
        Trace.beginSection("Bg_Memory_${durationMs}ms")
        val startTime = System.nanoTime()
        val targetNanos = durationMs * 1_000_000L

        while (System.nanoTime() - startTime < targetNanos) {
            try {
                val size = 1024 * (64 + random.nextInt(256)) // 64KB - 320KB
                val array = ByteArray(size)
                array[0] = random.nextInt().toByte()
                array[size / 2] = random.nextInt().toByte()
                array[size - 1] = random.nextInt().toByte()
                blackHoleInt = array[0] + array[size / 2] + array[size - 1]
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "Memory load OOM")
                break
            }
        }
        Trace.endSection()
    }

    /**
     * 执行指定时长的字符串处理任务
     */
    private fun runStringProcessingWithDuration(random: Random, durationMs: Int) {
        Trace.beginSection("Bg_String_${durationMs}ms")
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
            blackHoleLong = result.hashCode().toLong()
        }
        Trace.endSection()
    }

    private fun runBinderLoad(calls: Int) {
        Trace.beginSection("Bg_Binder")
        val pm = packageManager
        val packageName = packageName
        repeat(calls) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        Trace.endSection()
    }

    private fun runMemoryLoad(mb: Int) {
        Trace.beginSection("Bg_Memory")
        try {
            val size = 1024 * 1024 * mb
            val array = ByteArray(size)
            array[0] = 1
            array[size / 2] = 2
            array[size - 1] = 3
            blackHole += array[0] + array[size / 2] + array[size - 1].toDouble()
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Memory load OOM")
        }
        Trace.endSection()
    }

    private fun runIoLoad(sizeBytes: Int) {
        Trace.beginSection("Bg_IO")
        val file = File(cacheDir, "bg_load_${Thread.currentThread().id}.tmp")
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
        Trace.endSection()
    }

    private fun runSqliteLoad() {
        Trace.beginSection("Bg_SQLite")
        val dbName = "bg_load_${Thread.currentThread().id}.db"
        var db: android.database.sqlite.SQLiteDatabase? = null
        try {
            db = openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS temp_data (id INTEGER PRIMARY KEY, value TEXT)")
            db.execSQL("INSERT OR REPLACE INTO temp_data (id, value) VALUES (1, 'test_value_${System.currentTimeMillis()}')")
            val cursor = db.rawQuery("SELECT * FROM temp_data WHERE id = 1", null)
            while (cursor.moveToNext()) {
                blackHole += cursor.getString(1).length.toDouble()
            }
            cursor.close()
        } catch (e: Exception) {
            Log.w(TAG, "SQLite load failed", e)
        } finally {
            db?.close()
        }
        Trace.endSection()
    }

    private fun runStringProcessing(random: Random, count: Int) {
        Trace.beginSection("Bg_String")
        val sb = StringBuilder()
        repeat(count) { i ->
            sb.append("Task_").append(i).append("_")
                .append(random.nextInt(1000)).append("_")
        }
        val result = sb.toString().uppercase().replace("_", "-")
        blackHole += result.length.toDouble()
        Trace.endSection()
    }
}
