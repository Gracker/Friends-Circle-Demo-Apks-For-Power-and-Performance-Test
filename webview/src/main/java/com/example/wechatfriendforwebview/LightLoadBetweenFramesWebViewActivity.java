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
 * 帧间轻负载WebView版朋友圈Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class LightLoadBetweenFramesWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "LightBetweenFramesWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTaskSchedulingEnabled = true;
    private final Random random = new Random(LoadConfig.TASK_INTERVAL_SEED);
    
    private LoadSimulator mLoadSimulator;
    private int mLoadType = LoadType.LIGHT_BETWEEN_FRAMES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 帧间轻负载");
        
        mLoadSimulator = new LoadSimulator();
        
        // 启动帧间任务调度
        scheduleNextBetweenFrameTask();
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
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + 
                         random.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
        handler.postDelayed(() -> {
            if (!isTaskSchedulingEnabled) return;
            
            mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "LightBetweenFramesWV_load");
            
            // 执行JavaScript轻负载
            if (webView != null) {
                String js = "(function() { var s = 0; for(var i = 0; i < 100; i++) { s += Math.sqrt(i); } return s; })();";
                webView.evaluateJavascript(js, null);
            }
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    @Override
    protected void executeFlingLoad() {
        // 帧间模式：fling时执行轻量负载
        mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "LightBetweenFramesWV_fling");
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
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
        super.onDestroy();
    }
}
