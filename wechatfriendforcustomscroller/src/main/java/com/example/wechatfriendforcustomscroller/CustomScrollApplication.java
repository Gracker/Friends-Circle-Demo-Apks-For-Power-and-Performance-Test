package com.example.wechatfriendforcustomscroller;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 自定义滚动模块的 Application，负责初始化 Hilt。
 */
@HiltAndroidApp
public class CustomScrollApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}

