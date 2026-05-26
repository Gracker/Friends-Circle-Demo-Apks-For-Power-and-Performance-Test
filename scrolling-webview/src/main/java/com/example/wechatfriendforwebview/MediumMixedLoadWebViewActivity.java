package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.util.Log;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * 混合中负载WebView版朋友圈Activity
 * 使用统一的 LoadSimulator 执行负载，帧间隔由 LoadSimulator 统一控制
 */
public class MediumMixedLoadWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "MediumMixedWV";

    private LoadSimulator mLoadSimulator;
    private int mLoadType = LoadType.MEDIUM_MIXED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 混合中负载");

        mLoadSimulator = new LoadSimulator();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "混合中负载模式 - 等待 fling 后执行负载");
    }

    @Override
    protected void executeFlingLoad() {
        mLoadSimulator.executeDoFrameLoad(mLoadType, "MediumMixedWV_fling_doFrame");
        mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MediumMixedWV_fling_betweenFrame");
        if (webView != null && isActivityActive) {
            String js = "(function() { var s = 0; for(var i = 0; i < 500; i++) { s += Math.sin(i) * Math.cos(i); } return s; })();";
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
