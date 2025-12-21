package com.example.wechatfriendforwebviewimagereader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 混合高负载 GeckoView ImageReader版朋友圈Activity
 */
public class HeavyMixedLoadGeckoViewActivity extends BaseGeckoViewImageReaderActivity {
    private static final String TAG = "HeavyMixedGecko";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyMixedLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView ImageReader朋友圈 - 混合高负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合高负载模式 - 等待滚动时启动");
        isRunning = true;
    }

    @Override
    protected void handleFling(float velocityX, float velocityY) {
        super.handleFling(velocityX, velocityY);
        startBetweenFramesLoad();
    }

    private void startBetweenFramesLoad() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning && isFling) {
                    double result = 0;
                    for (int i = 0; i < 15000; i++) {
                        result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
                    }
                    handler.postDelayed(this, 16);
                }
            }
        }, 16);
    }

    @Override
    protected void executeFlingLoad() {
        try { Thread.sleep(3); } catch (InterruptedException e) {}
        double result = 0;
        double[] data = new double[30000];
        for (int i = 0; i < 30000; i++) {
            data[i] = Math.sqrt(i) * Math.cos(i) * Math.sin(i) * Math.tan(i * 0.1);
            result += data[i];
        }
        Log.d(TAG, "混合高负载计算完成");
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
