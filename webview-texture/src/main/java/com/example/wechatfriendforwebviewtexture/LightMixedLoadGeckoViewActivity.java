package com.example.wechatfriendforwebviewtexture;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 混合轻负载 GeckoView SurfaceTexture版朋友圈Activity
 */
public class LightMixedLoadGeckoViewActivity extends BaseGeckoViewTextureActivity {
    private static final String TAG = "LightMixedGecko";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightMixedLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView SurfaceTexture朋友圈 - 混合轻负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合轻负载模式");
        isRunning = true;
        startBetweenFramesLoad();
    }
    
    private void startBetweenFramesLoad() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
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
        if (!isRunning) {
            isRunning = true;
            startBetweenFramesLoad();
        }
    }
    
    @Override
    protected void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
