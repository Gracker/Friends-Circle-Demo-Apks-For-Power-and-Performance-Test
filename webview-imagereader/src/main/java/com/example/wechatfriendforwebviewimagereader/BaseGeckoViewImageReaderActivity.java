package com.example.wechatfriendforwebviewimagereader;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ProgressBar;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.GeckoDisplay;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

/**
 * GeckoView ImageReader 版朋友圈基类Activity
 * 
 * 渲染模式：ImageReader 模拟
 * - 使用 TextureView 进行 GeckoView 渲染
 * - 在每次渲染时执行 getBitmap() 操作模拟 ImageReader 的开销
 * - 模拟从 ImageReader 获取 Image 并转换为 Bitmap 的过程
 * - 合成路径需要经过 App 的 UI Thread + RenderThread
 * - 类似淘宝天猫页面的渲染方式
 */
public abstract class BaseGeckoViewImageReaderActivity extends Activity {
    private static final String TAG = "GeckoViewImageReader";
    
    protected GeckoRuntime geckoRuntime;
    protected GeckoSession geckoSession;
    protected GeckoDisplay geckoDisplay;
    
    protected TextureView textureView;
    protected ProgressBar progressBar;
    protected int loadType;
    
    protected GestureDetector gestureDetector;
    
    protected boolean isFling = false;
    protected int flingFrameCount = 0;
    protected final int MAX_FLING_FRAMES = 200;
    
    private Surface surface;
    
    // 用于模拟 ImageReader 开销的计数器
    private int frameCount = 0;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("BaseGeckoViewImageReader_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geckoview_imagereader);
        
        loadType = getIntent().getIntExtra(GeckoViewMainActivity.EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT);
        
        initViews();
        initGeckoRuntime();
        setupTextureView();
        initGestureDetector();
        
        Trace.endSection();
    }
    
    private void initViews() {
        textureView = findViewById(R.id.texture_view);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void initGeckoRuntime() {
        geckoRuntime = GeckoViewApplication.getGeckoRuntime((GeckoViewApplication) getApplication());
        Log.d(TAG, "GeckoRuntime 已获取");
    }
    
    private void setupTextureView() {
        Trace.beginSection("BaseGeckoViewImageReader_setupTextureView");
        
        GeckoSessionSettings.Builder settingsBuilder = new GeckoSessionSettings.Builder();
        settingsBuilder.usePrivateMode(false);
        settingsBuilder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        
        geckoSession = new GeckoSession(settingsBuilder.build());
        
        geckoSession.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
            }
            
            @Override
            public void onPageStop(@NonNull GeckoSession session, boolean success) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loadFriendCircleData();
                });
            }
            
            @Override
            public void onProgressChange(@NonNull GeckoSession session, int progress) {
                runOnUiThread(() -> progressBar.setProgress(progress));
            }
        });
        
        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
                Log.d(TAG, "标题变更: " + title);
            }
        });
        
        geckoSession.open(geckoRuntime);
        geckoDisplay = geckoSession.acquireDisplay();
        
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                Log.d(TAG, "SurfaceTexture 可用: " + width + "x" + height);
                
                surface = new Surface(surfaceTexture);
                
                if (geckoDisplay != null) {
                    geckoDisplay.surfaceChanged(
                        new GeckoDisplay.SurfaceInfo.Builder(surface)
                            .size(width, height)
                            .build()
                    );
                    loadFriendCircleHtml();
                }
            }
            
            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                if (geckoDisplay != null && surface != null) {
                    geckoDisplay.surfaceChanged(
                        new GeckoDisplay.SurfaceInfo.Builder(surface)
                            .size(width, height)
                            .build()
                    );
                }
            }
            
            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                if (geckoDisplay != null) {
                    geckoDisplay.surfaceDestroyed();
                }
                if (surface != null) {
                    surface.release();
                    surface = null;
                }
                return true;
            }
            
            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // 模拟 ImageReader 的开销：每帧获取 Bitmap
                // 这模拟了 ImageReader.acquireLatestImage() -> toBitmap 的过程
                simulateImageReaderOverhead();
            }
        });
        
        setupTouchListener();
        Trace.endSection();
    }
    
    /**
     * 模拟 ImageReader 的额外开销
     * 通过 getBitmap() 获取当前帧的位图数据
     * 这会产生额外的内存拷贝和处理开销
     */
    private void simulateImageReaderOverhead() {
        frameCount++;
        
        // 每帧都模拟 ImageReader 的 acquireLatestImage 操作
        Trace.beginSection("ImageReader_acquireLatestImage_simulation");
        try {
            // 获取 Bitmap（模拟 ImageReader.acquireLatestImage + Image.toBitmap）
            Bitmap bitmap = textureView.getBitmap();
            if (bitmap != null) {
                // 模拟对 Bitmap 的处理（例如拷贝到另一个 Buffer）
                // 这里不需要实际显示，只是产生开销
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                
                // 每 100 帧记录一次日志
                if (frameCount % 100 == 0) {
                    Log.d(TAG, "ImageReader 模拟: 已处理 " + frameCount + " 帧, 尺寸: " + width + "x" + height);
                }
                
                // 回收 Bitmap 释放内存
                bitmap.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "模拟 ImageReader 开销时出错", e);
        } finally {
            Trace.endSection();
        }
    }
    
    private void initGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                isFling = true;
                flingFrameCount = 0;
                handleFling(velocityX, velocityY);
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(distanceY) > 10) {
                    try { Thread.sleep(3); } catch (InterruptedException e) {}
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
    }
    
    protected void handleFling(float velocityX, float velocityY) {
        final long startTime = SystemClock.elapsedRealtime();
        isFling = true;
        flingFrameCount = 0;
        executeFlingLoad();
        
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable flingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFling && flingFrameCount < MAX_FLING_FRAMES) {
                    flingFrameCount++;
                    executeFlingLoad();
                    handler.postDelayed(this, 16);
                } else {
                    isFling = false;
                }
            }
        };
        handler.postDelayed(flingRunnable, 16);
    }
    
    protected abstract void executeFlingLoad();
    
    private void loadFriendCircleHtml() {
        geckoSession.loadUri("resource://android/assets/friend_circle.html");
    }
    
    protected void loadFriendCircleData() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                String jsonData = GeckoViewDataCenter.getInstance().getFriendCircleJsonData(loadType);
                if (geckoSession != null && jsonData != null) {
                    String jsCode = "javascript:loadFriendCircleData(" + jsonData + ")";
                    geckoSession.loadUri(jsCode);
                }
                performLoadTask();
            } catch (Exception e) {
                Log.e(TAG, "加载朋友圈数据失败", e);
            }
        }, 100);
    }
    
    protected abstract void performLoadTask();
    
    private void setupTouchListener() {
        if (textureView != null) {
            textureView.setOnTouchListener((v, event) -> {
                if (geckoSession != null) {
                    geckoSession.getPanZoomController().onTouchEvent(event);
                }
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }
    }
    
    @Override
    public void onBackPressed() {
        // 直接关闭 Activity 返回主界面
        finish();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (geckoSession != null) {
            geckoSession.setActive(false);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (geckoSession != null) {
            geckoSession.setActive(true);
        }
    }
    
    @Override
    protected void onDestroy() {
        if (geckoDisplay != null) {
            geckoDisplay.surfaceDestroyed();
            geckoSession.releaseDisplay(geckoDisplay);
            geckoDisplay = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (geckoSession != null) {
            geckoSession.close();
            geckoSession = null;
        }
        super.onDestroy();
    }
}
