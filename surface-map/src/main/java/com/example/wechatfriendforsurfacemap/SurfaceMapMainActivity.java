package com.example.wechatfriendforsurfacemap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendforsurfacemap.databinding.ActivityMainBinding;

/**
 * Main activity for selecting different load profiles for SurfaceView map testing.
 */
public class SurfaceMapMainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_SurfaceMap_NoActionBar);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupAppInfo();
        
        // Minimal load
        binding.btnMinimalLoad.setOnClickListener(this);
        
        // In-frame loads
        binding.btnLightLoad.setOnClickListener(this);
        binding.btnMediumLoad.setOnClickListener(this);
        binding.btnHeavyLoad.setOnClickListener(this);
        
        // Between-frame loads
        binding.btnLightBetweenFrames.setOnClickListener(this);
        binding.btnMediumBetweenFrames.setOnClickListener(this);
        binding.btnHeavyBetweenFrames.setOnClickListener(this);
        
        // Mixed loads
        binding.btnLightMixed.setOnClickListener(this);
        binding.btnMediumMixed.setOnClickListener(this);
        binding.btnHeavyMixed.setOnClickListener(this);
        
        // Long frame load
        binding.btnLongFrame.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Class<?> targetActivity = null;
        int id = v.getId();
        
        if (id == R.id.btn_minimal_load) {
            targetActivity = MinimalLoadMapActivity.class;
        } else if (id == R.id.btn_light_load) {
            targetActivity = LightLoadMapActivity.class;
        } else if (id == R.id.btn_medium_load) {
            targetActivity = MediumLoadMapActivity.class;
        } else if (id == R.id.btn_heavy_load) {
            targetActivity = HeavyLoadMapActivity.class;
        } else if (id == R.id.btn_light_between_frames) {
            targetActivity = LightBetweenFramesMapActivity.class;
        } else if (id == R.id.btn_medium_between_frames) {
            targetActivity = MediumBetweenFramesMapActivity.class;
        } else if (id == R.id.btn_heavy_between_frames) {
            targetActivity = HeavyBetweenFramesMapActivity.class;
        } else if (id == R.id.btn_light_mixed) {
            targetActivity = LightMixedMapActivity.class;
        } else if (id == R.id.btn_medium_mixed) {
            targetActivity = MediumMixedMapActivity.class;
        } else if (id == R.id.btn_heavy_mixed) {
            targetActivity = HeavyMixedMapActivity.class;
        } else if (id == R.id.btn_long_frame) {
            targetActivity = LongFrameMapActivity.class;
        }
        
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


