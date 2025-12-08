package com.example.wechatfriendforwebviewimagereader;

import android.app.Application;
import android.util.Log;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;

/**
 * GeckoView ImageReader 版朋友圈应用
 * 初始化 GeckoRuntime，确保整个应用只有一个 GeckoRuntime 实例
 */
public class GeckoViewApplication extends Application {
    private static final String TAG = "GeckoViewApp";
    
    private static GeckoRuntime sGeckoRuntime;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GeckoView ImageReader 版朋友圈应用启动");
    }
    
    /**
     * 获取 GeckoRuntime 实例（懒加载）
     */
    public static synchronized GeckoRuntime getGeckoRuntime(Application app) {
        if (sGeckoRuntime == null) {
            Log.d(TAG, "创建 GeckoRuntime 实例");
            
            GeckoRuntimeSettings.Builder settingsBuilder = new GeckoRuntimeSettings.Builder();
            
            // 启用远程调试
            settingsBuilder.remoteDebuggingEnabled(true);
            
            // 启用 JavaScript
            settingsBuilder.javaScriptEnabled(true);
            
            // 启用 Web 字体
            settingsBuilder.webFontsEnabled(true);
            
            // 创建 GeckoRuntime
            sGeckoRuntime = GeckoRuntime.create(app, settingsBuilder.build());
            
            Log.d(TAG, "GeckoRuntime 创建成功");
        }
        return sGeckoRuntime;
    }
    
    public GeckoRuntime getGeckoRuntime() {
        return getGeckoRuntime(this);
    }
}
