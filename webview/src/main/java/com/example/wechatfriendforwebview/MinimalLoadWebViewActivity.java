package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Trace;
import android.util.Log;

/**
 * 最轻负载WebView版朋友圈Activity
 * 不添加任何额外的负载，仅显示内容
 */
public class MinimalLoadWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "MinimalLoadWebView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MinimalLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("WebView朋友圈 - 最轻负载");
        Trace.endSection();
    }

    /**
     * 执行负载任务 - 最轻负载不执行任何负载
     */
    @Override
    protected void performLoadTask() {
        Log.d(TAG, "最轻负载模式 - 不执行任何额外负载");
    }
    
    /**
     * 在fling过程中执行负载 - 最轻负载不执行任何负载
     */
    @Override
    protected void executeFlingLoad() {
        // 最轻负载：不执行任何负载
        Log.d(TAG, "最轻负载模式 - fling时不执行负载");
    }
}


