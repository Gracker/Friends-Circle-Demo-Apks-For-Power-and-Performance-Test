package com.example.wechatfriendforperformance;

import android.app.Application;

/**
 * 性能测试专用的Application类
 */
public class PerformanceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化应用
        PerformanceDataCenter.init();
    }
} 