package com.example.wechatfriendforsurfacemap;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base Activity for map demos with native top/bottom controls.
 */
public abstract class BaseMapActivity extends AppCompatActivity {
    
    protected MapSurfaceView mapSurfaceView;
    protected int loadType = LoadProfile.LOAD_TYPE_MINIMAL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
        loadType = getLoadType();
        
        // Initialize map surface
        mapSurfaceView = findViewById(R.id.map_surface_view);
        mapSurfaceView.setLoadType(loadType);
        
        // Set up top navigation bar
        TextView titleText = findViewById(R.id.title_text);
        titleText.setText(LoadProfile.toLabel(loadType));
        
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Set up bottom controls
        setupBottomControls();
    }
    
    protected abstract @LoadProfile.LoadType int getLoadType();
    
    private void setupBottomControls() {
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> {
            // Zoom in functionality (placeholder)
        });
        
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> {
            // Zoom out functionality (placeholder)
        });
        
        findViewById(R.id.btn_location).setOnClickListener(v -> {
            // Current location functionality (placeholder)
        });
        
        findViewById(R.id.btn_layers).setOnClickListener(v -> {
            // Layers functionality (placeholder)
        });
    }
}


