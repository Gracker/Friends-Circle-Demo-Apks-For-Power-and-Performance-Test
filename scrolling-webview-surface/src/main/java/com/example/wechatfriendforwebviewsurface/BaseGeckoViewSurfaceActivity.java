package com.example.wechatfriendforwebviewsurface;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
 * GeckoView SurfaceView 版朋友圈基类Activity
 * 
 * 渲染模式：SurfaceView
 * - 使用 SurfaceView 作为渲染载体
 * - GeckoView 内容直接渲染到 SurfaceView 的 Surface
 * - Surface 直接提交到 SurfaceFlinger，绕过 App 的 RenderThread
 * - 类似 Chrome App 的渲染方式
 */
public abstract class BaseGeckoViewSurfaceActivity extends Activity {
    private static final String TAG = "GeckoViewSurface";

    protected GeckoRuntime geckoRuntime;
    protected GeckoSession geckoSession;
    protected GeckoDisplay geckoDisplay;

    protected SurfaceView surfaceView;
    protected ProgressBar progressBar;
    protected int loadType;

    protected GestureDetector gestureDetector;

    protected boolean isFling = false;
    protected int flingFrameCount = 0;
    protected final int MAX_FLING_FRAMES = 200;

    private boolean surfaceReady = false;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;

    // 生命周期状态标志
    protected volatile boolean isActivityActive = true;
    // 主 Handler 用于所有延迟任务
    protected Handler mHandler;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("BaseGeckoViewSurface_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geckoview_surface);

        // 初始化 Handler
        mHandler = new Handler(Looper.getMainLooper());

        // 获取传入的负载类型（添加空指针保护）
        android.content.Intent intent = getIntent();
        if (intent != null) {
            loadType = intent.getIntExtra(GeckoViewMainActivity.EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT);
        } else {
            loadType = com.example.loadconfig.LoadType.LIGHT;
        }

        initViews();
        initGeckoRuntime();
        setupSurfaceView();
        initGestureDetector();

        Trace.endSection();
    }

    private void initViews() {
        Trace.beginSection("BaseGeckoViewSurface_initViews");
        surfaceView = findViewById(R.id.surface_view);
        progressBar = findViewById(R.id.progress_bar);
        Trace.endSection();
    }

    private void initGeckoRuntime() {
        Trace.beginSection("BaseGeckoViewSurface_initGeckoRuntime");
        geckoRuntime = GeckoViewApplication.getGeckoRuntime((GeckoViewApplication) getApplication());
        Log.d(TAG, "GeckoRuntime 已获取");
        Trace.endSection();
    }

    private void setupSurfaceView() {
        Trace.beginSection("BaseGeckoViewSurface_setupSurfaceView");

        GeckoSessionSettings.Builder settingsBuilder = new GeckoSessionSettings.Builder();
        settingsBuilder.usePrivateMode(false);
        settingsBuilder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);

        geckoSession = new GeckoSession(settingsBuilder.build());

