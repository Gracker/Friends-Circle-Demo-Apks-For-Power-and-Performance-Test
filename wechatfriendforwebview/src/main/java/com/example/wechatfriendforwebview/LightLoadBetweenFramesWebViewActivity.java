package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

import java.util.Random;

/**
 * 帧间轻负载WebView版朋友圈Activity
 * 在帧与帧之间执行轻量级后台任务
 */
public class LightLoadBetweenFramesWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "LightBetweenFramesWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(12345L);
    
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    private static final int COMPUTATION_LOOP_COUNT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightLoadBetweenFramesWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 帧间轻负载");
        
        // 启动帧间任务调度
        scheduleNextBetweenFrameTask();
        
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间轻负载模式 - 已启动帧间任务调度");
    }
    
    /**
     * 调度下一个帧间任务
     */
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("LightBetweenFramesWV_task");
            executeBetweenFrameLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    /**
     * 执行帧间轻负载
     */
    private void executeBetweenFrameLoad() {
        // 轻量级计算
        double sum = 0;
        for (int i = 0; i < COMPUTATION_LOOP_COUNT; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        
        // 执行JavaScript轻负载
        if (webView != null) {
            String js = "(function() { var s = 0; for(var i = 0; i < 100; i++) { s += Math.sqrt(i); } return s; })();";
            webView.evaluateJavascript(js, null);
        }
    }
    
    @Override
    protected void executeFlingLoad() {
        // 帧间模式：fling时执行轻量负载
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
            scheduleNextBetweenFrameTask();
        }
    }
    
    @Override
    protected void onDestroy() {
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}


