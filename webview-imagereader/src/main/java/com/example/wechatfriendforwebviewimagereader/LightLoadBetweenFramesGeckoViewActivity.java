package com.example.wechatfriendforwebviewimagereader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 帧间轻负载 GeckoView ImageReader版朋友圈Activity
 */
public class LightLoadBetweenFramesGeckoViewActivity extends BaseGeckoViewImageReaderActivity {
    private static final String TAG = "LightBetweenFramesGecko";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightLoadBetweenFramesGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView ImageReader朋友圈 - 帧间轻负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间轻负载模式 - 等待滚动时启动");
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
                    for (int i = 0; i < 1000; i++) {
                        result += Math.sqrt(i);
                    }
                    handler.postDelayed(this, 16);
                }
            }
        }, 16);
    }
    
    @Override
    protected void executeFlingLoad() {
        try { Thread.sleep(1); } catch (InterruptedException e) {}
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