        geckoSession.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                Log.d(TAG, "页面开始加载: " + url);
                runOnUiThread(() -> {
                    if (!isActivityActive) return;
                    progressBar.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onPageStop(@NonNull GeckoSession session, boolean success) {
                Log.d(TAG, "页面加载完成, 成功: " + success);
                runOnUiThread(() -> {
                    if (!isActivityActive) return;
                    progressBar.setVisibility(View.GONE);
                    loadFriendCircleData();
                });
            }

            @Override
            public void onProgressChange(@NonNull GeckoSession session, int progress) {
                runOnUiThread(() -> {
                    if (!isActivityActive) return;
                    progressBar.setProgress(progress);
                });
            }
        });

        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
                Log.d(TAG, "标题变更: " + title);
            }
        });

        // 设置导航委托来处理返回
        geckoSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
                Log.d(TAG, "canGoBack: " + canGoBack);
            }

            @Override
            public void onCanGoForward(@NonNull GeckoSession session, boolean canGoForward) {
                Log.d(TAG, "canGoForward: " + canGoForward);
            }
        });

        geckoSession.open(geckoRuntime);
        geckoDisplay = geckoSession.acquireDisplay();

        Log.d(TAG, "GeckoDisplay 已获取，准备绑定到 SurfaceView");

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.d(TAG, "Surface 已创建");
                surfaceReady = true;
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "Surface 变更: " + width + "x" + height + ", format=" + format);
                surfaceWidth = width;
                surfaceHeight = height;

                if (geckoDisplay != null) {
                    geckoDisplay.surfaceChanged(
                        new GeckoDisplay.SurfaceInfo.Builder(holder.getSurface())
                            .size(width, height)
                            .build()
                    );

                    Log.d(TAG, "GeckoDisplay 已绑定到 SurfaceView Surface");
                    loadFriendCircleHtml();
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                Log.d(TAG, "Surface 已销毁");
                surfaceReady = false;

                if (geckoDisplay != null) {
                    geckoDisplay.surfaceDestroyed();
                    Log.d(TAG, "GeckoDisplay Surface 已释放");
                }
            }
        });

        setupTouchListener();

        Trace.endSection();
    }

    private void initGestureDetector() {
        Log.d(TAG, "初始化手势检测器");

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distance = 0;
                if (e1 != null && e2 != null) {
                    distance = (float) Math.sqrt(Math.pow(e2.getX() - e1.getX(), 2) + Math.pow(e2.getY() - e1.getY(), 2));
                }

                Log.d(TAG, "onFling 检测到! 速度X:" + velocityX + " 速度Y:" + velocityY);

                isFling = true;
                flingFrameCount = 0;
                handleFling(velocityX, velocityY);

                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(distanceY) > 10) {
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "滚动等待被中断", e);
                    }
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

        Runnable flingRunnable = new Runnable() {
            @Override
            public void run() {
                // 检查 Activity 是否仍然活跃
                if (!isActivityActive) {
                    isFling = false;
                    return;
                }
                if (isFling && flingFrameCount < MAX_FLING_FRAMES) {
                    flingFrameCount++;
                    executeFlingLoad();
                    if (mHandler != null && isActivityActive) {
                        mHandler.postDelayed(this, 16);
                    }
                } else {
                    isFling = false;
                    long duration = SystemClock.elapsedRealtime() - startTime;
                    Log.d(TAG, "fling处理结束, 执行了 " + flingFrameCount + " 帧, 总用时: " + duration + "ms");
                }
            }
        };

        if (mHandler != null) {
            mHandler.postDelayed(flingRunnable, 16);
        }
    }

    protected abstract void executeFlingLoad();

    private void loadFriendCircleHtml() {
        Trace.beginSection("BaseGeckoViewSurface_loadFriendCircleHtml");
        geckoSession.loadUri("resource://android/assets/friend_circle.html");
        Log.d(TAG, "开始加载朋友圈HTML");
        Trace.endSection();
    }

    protected void loadFriendCircleData() {
        Trace.beginSection("BaseGeckoViewSurface_loadFriendCircleData");

        if (mHandler != null) {
            mHandler.postDelayed(() -> {
                // 检查 Activity 是否仍然活跃
                if (!isActivityActive) {
                    return;
                }
                try {
                    String jsonData = GeckoViewDataCenter.getInstance().getFriendCircleJsonData(loadType);

                    if (geckoSession != null && jsonData != null && isActivityActive) {
                        // 使用 loadUri 注入数据的方式
                        String jsCode = "javascript:loadFriendCircleData(" + jsonData + ")";
                        geckoSession.loadUri(jsCode);
                        Log.d(TAG, "朋友圈数据已发送到 GeckoView");
                    }

                    performLoadTask();
                } catch (Exception e) {
                    Log.e(TAG, "加载朋友圈数据失败", e);
                }
            }, 100);
        }

        Trace.endSection();
    }

    protected abstract void performLoadTask();

    private void setupTouchListener() {
        if (surfaceView != null) {
            surfaceView.setOnTouchListener((v, event) -> {
                if (geckoSession != null) {
                    geckoSession.getPanZoomController().onTouchEvent(event);
                }
                gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        try { Thread.sleep(2); } catch (InterruptedException e) {}
                        break;
                    case MotionEvent.ACTION_MOVE:
                        try { Thread.sleep(5); } catch (InterruptedException e) {}
                        break;
                }

                return true;
            });

            Log.d(TAG, "SurfaceView 触摸监听器已设置");
        }
    }

    /**
     * 执行 JavaScript (通过 loadUri javascript: 协议)
     */
    public void evaluateJavascript(String script, Runnable callback) {
        if (!isActivityActive || geckoSession == null) {
            return;
        }
        Log.d(TAG, "准备执行JavaScript: " + (script.length() > 100 ? script.substring(0, 100) + "..." : script));
        try {
            // GeckoView 新版本使用 loadUri 执行 JavaScript
            geckoSession.loadUri("javascript:" + script);
            if (callback != null && mHandler != null && isActivityActive) {
                mHandler.postDelayed(() -> {
                    if (isActivityActive) {
                        callback.run();
                    }
                }, 50);
            }
        } catch (Exception e) {
            Log.e(TAG, "执行JavaScript时出错", e);
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
        // 标记 Activity 不再活跃
        isActivityActive = false;
        isFling = false;
        if (geckoSession != null) {
            geckoSession.setActive(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
        if (geckoSession != null) {
            geckoSession.setActive(true);
        }
    }

    @Override
    protected void onDestroy() {
        // 首先标记 Activity 已销毁
        isActivityActive = false;
        isFling = false;

        // 清理 Handler
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        if (geckoDisplay != null) {
            geckoDisplay.surfaceDestroyed();
            geckoSession.releaseDisplay(geckoDisplay);
            geckoDisplay = null;
        }

        if (geckoSession != null) {
            geckoSession.close();
            geckoSession = null;
        }

        super.onDestroy();
    }
}
