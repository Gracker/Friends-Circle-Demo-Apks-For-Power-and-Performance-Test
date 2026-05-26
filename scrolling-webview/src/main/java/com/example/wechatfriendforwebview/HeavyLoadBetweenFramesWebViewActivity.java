package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.util.Log;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * 帧间重负载WebView版朋友圈Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class HeavyLoadBetweenFramesWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "HeavyBetweenFramesWV";

    private LoadSimulator mLoadSimulator;
    private int mLoadType = LoadType.HEAVY_BETWEEN_FRAMES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 帧间重负载");

        mLoadSimulator = new LoadSimulator();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "帧间重负载模式 - 等待 fling 后执行负载");
    }

    @Override
    protected void executeFlingLoad() {
        mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "HeavyBetweenFramesWV_fling");
        if (webView != null && isActivityActive) {
            String js = "(function() { var s = 0; for(var i = 0; i < 1000; i++) { s += Math.pow(Math.sin(i), 2) + Math.pow(Math.cos(i), 2); } return s; })();";
            webView.evaluateJavascript(js, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
        super.onDestroy();
    }
}
