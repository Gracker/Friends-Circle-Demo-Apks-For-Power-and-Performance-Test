package com.example.launch.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Trace
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.sin
import kotlin.math.sqrt

object LoadSimulator {
    
    private const val TAG = "LoadSimulator"

    @Volatile
    var blackHole: Double = 0.0
    
    // Anti-optimization sink
    @Volatile
    var keepAlive: Any? = null

    // Fixed seed for deterministic behavior
    private const val SEED = 123456789L

    // Thread pool for background tasks
    private val backgroundExecutor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    enum class LoadType {
        LIGHT, MEDIUM, HEAVY
    }

    // --- Phase 1: Application Init (Blocking) ---
    fun onApplicationCreate(context: Context, type: LoadType) {
        Trace.beginSection("LoadSimulator_AppInit")
        try {
            // Start background noise immediately to simulate content providers/receivers/other processes
            startBackgroundNoise(context, type)
            
            // Simulates SDK init, heavy reflection, etc.
            // Chaos Mode: Random tasks + Random Sleep
            runChaosLoop(context, type, phase = "AppInit")
        } finally {
            Trace.endSection()
        }
    }

    // --- Phase 2: Activity Init (Blocking) ---
    fun onActivityCreate(context: Context, type: LoadType) {
        Trace.beginSection("LoadSimulator_ActivityInit")
        try {
            // Reinforce background noise for this phase
            // Real apps trigger new async fetches/decoding when Activity starts
            startBackgroundNoise(context, type)

            // 1. View Inflation Simulation (Reflection loop) - kept as a block as it's usually a block
            val viewCount = when(type) {
                LoadType.LIGHT -> 1000
                LoadType.MEDIUM -> 3000
                LoadType.HEAVY -> 6000
            }
            simulateViewInflation(viewCount)

            // 2. Chaos Loop (Memory/CPU/Binder mixed with sleeps)
            runChaosLoop(context, type, phase = "ActivityInit")
            
            // 3. Inject Scattered Binder/IO calls on Main Thread (Future posts)
            injectScatteredWork(context, type)
        } finally {
            Trace.endSection()
        }
    }

    // --- Phase 3: Async Network / Data Load (Suspend) ---
    suspend fun simulateAsyncNetworkLoad(context: Context, type: LoadType, onProgress: (String) -> Unit) {
        val random = Random(SEED + 2)
        
        onProgress("Connecting to server...")
        val baseLatency = when(type) {
            LoadType.LIGHT -> 100L
            LoadType.MEDIUM -> 300L
            LoadType.HEAVY -> 600L
        }
        delay(baseLatency)

        onProgress("Authenticating...")
        delay(50L)

        onProgress("Downloading content...")
        // Simulate "stuttery" download processing
        val chunks = if (type == LoadType.HEAVY) 8 else 3
        for (i in 1..chunks) {
            val chunkLatency = if (type == LoadType.HEAVY) 150L else 40L
            delay(chunkLatency)
            
            // Process chunk on the calling thread (likely main or suspect) with chaos
            runCpuLoad(random, 10_000)
            if (random.nextBoolean()) {
                try { Thread.sleep(random.nextInt(5).toLong() + 1) } catch(e:Exception){}
            }
            
            onProgress("Processing batch $i/$chunks...")
        }

        if (type == LoadType.HEAVY) {
            onProgress("Unpacking assets...")
            runAssetExtraction(context, "large_video.mp4")
        }

        onProgress("Ready")
    }

