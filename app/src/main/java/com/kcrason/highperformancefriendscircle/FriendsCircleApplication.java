package com.kcrason.highperformancefriendscircle;

import android.app.Application;
import android.content.Context;

import com.kcrason.highperformancefriendscircle.others.DataCenter;

/**
 * @author KCrason
 * @date 2018/5/3
 */
public class FriendsCircleApplication extends Application {

    private static FriendsCircleApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        DataCenter.init();

        // 设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            android.util.Log.e("FriendsCircleApp", "Uncaught exception in thread " + thread.getName(), throwable);
            // 调用默认处理器
            Thread.getDefaultUncaughtExceptionHandler();
        });
    }

    /**
     * 获取Application Context，避免内存泄漏
     * @return Application Context
     */
    public static Context getAppContext() {
        return sInstance != null ? sInstance.getApplicationContext() : null;
    }

    /**
     * @deprecated 使用 getAppContext() 代替，避免内存泄漏
     */
    @Deprecated
    public static Context sContext() {
        return getAppContext();
    }
}
