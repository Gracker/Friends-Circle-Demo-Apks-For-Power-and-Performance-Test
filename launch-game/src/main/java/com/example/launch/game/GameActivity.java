package com.example.launch.game;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Canvas;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.launch.game.BuildConfig;

/**
 * 游戏启动性能测试 Activity
 * 实现专业的游戏加载流程：Logo -> Video -> 资源加载 -> 开始游戏
 */
public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GameActivity";

    // 调试开关 - 使用Canvas 2D渲染而不是OpenGL
    private static final boolean USE_CANVAS_2D = false;

    private GameGLSurfaceView20Fixed gameSurfaceView20;
    private GameGLSurfaceView gameSurfaceView;
    private GameSimpleView simpleView; // Canvas 2D视图
    private VideoView videoView;
    private LinearLayout logoContainer;

    // GL Loading 文字覆盖层
    private FrameLayout glLoadingOverlay;
    private TextView glLoaderTitle;
    private TextView glLoaderSubtitle;
    private TextView glLoadingPercent;
    private TextView glLoadingStatus;
    private TextView glLoadingHint;
    private LinearLayout loadingOverlay;
    private LinearLayout startGameContainer;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private TextView loadingDetail;
    private TextView fpsCounter;
    private Button btnStartGame;
    private TextView loadStats;

    private Handler mainHandler;
    private boolean isGameReady = false;
    private long loadingStartTime;
    private GameEngine gameEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("GameActivity_onCreate");
        super.onCreate(savedInstanceState);

        // 根据开关选择不同的布局
        if (USE_CANVAS_2D) {
            setContentView(R.layout.activity_game_simple);
            Log.d(TAG, "Using Canvas 2D rendering");
        } else {
            setContentView(R.layout.activity_game);
            Log.d(TAG, "Using OpenGL ES rendering");
        }

        Log.d(TAG, "Starting game with load duration: " + BuildConfig.LOAD_DURATION_MS + "ms");

        initViews();
        initGameEngine();
        loadingStartTime = System.currentTimeMillis();

        // 开始游戏加载流程
        startGameLoadingFlow();

        Trace.endSection();
    }

    private void initViews() {
        videoView = findViewById(R.id.video_view);
        logoContainer = findViewById(R.id.logo_container);
        loadingOverlay = findViewById(R.id.loading_overlay);
        startGameContainer = findViewById(R.id.start_game_container);
        loadingProgress = findViewById(R.id.loading_progress);
        loadingText = findViewById(R.id.loading_text);
        loadingDetail = findViewById(R.id.loading_detail);
        fpsCounter = findViewById(R.id.fps_counter);
        btnStartGame = findViewById(R.id.btn_start_game);
        loadStats = findViewById(R.id.load_stats);

        // 根据渲染方式初始化视图
        if (USE_CANVAS_2D) {
            simpleView = findViewById(R.id.simple_view);
            simpleView.setVisibility(View.VISIBLE);
            simpleView.setLoadingState(false);
            Log.d(TAG, "GameSimpleView initialized");
        } else {
            // 初始化GL加载文字覆盖层
            glLoadingOverlay = findViewById(R.id.gl_loading_overlay);
            glLoaderTitle = findViewById(R.id.gl_loader_title);
            glLoaderSubtitle = findViewById(R.id.gl_loader_subtitle);
            glLoadingPercent = findViewById(R.id.gl_loading_percent);
            glLoadingStatus = findViewById(R.id.gl_loading_status);
            glLoadingHint = findViewById(R.id.gl_loading_hint);

            // 使用新的 OpenGL ES 2.0 视图
            // 注意：需要修改布局文件以使用 GameGLSurfaceView20
            // 暂时使用旧的视图作为占位符
            gameSurfaceView = findViewById(R.id.game_surface);
            gameSurfaceView.setVisibility(View.GONE);

            // 创建新的 OpenGL ES 2.0 视图
            gameSurfaceView20 = new GameGLSurfaceView20Fixed(this);
            // 添加到布局中
            FrameLayout parent = findViewById(R.id.game_surface_placeholder);
            if (parent != null) {
                parent.addView(gameSurfaceView20, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ));
            } else {
                // 如果没有占位符，替换原有的 game_surface
                ViewGroup viewGroup = (ViewGroup) gameSurfaceView.getParent();
                int index = viewGroup.indexOfChild(gameSurfaceView);
                viewGroup.removeView(gameSurfaceView);
                gameSurfaceView20 = new GameGLSurfaceView20Fixed(this);
                viewGroup.addView(gameSurfaceView20, index);
            }

            gameSurfaceView20.setVisibility(View.VISIBLE);
            gameSurfaceView20.setLoadingState(false);
            Log.d(TAG, "GameGLSurfaceView20 (OpenGL ES 2.0) initialized");
        }

        videoView.setVisibility(View.GONE);
        logoContainer.setVisibility(View.VISIBLE);
        loadingOverlay.setVisibility(View.GONE);
        startGameContainer.setVisibility(View.GONE);
        fpsCounter.setVisibility(View.GONE);

        Log.d(TAG, "Views initialized - Game View: VISIBLE, Logo: VISIBLE, Others: GONE");

        btnStartGame.setOnClickListener(v -> {
            Log.d(TAG, "Start Game button clicked");
            startGame();
        });
    }

    private void initGameEngine() {
        mainHandler = new Handler(Looper.getMainLooper());

        // 创建游戏引擎
        gameEngine = new GameEngine();

        // 设置渲染器
        gameEngine.setRenderer(new GameEngine.GameRenderer() {
            @Override
            public void onFrame(long frameCount, float deltaTime) {
                // 在这里处理每帧渲染
                if (gameSurfaceView20 != null && gameSurfaceView20.getVisibility() == View.VISIBLE) {
                    // 触发OpenGL渲染
                    gameSurfaceView20.requestRender();
                } else if (gameSurfaceView != null && gameSurfaceView.getVisibility() == View.VISIBLE) {
                    // 触发OpenGL渲染
                    gameSurfaceView.requestRender();
                }
            }

            @Override
            public void onStart() {
                Log.d(TAG, "Game renderer started");
            }

            @Override
            public void onStop() {
                Log.d(TAG, "Game renderer stopped");
            }

            @Override
            public Canvas getCanvas() {
                return null; // OpenGL不需要Canvas
            }

            @Override
            public void swapBuffers() {
                // OpenGL自动处理缓冲区交换
            }
        });

        // 设置加载监听器
        gameEngine.setLoadingListener(new GameEngine.LoadingListener() {
            @Override
            public void onLoadingProgress(int progress, String message) {
                updateLoadingProgress(message, progress);
            }

            @Override
            public void onLoadingComplete() {
                mainHandler.post(() -> {
                    onAssetsLoaded();
                });
            }
        });

        // 设置FPS监听器
        gameEngine.setFPSListener(fps -> {
            if (fpsCounter != null && fpsCounter.getVisibility() == View.VISIBLE) {
                fpsCounter.setText(String.format("FPS: %d", fps));
            }
        });
    }

    /**
     * 游戏加载流程
     */
    private void startGameLoadingFlow() {
        // Stage 1: 显示 Logo
        showLogo();
    }

    private void showLogo() {
        Log.d(TAG, "Stage 1: Showing logo");
        logoContainer.setVisibility(View.VISIBLE);

        // Logo 显示时间：固定显示2秒
        long logoDuration = 2000;

        mainHandler.postDelayed(() -> {
            // Stage 2: 所有版本都播放视频
            playVideoAndLoadAssets();
        }, logoDuration);
    }

    private void playVideoAndLoadAssets() {
        Log.d(TAG, "Stage 2: Playing video and loading assets");
        logoContainer.setVisibility(View.GONE);

        // 播放视频
        playStartupVideo();

        // 同时开始预加载基础资源
        new Thread(() -> {
            // 预加载一些基础资源
            preloadBasicAssets();
        }, "PreloadThread").start();

        // 视频监听器已在 playStartupVideo 中处理，这里不需要重复设置
    }

    private void preloadBasicAssets() {
        // 模拟预加载基础资源
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void playStartupVideo() {
        Log.i(TAG, "Playing startup video for 2 seconds...");
        videoView.setVisibility(View.VISIBLE);

        // 获取屏幕尺寸
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 设置视频全屏显示，铺满整个屏幕
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            screenWidth,
            screenHeight
        );
        videoView.setLayoutParams(params);

        // 尝试加载视频
        int videoResId = getResources().getIdentifier("video1", "raw", getPackageName());
        if (videoResId != 0) {
            String videoPath = "android.resource://" + getPackageName() + "/" + videoResId;
            videoView.setVideoURI(Uri.parse(videoPath));

            videoView.setOnPreparedListener(mp -> {
                Log.d(TAG, "Video prepared for playback");

                // 获取视频尺寸
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                Log.d(TAG, "Video dimensions: " + videoWidth + "x" + videoHeight);

                // 计算视频应该保持的宽高比
                float videoRatio = (float) videoWidth / videoHeight;
                float screenRatio = (float) screenWidth / screenHeight;

                Log.d(TAG, "Video ratio: " + videoRatio + ", Screen ratio: " + screenRatio);

                // 根据屏幕和视频的比例，计算合适的缩放
                if (videoRatio > screenRatio) {
                    // 视频更宽，以高度为准
                    int scaledWidth = (int) (screenHeight * videoRatio);
                    FrameLayout.LayoutParams scaledParams = new FrameLayout.LayoutParams(
                        scaledWidth,
                        screenHeight,
                        android.view.Gravity.CENTER
                    );
                    videoView.setLayoutParams(scaledParams);
                } else {
                    // 视频更高，以宽度为准，可能会裁切上下部分
                    int scaledHeight = (int) (screenWidth / videoRatio);
                    FrameLayout.LayoutParams scaledParams = new FrameLayout.LayoutParams(
                        screenWidth,
                        scaledHeight,
                        android.view.Gravity.CENTER
                    );
                    videoView.setLayoutParams(scaledParams);
                }

                // 开始播放
                mp.start();

                // 2秒后自动停止视频
                mainHandler.postDelayed(() -> {
                    if (mp.isPlaying()) {
                        mp.pause();
                        videoView.setVisibility(View.GONE);
                        Log.d(TAG, "Video stopped after 2 seconds");

                        // 继续资源加载流程
                        loadGameAssets();
                    }
                }, 2000);
            });

            videoView.setOnCompletionListener(mp -> {
                Log.d(TAG, "Video playback completed naturally");
                videoView.setVisibility(View.GONE);
                loadGameAssets();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "Video playback error: " + what + ", extra: " + extra);
                videoView.setVisibility(View.GONE);
                // 视频播放失败，直接进入资源加载
                loadGameAssets();
                return true;
            });
        } else {
            Log.w(TAG, "Video file video1 not found in res/raw");
            videoView.setVisibility(View.GONE);
            // 没有视频文件，直接进入资源加载
            loadGameAssets();
        }
    }

    private void loadGameAssets() {
        Log.d(TAG, "Stage 3: Loading game assets");

        // 隐藏所有覆盖层
        logoContainer.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        Log.d(TAG, "All overlay views hidden");

        // 根据渲染方式显示相应的加载界面
        if (USE_CANVAS_2D) {
            simpleView.setVisibility(View.VISIBLE);
            simpleView.setLoadingState(true);
            Log.d(TAG, "GameSimpleView loading state set to true");
        } else {
            if (gameSurfaceView20 != null) {
                gameSurfaceView20.setVisibility(View.VISIBLE);
                gameSurfaceView20.setLoadingState(true);

                // 显示GL加载文字覆盖层
                if (glLoadingOverlay != null) {
                    glLoadingOverlay.setVisibility(View.VISIBLE);
                    // 重置文字
                    if (glLoaderTitle != null) {
                        glLoaderTitle.setText("LOADING");
                    }
                    if (glLoadingPercent != null) {
                        glLoadingPercent.setText("0%");
                    }
                    if (glLoadingStatus != null) {
                        glLoadingStatus.setText("INITIALIZING");
                    }
                    if (glLoadingHint != null) {
                        glLoadingHint.setText("按 ESC 跳过加载");
                    }
                }

                Log.d(TAG, "GameGLSurfaceView20 with text overlay loading state set to true");
            } else if (gameSurfaceView != null) {
                gameSurfaceView.setVisibility(View.VISIBLE);
                gameSurfaceView.setLoadingState(true);
                Log.d(TAG, "GLSurfaceView loading state set to true");
            }
        }

        // 使用游戏引擎的逻辑线程加载资源
        new Thread(() -> {
            // 直接在当前新线程中执行资源加载
            Log.d(TAG, "Starting resource loading in new thread");
            try {
                LoadSimulator loadSimulator = new LoadSimulator(getPackageName());

                // 模拟不同类型的资源加载
                String loadType = getPackageName();
                int loadLevel = 1; // 默认轻负载

                if (loadType.contains("light")) {
                    loadLevel = 1;
                } else if (loadType.contains("medium")) {
                    loadLevel = 3;
                } else if (loadType.contains("heavy")) {
                    loadLevel = 6;
                }

                Log.d(TAG, "Load level: " + loadLevel + " for type: " + loadType);

                // 初始化阶段
                updateGLLoadingProgress("初始化游戏引擎...", 5);
                loadSimulator.executeLoad(20 * loadLevel);
                Thread.sleep(100);

                // 着色器编译
                updateGLLoadingProgress("编译着色器...", 15);
                for (int i = 0; i < loadLevel; i++) {
                    loadSimulator.simulateShaderCompilation();
                    loadSimulator.executeLoad(10);
                    Thread.sleep(100);
                }

                // 纹理加载
                updateGLLoadingProgress("加载纹理资源...", 30);
                for (int i = 0; i < loadLevel * 2; i++) {
                    loadSimulator.simulateTextureLoading();
                    loadSimulator.executeLoad(15);
                    Thread.sleep(100);
                }

                // 音频资源
                updateGLLoadingProgress("加载音频资源...", 50);
                loadSimulator.executeLoad(40 * loadLevel);
                Thread.sleep(100);

                // 3D模型和场景
                updateGLLoadingProgress("加载3D模型...", 70);
                loadSimulator.executeLoad(60 * loadLevel);
                Thread.sleep(100);

                // 物理引擎
                updateGLLoadingProgress("初始化物理引擎...", 85);
                loadSimulator.simulatePhysicsEngineInit();
                Thread.sleep(100);

                // 最终优化
                updateGLLoadingProgress("最终优化...", 95);
                loadSimulator.executeLoad(30);
                Thread.sleep(100);

                // 完成
                updateGLLoadingProgress("加载完成!", 100);
                Thread.sleep(500);

                // 通知主线程加载完成
                mainHandler.post(() -> {
                    onAssetsLoaded();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during resource loading", e);
                mainHandler.post(() -> {
                    onAssetsLoaded();
                });
            }
        }, "ResourceLoadingThread").start();
    }

    private void updateLoadingProgress(String message, int progress) {
        mainHandler.post(() -> {
            if (loadingDetail != null) {
                loadingDetail.setText(message);
            }
            if (loadingProgress != null) {
                loadingProgress.setProgress(progress);
            }
        });
    }

    private void updateGLLoadingProgress(String message, int progress) {
        // 更新渲染的加载进度
        if (USE_CANVAS_2D && simpleView != null) {
            simpleView.updateLoadingProgress(message, progress);
        } else if (gameSurfaceView20 != null) {
            gameSurfaceView20.updateLoadingProgress(message, progress);

            // 安全地更新文字覆盖层 - 必须在主线程执行
            try {
                runOnUiThread(() -> {
                    if (glLoadingPercent != null) {
                        glLoadingPercent.setText(progress + "%");
                    }

                    // 根据进度更新状态文字
                    if (glLoadingStatus != null) {
                        if (progress < 30) {
                            glLoadingStatus.setText("INITIALIZING");
                        } else if (progress < 60) {
                            glLoadingStatus.setText("LOADING ASSETS");
                        } else if (progress < 90) {
                            glLoadingStatus.setText("OPTIMIZING");
                        } else if (progress < 100) {
                            glLoadingStatus.setText("FINALIZING");
                        } else {
                            glLoadingStatus.setText("READY");
                        }
                    }

                    // 加载完成时显示特别文字
                    if (glLoaderTitle != null && progress >= 100) {
                        glLoaderTitle.setText("LOADING COMPLETE");
                        if (glLoadingHint != null) {
                            glLoadingHint.setText("Touch to start");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating loading UI", e);
            }

        } else if (gameSurfaceView != null) {
            gameSurfaceView.updateLoadingProgress(message, progress);
        }
    }

    private void onAssetsLoaded() {
        Log.d(TAG, "All assets loaded successfully");

        // 通知渲染线程加载完成
        if (USE_CANVAS_2D && simpleView != null) {
            simpleView.setLoadingComplete();
        } else if (gameSurfaceView20 != null) {
            gameSurfaceView20.setLoadingComplete();
        } else if (gameSurfaceView != null) {
            gameSurfaceView.setLoadingComplete();
        }

        // 显示开始游戏界面
        showStartGameScreen();
    }

    private void showStartGameScreen() {
        Log.d(TAG, "Stage 4: Showing start game screen");

        // 隐藏GL加载文字覆盖层
        if (glLoadingOverlay != null) {
            glLoadingOverlay.setVisibility(View.GONE);
        }

        // 停止 OpenGL 渲染以节省资源
        if (gameSurfaceView20 != null) {
            gameSurfaceView20.stopRendering();
        }

        long totalLoadTime = System.currentTimeMillis() - loadingStartTime;
        String statsText = String.format("总加载时间: %.2f 秒\n负载级别: %s",
            totalLoadTime / 1000.0,
            getLoadLevelText());

        loadStats.setText(statsText);
        startGameContainer.setVisibility(View.VISIBLE);
    }

    private String getLoadLevelText() {
        if (BuildConfig.LOAD_DURATION_MS <= 5000) {
            return "轻负载 (3秒)";
        } else if (BuildConfig.LOAD_DURATION_MS <= 15000) {
            return "中负载 (10秒)";
        } else {
            return "重负载 (20秒)";
        }
    }

    private void startGame() {
        Log.d(TAG, "Starting game...");
        isGameReady = true;
        startGameContainer.setVisibility(View.GONE);
        fpsCounter.setVisibility(View.VISIBLE);

        // 游戏引擎已通过其他方式启动

        // 启动游戏渲染
        if (gameSurfaceView20 != null) {
            gameSurfaceView20.startGame();
        } else if (gameSurfaceView != null) {
            gameSurfaceView.startGame();
        }

        Trace.endSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGameReady && gameSurfaceView20 != null) {
            gameSurfaceView20.onResume();
        } else if (isGameReady && gameSurfaceView != null) {
            gameSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameSurfaceView20 != null) {
            gameSurfaceView20.onPause();
        } else if (gameSurfaceView != null) {
            gameSurfaceView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 游戏引擎清理已在其他地方处理

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}