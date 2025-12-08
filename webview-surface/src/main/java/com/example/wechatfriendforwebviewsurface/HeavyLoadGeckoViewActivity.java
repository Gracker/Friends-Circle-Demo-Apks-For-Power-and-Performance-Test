package com.example.wechatfriendforwebviewsurface;

import android.os.Bundle;
import android.os.Trace;
import android.util.Log;

/**
 * 重负载 GeckoView SurfaceView版朋友圈Activity
 */
public class HeavyLoadGeckoViewActivity extends BaseGeckoViewSurfaceActivity {
    private static final String TAG = "HeavyLoadGeckoView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyLoadGeckoViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        setTitle("GeckoView SurfaceView朋友圈 - 帧内高负载");
        Trace.endSection();
    }

    @Override
    protected void performLoadTask() {
        Log.d(TAG, "重负载模式");
    }
    
    @Override
    protected void executeFlingLoad() {
        try { Thread.sleep(3); } catch (InterruptedException e) {}
        
        long startTime = System.currentTimeMillis();
        double result = 0;
        double[] data = new double[50000];
        for (int i = 0; i < 50000; i++) {
            data[i] = Math.sqrt(i) * Math.cos(i) * Math.sin(i);
            result += data[i];
        }
        java.util.Arrays.sort(data);
        Log.d(TAG, "重负载计算完成, 耗时: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
