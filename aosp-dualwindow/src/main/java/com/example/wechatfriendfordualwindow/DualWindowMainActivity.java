package com.example.wechatfriendfordualwindow;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendfordualwindow.databinding.ActivityMainBinding;

public class DualWindowMainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private static final int OVERLAY_PERMISSION_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Request overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
        }
        
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
        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            return;
        }
        
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
}

