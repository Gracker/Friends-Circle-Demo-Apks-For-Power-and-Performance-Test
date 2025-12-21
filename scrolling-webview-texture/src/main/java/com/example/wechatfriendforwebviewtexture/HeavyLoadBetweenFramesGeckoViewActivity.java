package com.example.wechatfriendforwebviewtexture;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 帧间高负载 GeckoView SurfaceTexture版朋友圈Activity
 */
public class HeavyLoadBetweenFramesGeckoViewActivity extends BaseGeckoViewTextureActivity {
    private static final String TAG = "HeavyBetweenFramesGecko";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyLoadBetweenFramesGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView SurfaceTexture朋友圈 - 帧间高负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间高负载模式 - 等待滚动时启动");
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
                    for (int i = 0; i < 20000; i++) {
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