    // --- Core: Chaos Loop Logic (The User's Request) ---
    private fun runChaosLoop(context: Context, type: LoadType, phase: String) {
        val random = Random(SEED + phase.hashCode())
        
        val iterations = when(type) {
            LoadType.LIGHT -> 20   
            LoadType.MEDIUM -> 100 
            LoadType.HEAVY -> 250  
        }
        
        repeat(iterations) {
            Trace.beginSection("ChaosTask")
            
            // 1. Execute a Random Task (Mixed sizes)
            val taskType = random.nextInt(20) 
            
            // Logging noise (Anti-pattern)
            if (taskType == 0) Log.v(TAG, "Chaos task running")

            when {
                // Heavy CPU Tasks (10ms+)
                taskType < 3 -> { 
                    // ~10ms of Math
                    runCpuLoad(random, 300_000) 
                }
                
                // Heavy Crypto
                taskType < 5 -> {
                    // ~5-8ms of Crypto
                    repeat(5) { runCryptoLoad(random) }
                }

                // Binder (IPC) - INCREASED PROBABILITY for realism
                taskType < 10 -> runBinderLoad(context, random.nextInt(2) + 1)

                // Standard Mix
                taskType < 13 -> runMemoryLoad(random.nextInt(3) + 1)
                taskType < 16 -> runIoLoad(context, 128)
                else -> runSqliteLoad(context)
            }
            Trace.endSection()

            // 2. Realistic Gaps (Sleep/Lock contention)
            // Increased back to 50% chance, 1-3ms duration to break up the "Running" wall
            if (random.nextBoolean()) {
                 try { 
                     val sleepTime = random.nextInt(3) + 1L 
                     Thread.sleep(sleepTime) 
                 } catch (e: Exception) { }
            }
        }
    }
    
    // --- Background Noise Generation ---
    private fun startBackgroundNoise(context: Context, type: LoadType) {
        val threadCount = when(type) {
            LoadType.LIGHT -> 2
            LoadType.MEDIUM -> 4
            LoadType.HEAVY -> 8
        }
        
        repeat(threadCount) { threadId ->
            backgroundExecutor.submit {
                val random = Random(SEED + threadId)
                val iterations = if (type == LoadType.HEAVY) 100 else 30
                
                repeat(iterations) {
                    Trace.beginSection("BgTask_$threadId")
                    when(random.nextInt(4)) {
                        0 -> runCpuLoad(random, 20_000)
                        1 -> runIoLoad(context, 512)
                        2 -> runMemoryLoad(1)
                        3 -> runBinderLoad(context, 1)
                    }
                    Trace.endSection()
                    try { Thread.sleep((random.nextInt(10) + 1).toLong()) } catch (e: Exception) {}
                }
            }
        }
    }
    
    // --- Scattered Main Thread Work (Post-Layout) ---
    private fun injectScatteredWork(context: Context, type: LoadType) {
        val taskCount = when(type) {
            LoadType.LIGHT -> 5
            LoadType.MEDIUM -> 15
            LoadType.HEAVY -> 30
        }
        
        val random = Random(SEED + 999)
        
        repeat(taskCount) { i ->
            val delay = (random.nextDouble() * 2000).toLong() 
            val isBinder = random.nextBoolean()
            val cpuLoadIterations = 10_000
            
            mainHandler.postDelayed({
                Trace.beginSection("ScatteredTask_$i")
                try {
                    if (isBinder) {
                         runBinderLoad(context, 1)
                    } else {
                         runCpuLoad(Random(i.toLong()), cpuLoadIterations)
                    }
                    if (type == LoadType.HEAVY) {
                        try { Thread.sleep(2) } catch(e:Exception){}
                    }
                } finally {
                    Trace.endSection()
                }
            }, delay)
        }
    }

