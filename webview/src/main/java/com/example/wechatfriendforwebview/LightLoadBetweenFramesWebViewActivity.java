package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * 帧间轻负载WebView版朋友圈Activity
 * 使用统一的 LoadSimulator 执行负载，帧间隔由 LoadSimulator 统一控制
 */
public class LightLoadBetweenFramesWebViewActivity extends BaseFriendCircleWebViewActivity 
        implements Choreographer.FrameCallback {
    private static final String TAG = "LightBetweenFramesWV";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Choreographer choreographer;
    private boolean isTaskSchedulingEnabled = true;
    
    private LoadSimulator mLoadSimulator;
    private int mLoadType = LoadType.LIGHT_BETWEEN_FRAMES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 帧间轻负载");
        
        mLoadSimulator = new LoadSimulator();
        choreographer = Choreographer.getInstance();
        
        // 启动帧回调，由 LoadSimulator 统一控制伪随机帧间隔
        choreographer.postFrameCallback(this);
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间轻负载模式 - 已启动帧间任务调度");
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isTaskSchedulingEnabled) return;
        
        // 每帧调用，由 LoadSimulator 统一控制伪随机帧间隔
        mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "LightBetweenFramesWV_load");
        
        // 执行JavaScript轻负载（每帧执行）
        if (webView != null) {
            String js = "(function() { var s = 0; for(var i = 0; i < 100; i++) { s += Math.sqrt(i); } return s; })();";
            webView.evaluateJavascript(js, null);
        }
        
        choreographer.postFrameCallback(this);
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
