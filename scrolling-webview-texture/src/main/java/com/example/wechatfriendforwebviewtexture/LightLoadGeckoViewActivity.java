package com.example.wechatfriendforwebviewtexture;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 轻负载 GeckoView SurfaceTexture版朋友圈Activity
 * 帧内轻负载实现
 */
public class LightLoadGeckoViewActivity extends BaseGeckoViewTextureActivity {
    private static final String TAG = "LightLoadGeckoView";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView SurfaceTexture朋友圈 - 帧内轻负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "轻负载模式 - 仅在滚动时执行负载");
    }

    @Override
    protected void executeFlingLoad() {
        // 轻微阻塞主线程
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad阻塞被中断", e);
        }

        // 轻负载：简单的计算
        long startTime = System.currentTimeMillis();
        double result = 0;
        for (int i = 0; i < 5000; i++) {
            result += Math.sqrt(i) * Math.cos(i);
        }
        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "轻负载计算完成, 耗时: " + duration + "ms, 结果: " + result);
    }
}
