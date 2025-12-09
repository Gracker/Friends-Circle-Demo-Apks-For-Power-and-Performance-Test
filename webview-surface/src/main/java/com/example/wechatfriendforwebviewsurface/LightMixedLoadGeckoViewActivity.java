package com.example.wechatfriendforwebviewsurface;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 混合轻负载 GeckoView SurfaceView版朋友圈Activity
 */
public class LightMixedLoadGeckoViewActivity extends BaseGeckoViewSurfaceActivity {
    private static final String TAG = "LightMixedGecko";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightMixedLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView SurfaceView朋友圈 - 混合轻负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合轻负载模式 - 等待滚动时启动");
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
                    for (int i = 0; i < 500; i++) {
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
        double result = 0;
        for (int i = 0; i < 2000; i++) {
            result += Math.sqrt(i) * Math.cos(i);
        }
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
