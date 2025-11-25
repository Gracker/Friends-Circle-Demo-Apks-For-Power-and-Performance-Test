package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

import java.util.Random;

/**
 * 帧间中负载WebView版朋友圈Activity
 * 在帧与帧之间执行中等强度后台任务
 */
public class MediumLoadBetweenFramesWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "MediumBetweenFramesWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(12345L);
    
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    private static final int COMPUTATION_LOOP_COUNT = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MediumLoadBetweenFramesWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 帧间中负载");
        
        scheduleNextBetweenFrameTask();
        
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间中负载模式 - 已启动帧间任务调度");
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            Trace.beginSection("MediumBetweenFramesWV_task");
            executeBetweenFrameLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeBetweenFrameLoad() {
        // 中等强度计算
        double sum = 0;
        for (int i = 0; i < COMPUTATION_LOOP_COUNT; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1) + Math.log(i + 1);
        }
        
        // 字符串处理
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("MediumTask").append(i).append("_");
        }
        String processed = sb.toString().toUpperCase();
        
        // 执行JavaScript中负载
        if (webView != null) {
            String js = "(function() { " +
                    "var s = 0; " +
                    "for(var i = 0; i < 500; i++) { " +
                    "  s += Math.sqrt(i) * Math.sin(i); " +
                    "  var arr = []; " +
                    "  for(var j = 0; j < 10; j++) arr.push(j); " +
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
            Thread.sleep(3);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad被中断", e);
        }
        
        // 中等强度计算
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