    // --- New Phase: Message Queue / Looper Congestion ---
    fun injectMessageQueueBlockers(context: Context, type: LoadType) {
        val random = Random(SEED + 777)
        // Multiple parallel chains
        val chainCount = if (type == LoadType.HEAVY) 4 else 1
        
        // Self-replicating task runnable (Long-running background processing simulation)
        class ChainTask(var counter: Int, val chainId: Int) : Runnable {
            // Adjusted chain depth for ~1.5 - 2s duration
            // Avg task+delay ~15-20ms. 100 * 18ms = 1800ms
            val maxChain = if (type == LoadType.HEAVY) 100 else 20
            
            override fun run() {
                Trace.beginSection("MQ_Chain_${chainId}_Step")
                try {
                    // 1. Do Work (Mixed Load, not just CPU)
                    val workType = random.nextInt(10)
                    when {
                        // CPU Burst
                        workType < 3 -> runCpuLoad(random, 100_000) 
                        // Binder Block (IPC overhead)
                        workType < 5 -> runBinderLoad(context, 1)
                        // IO Check
                        workType < 7 -> runIoLoad(context, 64)
                        // Lock Contention / Sleep
                        else -> {
                            try { Thread.sleep(random.nextInt(3) + 1L) } catch(e:Exception){}
                        }
                    }
                    
                    // 2. Re-post self (Chain Reaction) with DELAY
                    // Using postDelayed creates "breathing room" for UI but keeps load persistent over seconds
                    if (counter < maxChain) {
                        counter++
                        val delay = 5 + random.nextInt(15) // 5-20ms delay
                        mainHandler.postDelayed(this, delay.toLong())
                    }
                } finally {
                    Trace.endSection()
                }
            }
        }
        
        // Ignite the chains (Main Thread)
        repeat(chainCount) { id ->
             mainHandler.post(ChainTask(0, id))
        }
        
        // --- Concurrently, start background chains (simulating parallel workers) ---
        class BackgroundChainTask(var counter: Int, val chainId: Int, private val bgRandom: Random) : Runnable {
            val maxChain = if (type == LoadType.HEAVY) 100 else 20 // Same depth as main thread chain
            
            override fun run() {
                Trace.beginSection("BG_Chain_${chainId}_Step")
                try {
                    val workType = bgRandom.nextInt(10)
                    when {
                        workType < 4 -> runCpuLoad(bgRandom, 150_000) // Heavier CPU
                        workType < 7 -> runIoLoad(context, 256)
                        else -> runMemoryLoad(bgRandom.nextInt(2) + 1)
                    }
                    
                    if (counter < maxChain) {
                        counter++
                        // No artificial delay for background tasks, they just run
                        backgroundExecutor.submit(this)
                    }
                } finally {
                    Trace.endSection()
                }
            }
        }

        // Ignite background chains
        repeat(chainCount) { id ->
            backgroundExecutor.submit(BackgroundChainTask(0, id, Random(SEED + 888 + id)))
        }
        
        // Urgent blockers (Front of queue)
        repeat(3) {
             mainHandler.postAtFrontOfQueue {
                 Trace.beginSection("MQ_Urgent_Blocker")
                 runCpuLoad(random, 20_000)
                 Trace.endSection()
             }
        }
        
        // IdleHandler
        Looper.myQueue().addIdleHandler {
            Trace.beginSection("IdleHandler_Work")
            runCpuLoad(random, if (type == LoadType.HEAVY) 50_000 else 10_000)
            if (type == LoadType.HEAVY) runMemoryLoad(2)
            Trace.endSection()
            false 
        }
    }

    // --- New Phase: Post-Frame / Final Freeze ---
    fun simulateFinalFreeze(type: LoadType) {
        Trace.beginSection("Final_UI_Freeze")
        try {
            val iterations = when(type) {
                LoadType.LIGHT -> 500
                LoadType.MEDIUM -> 2000
                LoadType.HEAVY -> 5000
            }

            val complexList = ArrayList<Any>(iterations)
            
            for (i in 0 until iterations) {
                val map = HashMap<String, String>()
                map["id"] = i.toString()
                map["title"] = "Item Title $i"
                map["desc"] = "This is a description for item $i."
                map["timestamp"] = System.currentTimeMillis().toString()
                
                val subList = ArrayList<Int>()
                repeat(5) { subList.add(it * i) }
                
                complexList.add(Pair(map, subList))
                
                blackHole += map.hashCode().toDouble() + subList.hashCode()
            }
            
            var index = 0
            for (item in complexList) {
                blackHole += item.hashCode().toDouble()
                if (index % 200 == 0) {
                     Log.d(TAG, "Processed item $index")
                }
                index++
            }
            
            keepAlive = complexList
            complexList.clear()
            keepAlive = null
            
        } finally {
            Trace.endSection()
        }
    }

    // --- Internal Helpers ---

    fun readSharedPreferences(context: Context, items: Int) {
        Trace.beginSection("readSP")
        try {
            val prefs = context.getSharedPreferences("startup_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            for (i in 0 until items) {
                editor.putString("key_$i", "value_$i")
            }
            editor.apply() 
            for (i in 0 until items) {
                val v = prefs.getString("key_$i", null)
                if (v != null) blackHole += v.length.toDouble()
            }
        } finally {
            Trace.endSection()
        }
    }

