package com.example.launch.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.launch.common.BuildConfig
import com.example.launch.common.LifecycleLoadSimulator
import com.example.launch.common.LoadSimulator
import com.example.launch.common.PerformanceLogger
import java.util.Random

class MainActivity : ComponentActivity() {

    companion object {
        var isProcessCold = true
    }

    private lateinit var lifecycleSim: LifecycleLoadSimulator

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
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

        // Phase 2: Activity Init Load (Blocking) - Interleaved
        LoadSimulator.onActivityCreate(this, loadType)

        setContent {
            var status by remember { mutableStateOf("Initializing UI...") }
            var duration by remember { mutableStateOf(0L) }
            
            // Compose Specific Load: State Churn
            // Simulate heavy recomposition logic or state snapshot writes
            SimulateComposeLoad(loadType)

            // Phase 3: Async Load
            LaunchedEffect(Unit) {
                LoadSimulator.simulateAsyncNetworkLoad(this@MainActivity, loadType) { progress ->
                    status = progress
                }
                val end = System.currentTimeMillis()
                duration = end - startTime
                PerformanceLogger.log("Compose", loadType, startType, duration)
                status = "Finished in ${duration}ms"
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Launch Compose")
                Text(text = "Load: $loadType")
                Text(text = "Start: $startType")
                Text(text = "Status: $status")

                Button(onClick = {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) {
                    Text("Kill Process")
                }

                Button(onClick = {
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }) {
                    Text("Restart Activity")
                }
                
                // Render a list to verify UI performance
                if (loadType != LoadSimulator.LoadType.LIGHT) {
                    LazyColumn {
                        items(50) { index ->
                            Text("Item $index")
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::lifecycleSim.isInitialized) lifecycleSim.onStart(this)
    }

    override fun onResume() {
        super.onResume()
        if (::lifecycleSim.isInitialized) lifecycleSim.onResume(this)
    }
}

@Composable
fun SimulateComposeLoad(loadType: LoadSimulator.LoadType) {
    // Heavy Side Effect during Composition
    SideEffect {
        val iterations = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 100
            LoadSimulator.LoadType.MEDIUM -> 1000
            LoadSimulator.LoadType.HEAVY -> 5000
        }
        val r = Random(123)
        var sum = 0.0
        repeat(iterations) {
            sum += Math.sin(r.nextDouble())
        }
    }
}
