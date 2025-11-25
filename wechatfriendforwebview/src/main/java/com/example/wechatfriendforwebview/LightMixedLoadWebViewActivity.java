package com.example.wechatfriendforwebview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.view.Choreographer;

import java.util.Random;

/**
 * 混合轻负载WebView版朋友圈Activity
 * 同时执行帧内负载和帧间负载
 */
public class LightMixedLoadWebViewActivity extends BaseFriendCircleWebViewActivity 
        implements Choreographer.FrameCallback {
    private static final String TAG = "LightMixedLoadWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(12345L);
    
    // 混合负载配置
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    private static final int DOFRAME_TASK_INTENSITY = 1000;
    private static final int BETWEEN_FRAME_TASK_INTENSITY = 2400;
    
    // 绘制资源
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightMixedLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 混合轻负载");
        
        // 初始化绘制资源
        bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setAntiAlias(true);
        
        // 启动Choreographer帧回调
        choreographer = Choreographer.getInstance();
        choreographer.postFrameCallback(this);
        
        // 启动帧间任务调度
        scheduleNextBetweenFrameTask();
        scheduleNextDoFrameTask();
        
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合轻负载模式 - 已启动双调度器");
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (isTaskSchedulingEnabled) {
            choreographer.postFrameCallback(this);
        }
    }
    
    private void scheduleNextDoFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("LightMixedWV_doFrameLoad");
            executeDoFrameLightLoad();
            Trace.endSection();
            
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("LightMixedWV_betweenFrameLoad");
            executeBetweenFrameLightLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeDoFrameLightLoad() {
        double sum = 0;
        for (int i = 0; i < DOFRAME_TASK_INTENSITY; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
    }
    
    private void executeBetweenFrameLightLoad() {
        // 数学计算
        double sum = 0;
        for (int i = 1; i <= BETWEEN_FRAME_TASK_INTENSITY / 20; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i);
        }
        
        // 简单图形绘制
        if (canvas != null && paint != null) {
            paint.setColor(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            for (int i = 0; i < 20; i++) {
                canvas.drawCircle(random.nextFloat() * 200, random.nextFloat() * 200, 5 + random.nextFloat() * 10, paint);
            }
        }
        
        // JavaScript负载
        if (webView != null) {
            String js = "(function() { var s = 0; for(var i = 0; i < 200; i++) { s += Math.sqrt(i); } return s; })();";
            webView.evaluateJavascript(js, null);
        }
    }
    
    @Override
    protected void executeFlingLoad() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad被中断", e);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!isTaskSchedulingEnabled) {
            isTaskSchedulingEnabled = true;
            choreographer.postFrameCallback(this);
            scheduleNextBetweenFrameTask();
            scheduleNextDoFrameTask();
        }
    }
    
    @Override
    protected void onDestroy() {
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        canvas = null;
        super.onDestroy();
    }
}


