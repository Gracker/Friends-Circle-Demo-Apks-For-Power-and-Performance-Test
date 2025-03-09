package com.example.wechatfriendforperformance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Trace;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 性能测试主Activity，用于选择不同负载级别的测试
 */
public class PerformanceMainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("PerformanceMainActivity_onCreate");
        super.onCreate(savedInstanceState);
        // 应用无ActionBar的主题
        setTheme(R.style.Theme_HighPerformanceFriendsCircle_NoActionBar);
        setContentView(R.layout.activity_performance_main);

        // 初始化按钮
        Button btnLightLoad = findViewById(R.id.btn_light_load);
        Button btnMediumLoad = findViewById(R.id.btn_medium_load);
        Button btnHeavyLoad = findViewById(R.id.btn_heavy_load);

        // 设置点击监听器
        btnLightLoad.setOnClickListener(this);
        btnMediumLoad.setOnClickListener(this);
        btnHeavyLoad.setOnClickListener(this);
        Trace.endSection();
    }

    @Override
    protected void onResume() {
        Trace.beginSection("PerformanceMainActivity_onResume");
        super.onResume();
        Trace.endSection();
    }

    @Override
    public void onClick(View v) {
        Trace.beginSection("PerformanceMainActivity_onClick");
        int id = v.getId();
        
        if (id == R.id.btn_light_load) {
            // 启动轻负载Activity
            Trace.beginSection("StartLightLoadActivity");
            startActivity(new Intent(this, LightLoadActivity.class));
            Trace.endSection();
        } else if (id == R.id.btn_medium_load) {
            // 启动中等负载Activity
            Trace.beginSection("StartMediumLoadActivity");
            startActivity(new Intent(this, MediumLoadActivity.class));
            Trace.endSection();
        } else if (id == R.id.btn_heavy_load) {
            // 启动高负载Activity
            Trace.beginSection("StartHeavyLoadActivity");
            startActivity(new Intent(this, HeavyLoadActivity.class));
            Trace.endSection();
        }
        Trace.endSection();
    }
} 