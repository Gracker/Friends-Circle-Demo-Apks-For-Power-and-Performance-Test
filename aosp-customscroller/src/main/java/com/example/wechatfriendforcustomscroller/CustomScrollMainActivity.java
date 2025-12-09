package com.example.wechatfriendforcustomscroller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.loadconfig.LoadType;
import com.example.wechatfriendforcustomscroller.databinding.ActivityPerformanceMainBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Main activity for selecting different load profiles.
 * Maintains consistent UI with the AOSP version.
 */
@AndroidEntryPoint
public class CustomScrollMainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityPerformanceMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerformanceMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
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
        int loadType;
        int id = v.getId();
        
        if (id == binding.btnMinimalLoad.getId()) {
            loadType = LoadType.MINIMAL;
        } else if (id == binding.btnLightLoad.getId()) {
            loadType = LoadType.LIGHT;
        } else if (id == binding.btnMediumLoad.getId()) {
            loadType = LoadType.MEDIUM;
        } else if (id == binding.btnHeavyLoad.getId()) {
            loadType = LoadType.HEAVY;
        } else if (id == binding.btnLightBetweenFrames.getId()) {
            loadType = LoadType.LIGHT_BETWEEN_FRAMES;
        } else if (id == binding.btnMediumBetweenFrames.getId()) {
            loadType = LoadType.MEDIUM_BETWEEN_FRAMES;
        } else if (id == binding.btnHeavyBetweenFrames.getId()) {
            loadType = LoadType.HEAVY_BETWEEN_FRAMES;
        } else if (id == binding.btnLightMixed.getId()) {
            loadType = LoadType.LIGHT_MIXED;
        } else if (id == binding.btnMediumMixed.getId()) {
            loadType = LoadType.MEDIUM_MIXED;
        } else if (id == binding.btnHeavyMixed.getId()) {
            loadType = LoadType.HEAVY_MIXED;
        } else if (id == binding.btnLongFrame.getId()) {
            loadType = LoadType.LONG_FRAME;
        } else {
            Toast.makeText(this, R.string.description, Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, CustomScrollFeedActivity.class);
        intent.putExtra(CustomScrollFeedActivity.EXTRA_LOAD_TYPE, loadType);
        startActivity(intent);
    }
}
