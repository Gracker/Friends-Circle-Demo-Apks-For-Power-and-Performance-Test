package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * 混合中负载WebView版朋友圈Activity
 * 使用统一的 LoadSimulator 执行负载，帧间隔由 LoadSimulator 统一控制
 */
public class MediumMixedLoadWebViewActivity extends BaseFriendCircleWebViewActivity 
        implements Choreographer.FrameCallback {
    private static final String TAG = "MediumMixedWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private boolean isTaskSchedulingEnabled = true;
    
    private LoadSimulator mLoadSimulator;
    private int mLoadType = LoadType.MEDIUM_MIXED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 混合中负载");
        
        mLoadSimulator = new LoadSimulator();
        choreographer = Choreographer.getInstance();
        
        // 启动帧回调，由 LoadSimulator 统一控制伪随机帧间隔
        choreographer.postFrameCallback(this);
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合中负载模式 - 已启动任务调度");
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isTaskSchedulingEnabled) return;
        
        // 混合负载：每帧调用 doFrame 和 betweenFrame，由 LoadSimulator 统一控制伪随机帧间隔
        mLoadSimulator.executeDoFrameLoad(mLoadType, "MediumMixedWV_doFrameLoad");
        handler.post(() -> mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MediumMixedWV_betweenFrameLoad"));
        
        // 执行JavaScript负载（每帧执行）
        if (webView != null) {
            String js = "(function() { var s = 0; for(var i = 0; i < 500; i++) { s += Math.sin(i) * Math.cos(i); } return s; })();";
            webView.evaluateJavascript(js, null);
        }
        
        choreographer.postFrameCallback(this);
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
        if (choreographer != null) {
            choreographer.removeFrameCallback(this);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!isTaskSchedulingEnabled) {
            isTaskSchedulingEnabled = true;
            choreographer.postFrameCallback(this);
        }
    }
    
    @Override
    protected void onDestroy() {
        isTaskSchedulingEnabled = false;
        handler.removeCallbacksAndMessages(null);
        if (choreographer != null) {
            choreographer.removeFrameCallback(this);
        }
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
        super.onDestroy();
    }
}
