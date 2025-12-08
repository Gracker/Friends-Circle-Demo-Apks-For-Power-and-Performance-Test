package com.example.wechatfriendforwebviewimagereader;

import android.os.Bundle;
import android.os.Trace;
import android.util.Log;

/**
 * 中负载 GeckoView ImageReader版朋友圈Activity
 */
public class MediumLoadGeckoViewActivity extends BaseGeckoViewImageReaderActivity {
    private static final String TAG = "MediumLoadGeckoView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MediumLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView ImageReader朋友圈 - 帧内中负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "中负载模式");
    }
    
    @Override
    protected void executeFlingLoad() {
        try { Thread.sleep(2); } catch (InterruptedException e) {}
        
        long startTime = System.currentTimeMillis();
        double result = 0;
        for (int i = 0; i < 15000; i++) {
            result += Math.sqrt(i) * Math.cos(i) * Math.sin(i);
        }
        Log.d(TAG, "中负载计算完成, 耗时: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
