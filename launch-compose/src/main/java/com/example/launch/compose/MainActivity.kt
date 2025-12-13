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
    // Heavy Side Effect during Composition
    // Simulate Complex UI Logic:
    // 1. Large Data List creation
    // 2. Sorting / Filtering (ViewModel logic)
    // 3. State Snapshot writes
    
    SideEffect {
        val itemCount = when(loadType) {
            LoadSimulator.LoadType.LIGHT -> 100
            LoadSimulator.LoadType.MEDIUM -> 2000
            LoadSimulator.LoadType.HEAVY -> 10000
        }

        // 1. Data Generation
        val items = ArrayList<UiModel>(itemCount)
        val r = Random(123)
        for(i in 0 until itemCount) {
            items.add(UiModel(i, "Item $i", r.nextDouble(), if (r.nextBoolean()) "A" else "B"))
        }

        // 2. Logic (Sort & Filter)
        val filtered = items.filter { it.category == "A" }
            .sortedByDescending { it.score }
            .map { it.copy(name = it.name.uppercase()) }

        // 3. State Churn (Simulate writing to many MutableStates)
        var globalStateHash = 0
        filtered.forEach { 
             globalStateHash += it.id 
        }
    }
}

data class UiModel(
    val id: Int,
    val name: String,
    val score: Double,
    val category: String
)

