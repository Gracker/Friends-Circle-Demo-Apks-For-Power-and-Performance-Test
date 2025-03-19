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
    private Button btnLightLoad;
    private Button btnMediumLoad; 
    private Button btnHeavyLoad;
    
    // 定义常量，用于Intent传递负载类型
    public static final String EXTRA_LOAD_TYPE = "load_type";
    
    // WebView负载类型常量
    public static final int LOAD_TYPE_LIGHT = 1;
    public static final int LOAD_TYPE_MEDIUM = 2;
    public static final int LOAD_TYPE_HEAVY = 3;

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
        
        btnLightLoad = findViewById(R.id.btn_light_load);
        btnMediumLoad = findViewById(R.id.btn_medium_load);
        btnHeavyLoad = findViewById(R.id.btn_heavy_load);
        
        Trace.endSection();
    }
    
    /**
     * 设置点击监听器
     */
    private void setClickListeners() {
        Trace.beginSection("WebViewMainActivity_setClickListeners");
        
        btnLightLoad.setOnClickListener(this);
        btnMediumLoad.setOnClickListener(this);
        btnHeavyLoad.setOnClickListener(this);
        
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
        
        if (id == R.id.btn_light_load) {
            Trace.beginSection("WebViewMainActivity_startLightLoad");
            Log.d(TAG, "启动轻负载WebView朋友圈");
            
            // 启动轻负载Activity
            Intent intent = new Intent(this, LightLoadWebViewActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, LOAD_TYPE_LIGHT);
            startActivity(intent);
            
            Trace.endSection();
        } else if (id == R.id.btn_medium_load) {
            Trace.beginSection("WebViewMainActivity_startMediumLoad");
            Log.d(TAG, "启动中负载WebView朋友圈");
            
            // 启动中负载Activity
            Intent intent = new Intent(this, MediumLoadWebViewActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, LOAD_TYPE_MEDIUM);
            startActivity(intent);
            
            Trace.endSection();
        } else if (id == R.id.btn_heavy_load) {
            Trace.beginSection("WebViewMainActivity_startHeavyLoad");
            Log.d(TAG, "启动重负载WebView朋友圈");
            
            // 启动重负载Activity
            Intent intent = new Intent(this, HeavyLoadWebViewActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, LOAD_TYPE_HEAVY);
            startActivity(intent);
            
            Trace.endSection();
        }
    }
} 