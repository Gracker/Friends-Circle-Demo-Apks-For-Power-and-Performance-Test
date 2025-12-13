package com.example.launch.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
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

    @Volatile
    var blackHole: Double = 0.0

    // Fixed seed for deterministic behavior
    private const val SEED = 123456789L

    enum class LoadType {
        LIGHT, MEDIUM, HEAVY
    }

    // --- Phase 1: Application Init (Blocking) ---
    fun onApplicationCreate(context: Context, type: LoadType) {
        // Simulates SDK init, heavy reflection, etc.
        // Interleaved: CPU -> Binder -> IO -> CPU
        runInterleavedLoad(context, type, phase = "AppInit")
    }

    // --- Phase 2: Activity Init (Blocking) ---
    fun onActivityCreate(context: Context, type: LoadType) {
        // Simulates View inflation, layout calc, initial data unmarshalling
        val random = Random(SEED + 1)
        
        // 1. View Inflation Simulation (Reflection/Recursion)
        val viewDepth = when(type) {
            LoadType.LIGHT -> 10
            LoadType.MEDIUM -> 50
            LoadType.HEAVY -> 200
        }
        simulateViewInflation(viewDepth)

        // 2. Interleaved Logic (Memory/CPU mixed)
        runInterleavedLoad(context, type, phase = "ActivityInit")
    }

    // --- Phase 3: Async Network / Data Load (Suspend) ---
    suspend fun simulateAsyncNetworkLoad(context: Context, type: LoadType, onProgress: (String) -> Unit) {
        val random = Random(SEED + 2)
        
        // 1. DNS / Connection Setup (Latency)
        onProgress("Connecting to server...")
        val baseLatency = when(type) {
            LoadType.LIGHT -> 50L
            LoadType.MEDIUM -> 200L
            LoadType.HEAVY -> 800L
        }
        val jitter = (baseLatency * 0.2 * (random.nextDouble() - 0.5)).toLong()
        delay(baseLatency + jitter)

        // 2. Request Processing / Auth
        onProgress("Authenticating...")
        delay(if (type == LoadType.HEAVY) 300L else 50L)

        // 3. Data Fetching (Throughput simulation)
        onProgress("Downloading content...")
        val chunks = if (type == LoadType.HEAVY) 5 else 2
        for (i in 1..chunks) {
            val chunkLatency = if (type == LoadType.HEAVY) 200L else 50L
            delay(chunkLatency)
            
            // Interleaved processing of chunk
            // Run a mini-interleaved load here to simulate parsing JSON/Protobuf on bg thread
            runCpuLoad(random, 50_000)
            
            onProgress("Processing batch $i/$chunks...")
        }

        // 4. Heavy Asset Extraction (Only Heavy)
        if (type == LoadType.HEAVY) {
            onProgress("Unpacking assets...")
            runAssetExtraction(context, "large_video.mp4")
        }

        onProgress("Ready")
    }

    // --- Core: Interleaved Load Logic ---
    private fun runInterleavedLoad(context: Context, type: LoadType, phase: String) {
        val random = Random(SEED + phase.hashCode())
        
        // Determine number of "Context Switches"
        val switchCount = when(type) {
            LoadType.LIGHT -> 2
            LoadType.MEDIUM -> 10
            LoadType.HEAVY -> 30
        }

        repeat(switchCount) {
            // Task 1: CPU (Short burst)
            runCpuLoad(random, 50_000)

            // Task 2: Small IO (Config read)
            if (it % 5 == 0) { // Every 5th switch
                 runIoLoad(context, 1024) // 1KB
            }

            // Task 3: Binder (System Service check)
            if (it % 3 == 0) {
                 runBinderLoad(context, 1)
            }

            // Task 4: Memory (Small alloc)
            if (it % 10 == 0) {
                 runMemoryLoad(1) // 1MB
            }
        }
    }

    // --- Internal Helpers ---

    fun readSharedPreferences(context: Context, items: Int) {
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
    }

    fun simulateBitmapDecode(context: Context, scale: Int) {
        try {
            context.assets.open("large_image.png").use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            runCpuLoad(Random(SEED), 100_000 * scale)
        }
    }

    fun simulateXmlParse(iterations: Int) {
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
    }

    fun simulateViewInflation(depth: Int) {
        // Recursion + Reflection to simulate View Creation
        recursiveViewBuilder(depth, 0)
    }

    private fun recursiveViewBuilder(maxDepth: Int, currentDepth: Int) {
        if (currentDepth >= maxDepth) return
        
        // Reflection overhead
        try {
            val clazz = Class.forName("java.lang.String") // Cheap reflection
            clazz.methods // Trigger method lookup
        } catch (e: Exception) {}

        // Branching (simulate ViewGroup having children)
        repeat(2) {
             recursiveViewBuilder(maxDepth, currentDepth + 1)
        }
    }

    private fun runCpuLoad(random: Random, iterations: Int) {
        var result = 0.0
        for (i in 0 until iterations) {
            val a = random.nextDouble()
            val b = random.nextDouble()
            result += sin(a) * sqrt(b)
        }
        blackHole += result
    }

    private fun runIoLoad(context: Context, sizeBytes: Int) {
        val file = File(context.cacheDir, "load_sim.tmp")
        try {
            val data = ByteArray(sizeBytes)
            Random(SEED).nextBytes(data) 
            FileOutputStream(file).use { it.write(data) }
            file.inputStream().use { it.readBytes() }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            file.delete()
        }
    }

    private fun runBinderLoad(context: Context, calls: Int) {
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
    }

    private fun runMemoryLoad(mb: Int) {
        val list = ArrayList<ByteArray>()
        try {
            for (i in 0 until mb) {
                val array = ByteArray(1024 * 1024) 
                array[0] = 1 
                array[array.lastIndex] = 1
                list.add(array)
            }
            blackHole += list.size.toDouble()
        } catch (e: OutOfMemoryError) {}
    }

    private fun runAssetExtraction(context: Context, fileName: String) {
        try {
            val file = File(context.cacheDir, "extracted_$fileName")
            if (file.exists()) file.delete() 
            
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) { } 
    }
}