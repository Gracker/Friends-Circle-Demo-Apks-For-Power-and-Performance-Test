package com.example.wechatfriendforwebviewimagereader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.app.Activity;

/**
 * GeckoView ImageReader版朋友圈主界面
 * 提供选择不同负载级别的入口，用于性能测试对比
 * 
 * 渲染模式：ImageReader
 * - GeckoView 渲染到 ImageReader 的 Surface
 * - 从 ImageReader 获取 Image，转换为 Bitmap 显示
 * - 合成路径需要经过 App 的 UI Thread + RenderThread
 */
public class GeckoViewMainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "GeckoViewMainActivity";

    // UI组件
    private Button btnMinimalLoad;
    private Button btnLightLoad;
    private Button btnMediumLoad; 
    private Button btnHeavyLoad;
    private Button btnLightBetweenFrames;
    private Button btnMediumBetweenFrames;
    private Button btnHeavyBetweenFrames;
    private Button btnLightMixed;
    private Button btnMediumMixed;
    private Button btnHeavyMixed;
    
    // 定义常量，用于Intent传递负载类型
    public static final String EXTRA_LOAD_TYPE = "load_type";
    
    // GeckoView负载类型常量
    public static final int LOAD_TYPE_MINIMAL = 0;
    public static final int LOAD_TYPE_LIGHT = 1;
    public static final int LOAD_TYPE_MEDIUM = 2;
    public static final int LOAD_TYPE_HEAVY = 3;
    public static final int LOAD_TYPE_LIGHT_BETWEEN_FRAMES = 4;
    public static final int LOAD_TYPE_MEDIUM_BETWEEN_FRAMES = 5;
    public static final int LOAD_TYPE_HEAVY_BETWEEN_FRAMES = 6;
    public static final int LOAD_TYPE_LIGHT_MIXED = 7;
    public static final int LOAD_TYPE_MEDIUM_MIXED = 8;
    public static final int LOAD_TYPE_HEAVY_MIXED = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("GeckoViewMainActivity_onCreate");
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_geckoview_main);
        
        Log.d(TAG, "初始化GeckoView ImageReader版朋友圈测试主界面");
        
        // 初始化按钮
        initViews();
        
        // 设置点击监听器
        setClickListeners();
        
        Trace.endSection();
    }
    
    /**
     * 初始化界面控件
     */
    private void initViews() {
        Trace.beginSection("GeckoViewMainActivity_initViews");
        
        btnMinimalLoad = findViewById(R.id.btn_minimal_load);
        btnLightLoad = findViewById(R.id.btn_light_load);
        btnMediumLoad = findViewById(R.id.btn_medium_load);
        btnHeavyLoad = findViewById(R.id.btn_heavy_load);
        btnLightBetweenFrames = findViewById(R.id.btn_light_between_frames);
        btnMediumBetweenFrames = findViewById(R.id.btn_medium_between_frames);
        btnHeavyBetweenFrames = findViewById(R.id.btn_heavy_between_frames);
        btnLightMixed = findViewById(R.id.btn_light_mixed);
        btnMediumMixed = findViewById(R.id.btn_medium_mixed);
        btnHeavyMixed = findViewById(R.id.btn_heavy_mixed);
        
        Trace.endSection();
    }
    
    /**
     * 设置点击监听器
     */
    private void setClickListeners() {
        Trace.beginSection("GeckoViewMainActivity_setClickListeners");
        
        btnMinimalLoad.setOnClickListener(this);
        btnLightLoad.setOnClickListener(this);
        btnMediumLoad.setOnClickListener(this);
        btnHeavyLoad.setOnClickListener(this);
        btnLightBetweenFrames.setOnClickListener(this);
        btnMediumBetweenFrames.setOnClickListener(this);
        btnHeavyBetweenFrames.setOnClickListener(this);
        btnLightMixed.setOnClickListener(this);
        btnMediumMixed.setOnClickListener(this);
        btnHeavyMixed.setOnClickListener(this);
        
        Trace.endSection();
    }

    @Override
    protected void onResume() {
        Trace.beginSection("GeckoViewMainActivity_onResume");
        super.onResume();
        
        // 确保数据中心的缓存被清空
        GeckoViewDataCenter.getInstance().clearCachedData();
        Log.d(TAG, "数据缓存已清除");
        
        Trace.endSection();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        // 先清除所有缓存的数据，确保每次都重新生成
        GeckoViewDataCenter.getInstance().clearCachedData();
        
        Intent intent = null;
        int loadType = LOAD_TYPE_LIGHT;
        String loadName = "";
        
        if (id == R.id.btn_minimal_load) {
            intent = new Intent(this, MinimalLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_MINIMAL;
            loadName = "最轻负载";
        } else if (id == R.id.btn_light_load) {
            intent = new Intent(this, LightLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_LIGHT;
            loadName = "帧内轻负载";
        } else if (id == R.id.btn_medium_load) {
            intent = new Intent(this, MediumLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_MEDIUM;
            loadName = "帧内中负载";
        } else if (id == R.id.btn_heavy_load) {
            intent = new Intent(this, HeavyLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_HEAVY;
            loadName = "帧内高负载";
        } else if (id == R.id.btn_light_between_frames) {
            intent = new Intent(this, LightLoadBetweenFramesGeckoViewActivity.class);
            loadType = LOAD_TYPE_LIGHT_BETWEEN_FRAMES;
            loadName = "帧间轻负载";
        } else if (id == R.id.btn_medium_between_frames) {
            intent = new Intent(this, MediumLoadBetweenFramesGeckoViewActivity.class);
            loadType = LOAD_TYPE_MEDIUM_BETWEEN_FRAMES;
            loadName = "帧间中负载";
        } else if (id == R.id.btn_heavy_between_frames) {
            intent = new Intent(this, HeavyLoadBetweenFramesGeckoViewActivity.class);
            loadType = LOAD_TYPE_HEAVY_BETWEEN_FRAMES;
            loadName = "帧间高负载";
        } else if (id == R.id.btn_light_mixed) {
            intent = new Intent(this, LightMixedLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_LIGHT_MIXED;
            loadName = "混合轻负载";
        } else if (id == R.id.btn_medium_mixed) {
            intent = new Intent(this, MediumMixedLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_MEDIUM_MIXED;
            loadName = "混合中负载";
        } else if (id == R.id.btn_heavy_mixed) {
            intent = new Intent(this, HeavyMixedLoadGeckoViewActivity.class);
            loadType = LOAD_TYPE_HEAVY_MIXED;
            loadName = "混合高负载";
        }
        
        if (intent != null) {
            Trace.beginSection("GeckoViewMainActivity_start" + loadName);
            Log.d(TAG, "启动" + loadName + "GeckoView ImageReader版朋友圈");
            intent.putExtra(EXTRA_LOAD_TYPE, loadType);
            startActivity(intent);
            Trace.endSection();
        }
    }
}

