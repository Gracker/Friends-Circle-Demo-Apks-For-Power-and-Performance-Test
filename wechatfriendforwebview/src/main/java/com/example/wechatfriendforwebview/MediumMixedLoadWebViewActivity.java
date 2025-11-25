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
 * 混合中负载WebView版朋友圈Activity
 * 同时执行帧内负载和帧间负载（中等强度）
 */
public class MediumMixedLoadWebViewActivity extends BaseFriendCircleWebViewActivity 
        implements Choreographer.FrameCallback {
    private static final String TAG = "MediumMixedLoadWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(12345L);
    
    // 混合负载配置
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    private static final int DOFRAME_TASK_INTENSITY = 2000;
    private static final int BETWEEN_FRAME_TASK_INTENSITY = 1600;
    
    // 绘制资源
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MediumMixedLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 混合中负载");
        
        bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setAntiAlias(true);
        
        choreographer = Choreographer.getInstance();
        choreographer.postFrameCallback(this);
        
        scheduleNextBetweenFrameTask();
        scheduleNextDoFrameTask();
        
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合中负载模式 - 已启动双调度器");
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
            
            Trace.beginSection("MediumMixedWV_doFrameLoad");
            executeDoFrameMediumLoad();
            Trace.endSection();
            
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("MediumMixedWV_betweenFrameLoad");
            executeBetweenFrameMediumLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeDoFrameMediumLoad() {
        double sum = 0;
        for (int i = 0; i < DOFRAME_TASK_INTENSITY; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1) + Math.log(i + 1);
        }
    }
    
    private void executeBetweenFrameMediumLoad() {
        // 数学计算
        double sum = 0;
        for (int i = 1; i <= BETWEEN_FRAME_TASK_INTENSITY / 10; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i) + Math.log(i);
        }
        
        // 矩阵运算
        double[][] matrixA = {{random.nextDouble(), random.nextDouble()}, 
                             {random.nextDouble(), random.nextDouble()}};
        double[][] matrixB = {{random.nextDouble(), random.nextDouble()}, 
                             {random.nextDouble(), random.nextDouble()}};
        double[][] result = new double[2][2];
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // 中等强度图形绘制
        if (canvas != null && paint != null) {
            for (int i = 0; i < 50; i++) {
                paint.setColor(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                canvas.drawCircle(random.nextFloat() * 400, random.nextFloat() * 400, 5 + random.nextFloat() * 15, paint);
            }
        }
        
        // JavaScript中负载
        if (webView != null) {
            String js = "(function() { " +
                    "var s = 0; " +
                    "for(var i = 0; i < 500; i++) { " +
                    "  s += Math.sqrt(i) * Math.sin(i); " +
                    "} " +
                    "return s; " +
                    "})();";
            webView.evaluateJavascript(js, null);
        }
    }
    
    @Override
    protected void executeFlingLoad() {
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad被中断", e);
        }
        
        double result = 0;
        for (int i = 0; i < 200; i++) {
            result += Math.sin(i) * Math.cos(i);
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


