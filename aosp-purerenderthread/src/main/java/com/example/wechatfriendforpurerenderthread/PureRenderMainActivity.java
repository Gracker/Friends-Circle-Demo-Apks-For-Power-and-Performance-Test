package com.example.wechatfriendforpurerenderthread;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wechatfriendforpurerenderthread.databinding.ActivityMainBinding;

public class PureRenderMainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_PureRenderThread_NoActionBar);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupAppInfo();

        binding.btnMinimalLoad.setOnClickListener(this);
        binding.btnLightLoad.setOnClickListener(this);
        binding.btnMediumLoad.setOnClickListener(this);
        binding.btnHeavyLoad.setOnClickListener(this);
        binding.btnLightBetweenFrames.setOnClickListener(this);
        binding.btnMediumBetweenFrames.setOnClickListener(this);
        binding.btnHeavyBetweenFrames.setOnClickListener(this);
        binding.btnLightMixed.setOnClickListener(this);
        binding.btnMediumMixed.setOnClickListener(this);
        binding.btnHeavyMixed.setOnClickListener(this);
        binding.btnLongFrame.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Class<?> targetActivity = null;
        int id = v.getId();

        if (id == R.id.btn_minimal_load) targetActivity = MinimalLoadActivity.class;
        else if (id == R.id.btn_light_load) targetActivity = LightLoadActivity.class;
        else if (id == R.id.btn_medium_load) targetActivity = MediumLoadActivity.class;
        else if (id == R.id.btn_heavy_load) targetActivity = HeavyLoadActivity.class;
        else if (id == R.id.btn_light_between_frames) targetActivity = LightBetweenFramesActivity.class;
        else if (id == R.id.btn_medium_between_frames) targetActivity = MediumBetweenFramesActivity.class;
        else if (id == R.id.btn_heavy_between_frames) targetActivity = HeavyBetweenFramesActivity.class;
        else if (id == R.id.btn_light_mixed) targetActivity = LightMixedActivity.class;
        else if (id == R.id.btn_medium_mixed) targetActivity = MediumMixedActivity.class;
        else if (id == R.id.btn_heavy_mixed) targetActivity = HeavyMixedActivity.class;
        else if (id == R.id.btn_long_frame) targetActivity = LongFrameActivity.class;

        if (targetActivity != null) {
            startActivity(new Intent(this, targetActivity));
        }
    }

    private void setupAppInfo() {
        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvFeature = findViewById(R.id.tv_app_feature);
        TextView tvPackageName = findViewById(R.id.tv_package_name);

        if (tvAppName != null) {
            tvAppName.setText(getString(R.string.app_name));
        }
        if (tvFeature != null) {
            tvFeature.setText(getString(R.string.title_main));
        }
        if (tvPackageName != null) {
            tvPackageName.setText(getPackageName());
        }
    }
}


