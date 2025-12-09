package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * 混合中负载WebView版朋友圈Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class MediumMixedLoadWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "MediumMixedWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTaskSchedulingEnabled = true;
    private final Random taskIntervalRandom = new Random(LoadConfig.TASK_INTERVAL_SEED);
    private final Random doFrameIntervalRandom = new Random(LoadConfig.DOFRAME_INTERVAL_SEED);
    
    private LoadSimulator mLoadSimulator;
    private int mLoadType = LoadType.MEDIUM_MIXED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 混合中负载");
        
        mLoadSimulator = new LoadSimulator();
        
        // 启动任务调度
        scheduleNextBetweenFrameTask();
        scheduleNextDoFrameTask();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合中负载模式 - 已启动任务调度");
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + 
                         taskIntervalRandom.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MediumMixedWV_betweenFrameLoad");
            
            if (webView != null) {
                String js = "(function() { var s = 0; for(var i = 0; i < 500; i++) { s += Math.sin(i) * Math.cos(i); } return s; })();";
                webView.evaluateJavascript(js, null);
            }
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextDoFrameTask() {
        if (!isTaskSchedulingEnabled) return;
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + 
                         doFrameIntervalRandom.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            mLoadSimulator.executeDoFrameLoad(mLoadType, "MediumMixedWV_doFrameLoad");
            
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    @Override
    protected void executeFlingLoad() {
        mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MediumMixedWV_fling");
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
            scheduleNextDoFrameTask();
        }
    }
    
    @Override
    protected void onDestroy() {
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
        super.onDestroy();
    }
}
