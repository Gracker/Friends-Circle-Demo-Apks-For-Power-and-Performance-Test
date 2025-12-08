package com.example.wechatfriendfordualwindow;

import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base Activity that creates dual windows for testing.
 * Window 1: Main activity window with scrollable list
 * Window 2: Overlay window that continuously animates
 * 
 * In systrace, you will see:
 * - 2 doFrame callbacks per vsync
 * - 2 RenderThread drawFrame per vsync
 */
public abstract class BaseDualWindowActivity extends AppCompatActivity {
    
    protected MainWindowView mainWindowView;
    protected SecondWindowView secondWindowView;
    protected WindowManager windowManager;
    protected boolean isSecondWindowAdded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_window);
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Setup main window view
        mainWindowView = findViewById(R.id.main_window_view);
        mainWindowView.setLoadType(getLoadType());
        
        // Create second window view
        secondWindowView = new SecondWindowView(this);
        secondWindowView.setLoadType(getLoadType());
        
        // Check overlay permission and add second window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                addSecondWindow();
            } else {
                Toast.makeText(this, "Please grant overlay permission for dual-window demo", Toast.LENGTH_LONG).show();
            }
        } else {
            addSecondWindow();
        }
    }
    
    private void addSecondWindow() {
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                400,
                600,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 100;
        
        try {
            windowManager.addView(secondWindowView, params);
            isSecondWindowAdded = true;
            secondWindowView.startAnimation();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to add second window: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    protected abstract @LoadProfile.LoadType int getLoadType();

    @Override
    protected void onResume() {
        super.onResume();
        if (isSecondWindowAdded) {
            secondWindowView.startAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSecondWindowAdded) {
            secondWindowView.stopAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        if (isSecondWindowAdded && secondWindowView != null) {
            secondWindowView.stopAnimation();
            try {
                windowManager.removeView(secondWindowView);
            } catch (Exception e) {
                // View might already be removed
            }
        }
        super.onDestroy();
    }
}

