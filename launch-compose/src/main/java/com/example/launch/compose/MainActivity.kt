package com.example.launch.compose

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
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

class MainActivity : AppCompatActivity() {

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

        // Phase 1.5: Inject Message Queue Blockers
        LoadSimulator.injectMessageQueueBlockers(this, loadType)

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
                
                // Phase 4: Final UI Freeze (Jank)
                LoadSimulator.simulateFinalFreeze(loadType)
                
                val end = System.currentTimeMillis()
                duration = end - startTime
                PerformanceLogger.log("Compose", loadType, startType, duration)
                status = "Finished\n${duration}ms"
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Package: ${applicationContext.packageName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Type: Compose",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Load: $loadType",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start: $startType",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = status,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (status.startsWith("Finished")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground
                            ),
                            fontSize = 32.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        // Buttons removed
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
    // Compose Specific Load:
    // 1. Emit a large number of nodes to stress the SlotTable and Layout system.
    // 2. Use deep modifier chains.
    val count = when(loadType) {
        LoadSimulator.LoadType.LIGHT -> 50
        LoadSimulator.LoadType.MEDIUM -> 200
        LoadSimulator.LoadType.HEAVY -> 1000
    }

    // Hidden container that still participates in composition and layout
    Box(modifier = Modifier.height(0.dp).fillMaxWidth()) {
        Column {
            repeat(count) { i ->
                // Simulate a complex node with modifiers
                Box(
                    modifier = Modifier
                        .padding(1.dp)
                        .fillMaxWidth()
                        .padding(2.dp)
                ) {
                    Text(text = "Hidden Item $i for load simulation")
                }
            }
        }
    }

    // Also keep the SideEffect for pure state logic simulation
    SideEffect {
        // Simple logic churn
        val list = ArrayList<String>()
        repeat(count) { list.add("Item $it") }
        list.sort()
    }
}

data class UiModel(
    val id: Int,
    val name: String,
    val score: Double,
    val category: String
)

