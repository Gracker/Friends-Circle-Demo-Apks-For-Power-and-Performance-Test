package com.example.wechatfriendforglmap;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.loadconfig.LoadType;

/**
 * Base Activity for OpenGL map demo with native UI components.
 */
public abstract class BaseGLMapActivity extends AppCompatActivity {

    protected GLMapView glMapView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl_map);

        glMapView = findViewById(R.id.gl_map_view);
        glMapView.setLoadType(getLoadType());

        // Set up top navigation bar
        TextView titleText = findViewById(R.id.title_text);
        titleText.setText(LoadType.toLabel(getLoadType()));

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Set up bottom controls
        setupBottomControls();
    }

    protected abstract @LoadType.Type int getLoadType();

    private void setupBottomControls() {
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> glMapView.zoomIn());

        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> glMapView.zoomOut());

        findViewById(R.id.btn_location).setOnClickListener(v -> glMapView.resetView());

        findViewById(R.id.btn_layers).setOnClickListener(v -> glMapView.toggleMarkers());
    }

    @Override
    protected void onResume() {
        super.onResume();
        glMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glMapView.onPause();
    }
}
