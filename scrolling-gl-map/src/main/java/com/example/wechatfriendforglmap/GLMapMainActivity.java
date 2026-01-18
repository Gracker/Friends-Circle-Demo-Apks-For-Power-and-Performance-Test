package com.example.wechatfriendforglmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendforglmap.databinding.ActivityMainBinding;

public class GLMapMainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_GLMap_NoActionBar);
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

        checkForDirectActivityLaunch();
    }

    private void checkForDirectActivityLaunch() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("activity_type")) {
            String activityType = intent.getStringExtra("activity_type");
            android.util.Log.e("GLMapMainActivity", "Found activity_type extra: " + activityType);
            Class<?> targetActivity = null;

            if ("minimal".equals(activityType))
                targetActivity = MinimalLoadActivity.class;
            else if ("light".equals(activityType))
                targetActivity = LightLoadActivity.class;
            else if ("medium".equals(activityType))
                targetActivity = MediumLoadActivity.class;
            else if ("heavy".equals(activityType))
                targetActivity = HeavyLoadActivity.class;
            else if ("light_between_frames".equals(activityType))
                targetActivity = LightBetweenFramesActivity.class;
            else if ("medium_between_frames".equals(activityType))
                targetActivity = MediumBetweenFramesActivity.class;
            else if ("heavy_between_frames".equals(activityType))
                targetActivity = HeavyBetweenFramesActivity.class;
            else if ("light_mixed".equals(activityType))
                targetActivity = LightMixedActivity.class;
            else if ("medium_mixed".equals(activityType))
                targetActivity = MediumMixedActivity.class;
            else if ("heavy_mixed".equals(activityType))
                targetActivity = HeavyMixedActivity.class;
            else if ("long_frame".equals(activityType))
                targetActivity = LongFrameActivity.class;

            if (targetActivity != null) {
                startActivity(new Intent(this, targetActivity));
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        Class<?> targetActivity = null;
        int id = v.getId();

        if (id == R.id.btn_minimal_load)
            targetActivity = MinimalLoadActivity.class;
        else if (id == R.id.btn_light_load)
            targetActivity = LightLoadActivity.class;
        else if (id == R.id.btn_medium_load)
            targetActivity = MediumLoadActivity.class;
        else if (id == R.id.btn_heavy_load)
            targetActivity = HeavyLoadActivity.class;
        else if (id == R.id.btn_light_between_frames)
            targetActivity = LightBetweenFramesActivity.class;
        else if (id == R.id.btn_medium_between_frames)
            targetActivity = MediumBetweenFramesActivity.class;
        else if (id == R.id.btn_heavy_between_frames)
            targetActivity = HeavyBetweenFramesActivity.class;
        else if (id == R.id.btn_light_mixed)
            targetActivity = LightMixedActivity.class;
        else if (id == R.id.btn_medium_mixed)
            targetActivity = MediumMixedActivity.class;
        else if (id == R.id.btn_heavy_mixed)
            targetActivity = HeavyMixedActivity.class;
        else if (id == R.id.btn_long_frame)
            targetActivity = LongFrameActivity.class;

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
