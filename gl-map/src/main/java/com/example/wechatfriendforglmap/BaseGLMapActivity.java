package com.example.wechatfriendforglmap;

import android.os.Bundle;

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
    }

    protected abstract @LoadType.Type int getLoadType();

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

