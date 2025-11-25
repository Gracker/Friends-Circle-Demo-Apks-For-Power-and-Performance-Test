package com.example.wechatfriendforcompose

import android.app.Application
import com.example.wechatfriendforcompose.data.ComposeDataCenter

/**
 * Application class for Compose performance test module
 */
class ComposeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 清除上一次运行可能残留的缓存数据
        ComposeDataCenter.clearCachedData()
    }
}


