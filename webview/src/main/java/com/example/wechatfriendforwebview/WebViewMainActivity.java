package com.example.wechatfriendforwebview;

import android.content.Intent;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * WebView版朋友圈主界面
 * 提供选择不同负载级别的入口，用于性能测试对比
 */
public class WebViewMainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "WebViewMainActivity";

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
    
    // 负载类型使用统一的 LoadType 常量，不再本地定义

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("WebViewMainActivity_onCreate");
        super.onCreate(savedInstanceState);
        
        // 应用无ActionBar的主题
        setTheme(R.style.Theme_HighPerformanceFriendsCircle_NoActionBar);
        setContentView(R.layout.activity_webview_main);
        
        Log.d(TAG, "初始化WebView朋友圈测试主界面");
        
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
        Trace.beginSection("WebViewMainActivity_initViews");
        
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
        Trace.beginSection("WebViewMainActivity_setClickListeners");
        
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
        Trace.beginSection("WebViewMainActivity_onResume");
        super.onResume();
        
        // 确保数据中心的缓存被清空
        WebViewDataCenter.getInstance().clearCachedData();
        Log.d(TAG, "数据缓存已清除");
        
        Trace.endSection();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        // 先清除所有缓存的数据，确保每次都重新生成
        WebViewDataCenter.getInstance().clearCachedData();
        
        Intent intent = null;
        int loadType = com.example.loadconfig.LoadType.LIGHT;
        String loadName = "";
        
        if (id == R.id.btn_minimal_load) {
            intent = new Intent(this, MinimalLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.MINIMAL;
            loadName = "最轻负载";
        } else if (id == R.id.btn_light_load) {
            intent = new Intent(this, LightLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.LIGHT;
            loadName = "帧内轻负载";
        } else if (id == R.id.btn_medium_load) {
            intent = new Intent(this, MediumLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.MEDIUM;
            loadName = "帧内中负载";
        } else if (id == R.id.btn_heavy_load) {
            intent = new Intent(this, HeavyLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.HEAVY;
            loadName = "帧内高负载";
        } else if (id == R.id.btn_light_between_frames) {
            intent = new Intent(this, LightLoadBetweenFramesWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.LIGHT_BETWEEN_FRAMES;
            loadName = "帧间轻负载";
        } else if (id == R.id.btn_medium_between_frames) {
            intent = new Intent(this, MediumLoadBetweenFramesWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.MEDIUM_BETWEEN_FRAMES;
            loadName = "帧间中负载";
        } else if (id == R.id.btn_heavy_between_frames) {
            intent = new Intent(this, HeavyLoadBetweenFramesWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.HEAVY_BETWEEN_FRAMES;
            loadName = "帧间高负载";
        } else if (id == R.id.btn_light_mixed) {
            intent = new Intent(this, LightMixedLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.LIGHT_MIXED;
            loadName = "混合轻负载";
        } else if (id == R.id.btn_medium_mixed) {
            intent = new Intent(this, MediumMixedLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.MEDIUM_MIXED;
            loadName = "混合中负载";
        } else if (id == R.id.btn_heavy_mixed) {
            intent = new Intent(this, HeavyMixedLoadWebViewActivity.class);
            loadType = com.example.loadconfig.LoadType.HEAVY_MIXED;
            loadName = "混合高负载";
        }
        
        if (intent != null) {
            Trace.beginSection("WebViewMainActivity_start" + loadName);
            Log.d(TAG, "启动" + loadName + "WebView朋友圈");
            intent.putExtra(EXTRA_LOAD_TYPE, loadType);
            startActivity(intent);
            Trace.endSection();
        }
    }
}
