package com.example.wechatfriendforrenderstress;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendforrenderstress.databinding.ActivityPerformanceMainBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 主界面，保持与AOSP版本一致的UI，用于选择不同的负载档位。
 */
@AndroidEntryPoint
public class CustomScrollMainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityPerformanceMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerformanceMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnLightLoad.setOnClickListener(this);
        binding.btnMediumLoad.setOnClickListener(this);
        binding.btnHeavyLoad.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int loadType;
        int id = v.getId();
        if (id == binding.btnLightLoad.getId()) {
            loadType = LoadProfile.LOAD_TYPE_LIGHT;
        } else if (id == binding.btnMediumLoad.getId()) {
            loadType = LoadProfile.LOAD_TYPE_MEDIUM;
        } else if (id == binding.btnHeavyLoad.getId()) {
            loadType = LoadProfile.LOAD_TYPE_HEAVY;
        } else {
            Toast.makeText(this, R.string.description, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CustomScrollFeedActivity.class);
        intent.putExtra(CustomScrollFeedActivity.EXTRA_LOAD_TYPE, loadType);
        startActivity(intent);
    }
}

