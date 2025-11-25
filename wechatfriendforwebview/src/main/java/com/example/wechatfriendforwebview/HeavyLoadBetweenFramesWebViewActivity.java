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

import java.util.Random;

/**
 * 帧间高负载WebView版朋友圈Activity
 * 在帧与帧之间执行高强度后台任务
 */
public class HeavyLoadBetweenFramesWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "HeavyBetweenFramesWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(12345L);
    
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    private static final int COMPUTATION_LOOP_COUNT = 800;
    
    // 绘制资源
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyLoadBetweenFramesWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 帧间高负载");
        
        // 初始化绘制资源
        bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setAntiAlias(true);
        
        scheduleNextBetweenFrameTask();
        
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间高负载模式 - 已启动帧间任务调度");
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("HeavyBetweenFramesWV_task");
            executeBetweenFrameLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeBetweenFrameLoad() {
        // 高强度计算
        double sum = 0;
        for (int i = 0; i < COMPUTATION_LOOP_COUNT; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1) + Math.log(i + 1) + Math.tan(i * 0.01);
        }
        
        // 矩阵运算
        double[][] matrixA = new double[10][10];
        double[][] matrixB = new double[10][10];
        double[][] result = new double[10][10];
        
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrixA[i][j] = random.nextDouble();
                matrixB[i][j] = random.nextDouble();
            }
        }
        
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 10; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // 图形绘制
        if (canvas != null && paint != null) {
            for (int i = 0; i < 100; i++) {
                paint.setColor(Color.argb(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256)
                ));
                canvas.drawCircle(
                        random.nextFloat() * 600,
                        random.nextFloat() * 600,
                        5 + random.nextFloat() * 20,
                        paint
                );
            }
        }
        
        // 执行JavaScript高负载
        if (webView != null) {
            String js = "(function() { " +
                    "var s = 0; " +
                    "for(var i = 0; i < 2000; i++) { " +
                    "  s += Math.sqrt(i) * Math.sin(i) * Math.cos(i); " +
                    "  var arr = []; " +
                    "  for(var j = 0; j < 50; j++) arr.push(Math.random()); " +
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
        
        // 高强度计算
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
            scheduleNextBetweenFrameTask();
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


