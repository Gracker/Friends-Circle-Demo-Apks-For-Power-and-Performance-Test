package com.example.wechatfriendforsoftwarerender;

import android.app.Application;

/**
 * Application class for performance test module
 */
public class SoftwareRenderApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 清除上一次运行可能残留的缓存数据，确保每次都重新生成数据
        SoftwareRenderDataCenter.getInstance().clearCachedData();
        
        // 预初始化数据中心，预热系统
        SoftwareRenderDataCenter.getInstance();
    }
} 