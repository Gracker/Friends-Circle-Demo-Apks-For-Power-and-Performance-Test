package com.example.wechatfriendforwebview;

import android.app.Application;
import android.os.Trace;

/**
 * WebView版朋友圈应用程序类
 */
public class WebViewApplication extends Application {

    @Override
    public void onCreate() {
        Trace.beginSection("WebViewApplication_onCreate");
        super.onCreate();
        
        // 清除缓存数据
        WebViewDataCenter.getInstance().clearCachedData();
        
        // 预初始化数据中心实例
        WebViewDataCenter.getInstance();
        
        Trace.endSection();
    }
} 