    fun simulateBitmapDecode(context: Context, scale: Int) {
        Trace.beginSection("BitmapDecode")
        try {
            try {
                context.assets.open("large_image.png").use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                runCpuLoad(Random(SEED), 50_000 * scale)
            }
        } finally {
            Trace.endSection()
        }
    }

    fun simulateXmlParse(iterations: Int) {
        Trace.beginSection("XmlParse")
        try {
            val sb = StringBuilder()
            sb.append("<root>")
            repeat(iterations) {
                sb.append("<item attr=\"val\">Content $it</item>")
            }
            sb.append("</root>")
            val xml = sb.toString()
            var idx = 0
            while (idx < xml.length) {
                val nextTag = xml.indexOf("<", idx)
                if (nextTag == -1) break
                idx = nextTag + 1
                blackHole += idx.toDouble()
            }
        } finally {
            Trace.endSection()
        }
    }

    fun simulateViewInflation(count: Int) {
        Trace.beginSection("SimulateInflation")
        try {
            val clazz = try { Class.forName("java.lang.String") } catch (e: Exception) { return }
            for(i in 0 until count) {
                try { clazz.methods } catch (e: Exception) {}
            }
        } finally {
            Trace.endSection()
        }
    }

    fun runCpuLoad(random: Random, iterations: Int) {
        var result = 0.0
        for (i in 0 until iterations) {
            val a = random.nextDouble()
            val b = random.nextDouble()
            result += sin(a) * sqrt(b)
        }
        blackHole += result
    }

    fun runIoLoad(context: Context, sizeBytes: Int) {
        Trace.beginSection("IOLoad")
        val file = File(context.cacheDir, "load_sim_deterministic.tmp")
        try {
            val data = ByteArray(sizeBytes)
            data[0] = 1
            data[data.lastIndex] = 2
            FileOutputStream(file).use { it.write(data) }
            file.inputStream().use { it.readBytes() }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            file.delete()
            Trace.endSection()
        }
    }

    fun runBinderLoad(context: Context, calls: Int) {
        Trace.beginSection("BinderLoad")
        val pm = context.packageManager
        val packageName = context.packageName
        for (i in 0 until calls) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                     pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                     @Suppress("DEPRECATION")
                     pm.getPackageInfo(packageName, 0)
                }
            } catch (e: Exception) {}
        }
        Trace.endSection()
    }

    fun runMemoryLoad(mb: Int) {
        Trace.beginSection("MemAlloc")
        try {
            val size = 1024 * 1024 * mb
            val array = ByteArray(size)
            array[0] = 1
            blackHole += array[0].toDouble()
        } catch (e: OutOfMemoryError) {}
        Trace.endSection()
    }

    fun runCryptoLoad(random: Random) {
        Trace.beginSection("CryptoLoad")
        try {
            val data = ByteArray(4096)
            random.nextBytes(data)
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            repeat(50) {
                digest.reset()
                val hash = digest.digest(data)
                blackHole += hash[0].toDouble()
                data[0] = hash[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            Trace.endSection()
        }
    }

    fun runSqliteLoad(context: Context) {
        Trace.beginSection("SqliteLoad")
        val dbName = "load_sim.db"
        var db: android.database.sqlite.SQLiteDatabase? = null
        try {
            db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS temp_load (id INTEGER PRIMARY KEY, content TEXT)")
            db.execSQL("INSERT INTO temp_load (content) VALUES ('Simulated DB Content')")
            val cursor = db.rawQuery("SELECT * FROM temp_load LIMIT 1", null)
            while (cursor.moveToNext()) {
                val value = cursor.getString(1)
                blackHole += value.length.toDouble()
            }
            cursor.close()
        } catch (e: Exception) {
        } finally {
            db?.close()
            Trace.endSection()
        }
    }

    private fun runAssetExtraction(context: Context, fileName: String) {
        Trace.beginSection("AssetExtract")
        try {
            val file = File(context.cacheDir, "extracted_$fileName")
            if (file.exists()) file.delete() 
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) { } finally {
            Trace.endSection()
        }
    }
}