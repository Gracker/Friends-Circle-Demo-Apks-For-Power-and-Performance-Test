package com.android.wechatfriendforpower;

import android.app.Application;
import android.content.Context;

/**
 * Power测试专用的Application类
 */
public class PowerFriendsCircleApplication extends Application {

    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        // 初始化数据在这里进行，确保每次都使用相同的数据
        PowerDataCenter.init();
    }
} 