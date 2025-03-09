package com.example.wechatfriendforperformance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendforperformance.adapters.PerformanceFriendCircleAdapter;

/**
 * Performance test main Activity, used to select different load levels for testing
 */
public class PerformanceMainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnLightLoad, btnMediumLoad, btnHeavyLoad;
    
    // 定义常量，用于Intent传递负载类型
    public static final String EXTRA_LOAD_TYPE = "load_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("PerformanceMainActivity_onCreate");
        super.onCreate(savedInstanceState);
        // Apply theme without ActionBar
        setTheme(R.style.Theme_HighPerformanceFriendsCircle_NoActionBar);
        setContentView(R.layout.activity_performance_main);
        
        // Initialize buttons
        btnLightLoad = findViewById(R.id.btn_light_load);
        btnMediumLoad = findViewById(R.id.btn_medium_load);
        btnHeavyLoad = findViewById(R.id.btn_heavy_load);
        
        // Set click listeners
        btnLightLoad.setOnClickListener(this);
        btnMediumLoad.setOnClickListener(this);
        btnHeavyLoad.setOnClickListener(this);
        
        Trace.endSection();
    }

    @Override
    protected void onResume() {
        Trace.beginSection("PerformanceMainActivity_onResume");
        super.onResume();
        // 确保数据中心的缓存被清空
        PerformanceDataCenter.getInstance().clearCachedData();
        Trace.endSection();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        // 先清除所有缓存的数据，确保每次都重新生成
        PerformanceDataCenter.getInstance().clearCachedData();
        
        if (id == R.id.btn_light_load) {
            Trace.beginSection("PerformanceMainActivity_startLightLoad");
            // Start LightLoad Activity
            Intent intent = new Intent(this, LightLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT);
            Log.d(TAG, "启动轻负载测试");
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_medium_load) {
            Trace.beginSection("PerformanceMainActivity_startMediumLoad");
            // Start MediumLoad Activity
            Intent intent = new Intent(this, MediumLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM);
            Log.d(TAG, "启动中负载测试");
            startActivity(intent);
            Trace.endSection();
        } else if (id == R.id.btn_heavy_load) {
            Trace.beginSection("PerformanceMainActivity_startHeavyLoad");
            // Start HeavyLoad Activity
            Intent intent = new Intent(this, HeavyLoadActivity.class);
            // 传递负载类型参数
            intent.putExtra(EXTRA_LOAD_TYPE, PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY);
            Log.d(TAG, "启动高负载测试");
            startActivity(intent);
            Trace.endSection();
        }
    }
} 