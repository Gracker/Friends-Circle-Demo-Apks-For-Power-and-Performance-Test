package com.example.wechatfriendforcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.wechatfriendforcompose.ui.navigation.ComposeNavigation
import com.example.wechatfriendforcompose.ui.theme.ComposeTheme

/**
 * Compose版朋友圈性能测试主Activity
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 检查是否通过Intent直接启动特定负载
        val activityType = intent?.getStringExtra("activity_type")
        
        setContent {
            ComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    ComposeNavigation(
                        navController = navController,
                        startDestination = activityType
                    )
                }
            }
        }
    }
}


