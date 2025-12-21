package com.example.wechatfriendforwebviewtexture;

import android.os.Bundle;
import android.os.Trace;
import android.util.Log;

/**
 * 最轻负载 GeckoView SurfaceTexture版朋友圈Activity
 * 不添加任何额外负载
 */
public class MinimalLoadGeckoViewActivity extends BaseGeckoViewTextureActivity {
    private static final String TAG = "MinimalLoadGeckoView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MinimalLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView SurfaceTexture朋友圈 - 最轻负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "最轻负载模式 - 不执行任何负载");
    }

    @Override
    protected void executeFlingLoad() {
        // 最轻负载：不执行任何负载
        Log.d(TAG, "最轻负载 - fling不执行负载");
    }
}

