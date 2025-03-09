package com.example.wechatfriendforperformance;

import android.app.Application;

/**
 * Application class for performance test module
 */
public class PerformanceApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize application
        PerformanceDataCenter.getInstance();
    }
} 