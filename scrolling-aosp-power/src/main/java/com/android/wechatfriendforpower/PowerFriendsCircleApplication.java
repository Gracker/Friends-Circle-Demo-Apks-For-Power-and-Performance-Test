package com.android.wechatfriendforpower;

import android.app.Application;
import android.content.Context;

/**
 * Power测试专用的Application类
 */
public class PowerFriendsCircleApplication extends Application {

    private static PowerFriendsCircleApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        // 初始化数据在这里进行，确保每次都使用相同的数据
        PowerDataCenter.init();
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