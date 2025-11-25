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
 * 混合高负载WebView版朋友圈Activity
 * 同时执行帧内负载和帧间负载（高强度）
 */
public class HeavyMixedLoadWebViewActivity extends BaseFriendCircleWebViewActivity 
        implements Choreographer.FrameCallback {
    private static final String TAG = "HeavyMixedLoadWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(12345L);
    
    // 混合负载配置
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    private static final int DOFRAME_TASK_INTENSITY = 4000;
    private static final int BETWEEN_FRAME_TASK_INTENSITY = 1067;
    
    // 绘制资源
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyMixedLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 混合高负载");
        
        bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
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
        Log.d(TAG, "混合高负载模式 - 已启动双调度器");
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
            
            Trace.beginSection("HeavyMixedWV_doFrameLoad");
            executeDoFrameHeavyLoad();
            Trace.endSection();
            
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("HeavyMixedWV_betweenFrameLoad");
            executeBetweenFrameHeavyLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeDoFrameHeavyLoad() {
        double sum = 0;
        for (int i = 0; i < DOFRAME_TASK_INTENSITY; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1) + Math.log(i + 1) + Math.tan(i * 0.01);
        }
    }
    
    private void executeBetweenFrameHeavyLoad() {
        // 高强度数学计算
        double sum = 0;
        for (int i = 1; i <= BETWEEN_FRAME_TASK_INTENSITY; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i) + Math.log(i) + Math.tan(i * 0.01);
        }
        
        // 复杂矩阵运算
        double[][] matrixA = new double[5][5];
        double[][] matrixB = new double[5][5];
        double[][] result = new double[5][5];
        
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                matrixA[i][j] = random.nextDouble();
                matrixB[i][j] = random.nextDouble();
            }
        }
        
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // 高强度图形绘制
        if (canvas != null && paint != null) {
            for (int i = 0; i < 100; i++) {
                paint.setColor(Color.argb(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256)
                ));
                canvas.drawCircle(random.nextFloat() * 600, random.nextFloat() * 600, 5 + random.nextFloat() * 25, paint);
            }
        }
        
        // JavaScript高负载
        if (webView != null) {
            String js = "(function() { " +
                    "var s = 0; " +
                    "for(var i = 0; i < 2000; i++) { " +
                    "  s += Math.sqrt(i) * Math.sin(i) * Math.cos(i); " +
                    "  var arr = []; " +
                    "  for(var j = 0; j < 20; j++) arr.push(Math.random()); " +
                    "  arr.sort(); " +
                    "} " +
                    "return s; " +
                    "})();";
            webView.evaluateJavascript(js, null);
        }
    }
    
    @Override
    protected void executeFlingLoad() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad被中断", e);
        }
        
        double result = 0;
        for (int i = 0; i < 500; i++) {
            result += Math.sin(i) * Math.cos(i) * Math.tan(i * 0.01);
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


