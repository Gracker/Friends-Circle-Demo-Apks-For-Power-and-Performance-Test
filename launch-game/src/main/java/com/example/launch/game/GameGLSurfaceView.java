package com.example.launch.game;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.*;

import com.example.launch.game.BuildConfig;
import com.example.launch.game.LoadSimulator;

/**
 * 游戏渲染使用的 OpenGL SurfaceView
 * 实现 60fps 的绚丽游戏界面渲染
 */
public class GameGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "GameGLSurfaceView";
    private static final int TARGET_FPS = 60;
    private static final int FRAME_TIME_MS = 1000 / TARGET_FPS;

    private GameRenderer renderer;
    private OnFrameUpdateListener frameUpdateListener;
    private boolean isGameStarted = false;
    private boolean isLoading = false;
    private String loadingMessage = "";
    private int loadingProgress = 0;
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private int currentFPS = 0;
    private long fpsUpdateTime = 0;
    private LoadSimulator renderLoadSimulator;

    public interface OnFrameUpdateListener {
        void onFrameUpdate(int fps);
    }

    public GameGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public GameGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 设置 OpenGL ES 2.0
        setEGLContextClientVersion(2);

        // 尝试设置EGL配置选择器，确保使用RGB565以支持更多设备
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        // 初始化渲染负载模拟器
        String loadType = "game_render_" + (BuildConfig.LOAD_DURATION_MS <= 5000 ? "light" :
                           BuildConfig.LOAD_DURATION_MS <= 15000 ? "medium" : "heavy");
        renderLoadSimulator = new LoadSimulator(loadType);

        // 创建渲染器
        renderer = new GameRenderer();
        setRenderer(renderer);

        // 设置渲染模式为持续渲染，确保加载界面动画流畅
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Log.d(TAG, "GLSurfaceView initialized with OpenGL ES 2.0 and custom EGL config");
    }

    public void startGame() {
        Log.d(TAG, "Starting game rendering");
        isGameStarted = true;
        lastFrameTime = System.currentTimeMillis();
        fpsUpdateTime = lastFrameTime;
        frameCount = 0;
        Log.d(TAG, "Game started: " + isGameStarted);
        queueEvent(() -> {
            Log.d(TAG, "Queueing renderer startGame");
            renderer.startGame();
        });
    }

    public void stopGame() {
        Log.d(TAG, "Stopping game rendering");
        isGameStarted = false;
        queueEvent(() -> renderer.stopGame());
    }

    public void setOnFrameUpdateListener(OnFrameUpdateListener listener) {
        this.frameUpdateListener = listener;
    }

    public void setLoadingState(boolean loading) {
        this.isLoading = loading;
    }

    public void updateLoadingProgress(String message, int progress) {
        this.loadingMessage = message;
        this.loadingProgress = progress;
        Log.d(TAG, "Loading progress updated: " + progress + "% - " + message);

        // 请求重新渲染以更新进度
        requestRender();
    }

    public void setLoadingComplete() {
        this.isLoading = false;
        this.loadingMessage = "";
        this.loadingProgress = 100;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopGame();
    }

    /**
     * 游戏渲染器类
     */
    private class GameRenderer implements GLSurfaceView.Renderer {
        private boolean gameStarted = false;
        private float time = 0.0f;

        // OpenGL 对象

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Trace.beginSection("GameRenderer_onSurfaceCreated");

            Log.d(TAG, "OpenGL surface created");

            // 设置清屏颜色为黑色
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // 启用混合
            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            // 使用OpenGL ES 1.0的固定管线
            Log.d(TAG, "OpenGL initialized with fixed pipeline");

            // 打印OpenGL信息
            Log.d(TAG, "OpenGL vendor: " + gl.glGetString(GL10.GL_VENDOR));
            Log.d(TAG, "OpenGL renderer: " + gl.glGetString(GL10.GL_RENDERER));
            Log.d(TAG, "OpenGL version: " + gl.glGetString(GL10.GL_VERSION));

            Trace.endSection();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Trace.beginSection("GameRenderer_onSurfaceChanged");

            // 设置视口
            glViewport(0, 0, width, height);

            Trace.endSection();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            Trace.beginSection("GameRenderer_onDrawFrame");

            Log.v(TAG, "onDrawFrame called, gameStarted=" + gameStarted);

            // 简单测试：设置一个固定的颜色而不是动态背景
            // 使用红色，这样明显能看出来是否在渲染
            gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);  // 红色
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            // 如果游戏还没开始，显示加载界面
            if (!gameStarted) {
                Log.d(TAG, "Game not started, showing loading screen");
                drawLoadingScreen(gl);
                performLoadingRender();
                drawLoadingUI(gl);
                Trace.endSection();
                return;
            }

            // 游戏已开始的简单渲染
            // 使用绿色背景
            gl.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);  // 绿色
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            // 更新时间
            time += 0.016f; // ~60fps

            // 清屏
            glClear(GL_COLOR_BUFFER_BIT);

            // 执行轻量级渲染负载
            performRenderingLoad();

            // 绘制游戏画面
            drawGameScene();

            // 更新 FPS
            updateFPS();

            Trace.endSection();
        }

        private void performRenderingLoad() {
            // 使用真实的计算负载来模拟渲染压力
            // 根据负载级别执行不同的计算

            int complexity = 10; // 基础复杂度
            if (BuildConfig.LOAD_DURATION_MS > 5000) {
                complexity = 30; // 中负载
            }
            if (BuildConfig.LOAD_DURATION_MS > 15000) {
                complexity = 60; // 高负载
            }

            // 每帧执行少量计算，避免影响帧率
            renderLoadSimulator.executeLoad(complexity / 10);
        }

        private void drawGameScene() {
            // 游戏开始后的简单渲染
            // 这里可以添加实际的游戏渲染逻辑
        }

        private java.nio.FloatBuffer createFloatBuffer(float[] array) {
            java.nio.FloatBuffer buffer = java.nio.ByteBuffer
                .allocateDirect(array.length * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer();
            buffer.put(array);
            buffer.position(0);
            return buffer;
        }

        public void startGame() {
            gameStarted = true;
            time = 0.0f;
            Log.d(TAG, "Game renderer started");
        }

        public void stopGame() {
            gameStarted = false;
            Log.d(TAG, "Game renderer stopped");
        }

        /**
         * 绘制加载屏幕
         */
        private void drawLoadingScreen(GL10 gl) {
            // 获取进度和消息
            int progress = GameGLSurfaceView.this.loadingProgress;
            String message = GameGLSurfaceView.this.loadingMessage;

            Log.d(TAG, "Drawing loading screen - Progress: " + progress + "%, Message: " + message);

            // 最简单的测试：只清屏为不同的颜色
            // 使用黄色，这样能和背景的红色区分开
            gl.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);  // 黄色
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            // 不绘制任何东西，只看颜色变化
            // 如果这里也看不到，说明OpenGL上下文有问题
        }

        /**
         * 绘制GRACKER Logo (OpenGL ES 1.0)
         */
        private void drawGRACKERLogoGL10(GL10 gl) {
            gl.glColor4f(0.3f, 0.69f, 0.31f, 1.0f); // GRACKER绿色

            // 绘制Logo矩形
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, createFloatBuffer(new float[] {
                -0.3f,  0.5f, 0.0f,  // 左上
                -0.3f,  0.1f, 0.0f,  // 左下
                 0.3f,  0.1f, 0.0f,  // 右下
                 0.3f,  0.5f, 0.0f   // 右上
            }));
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);

            // 绘制边框
            gl.glColor4f(0.2f, 0.59f, 0.21f, 1.0f); // 深绿色
            gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
        }

        /**
         * 绘制进度条 (OpenGL ES 1.0)
         */
        private void drawProgressBarGL10(GL10 gl, int progress) {
            // 进度条背景
            gl.glColor4f(0.2f, 0.2f, 0.2f, 0.8f); // 灰色背景
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, createFloatBuffer(new float[] {
                -0.6f, -0.3f, 0.0f,  // 左下
                 0.6f, -0.3f, 0.0f,  // 右下
                 0.6f, -0.2f, 0.0f,  // 右上
                -0.6f, -0.2f, 0.0f   // 左上
            }));
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);

            // 进度条填充
            if (progress > 0) {
                float width = 1.2f * (progress / 100.0f);
                gl.glColor4f(0.0f, 0.8f, 0.4f, 1.0f); // 绿色填充
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, createFloatBuffer(new float[] {
                    -0.6f, -0.3f, 0.0f,  // 左下
                    -0.6f + width, -0.3f, 0.0f,  // 右下
                    -0.6f + width, -0.2f, 0.0f,  // 右上
                    -0.6f, -0.2f, 0.0f   // 左上
                }));
                gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
            }
        }

        /**
         * 绘制加载文字区域 (OpenGL ES 1.0)
         */
        private void drawLoadingTextGL10(GL10 gl, String message) {
            // 在进度条上方绘制一个小方块表示文本
            gl.glColor4f(0.8f, 0.8f, 0.8f, 0.5f); // 半透明白色
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, createFloatBuffer(new float[] {
                -0.4f, -0.1f, 0.0f,  // 左下
                 0.4f, -0.1f, 0.0f,  // 右下
                 0.4f,  0.0f, 0.0f,  // 右上
                -0.4f,  0.0f, 0.0f   // 左上
            }));
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
        }

        /**
         * 绘制动态元素 (OpenGL ES 1.0)
         */
        private void drawDynamicElementsGL10(GL10 gl) {
            long time = System.currentTimeMillis() / 200;

            // 绘制移动的小方块作为粒子效果
            for (int i = 0; i < 5; i++) {
                float x = (float) Math.sin((time + i * 30) * 0.1) * 0.8f;
                float y = (float) Math.cos((time + i * 30) * 0.15) * 0.3f;
                float size = 0.05f;

                float r = 0.5f + 0.5f * (float)Math.sin((time + i * 30) * 0.1);
                float g = 0.5f + 0.5f * (float)Math.cos((time + i * 30) * 0.13);
                float b = 0.5f + 0.5f * (float)Math.sin((time + i * 30) * 0.07);

                gl.glColor4f(r, g, b, 0.8f);
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, createFloatBuffer(new float[] {
                    x - size, y - size, 0.0f,
                    x + size, y - size, 0.0f,
                    x + size, y + size, 0.0f,
                    x - size, y + size, 0.0f
                }));
                gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
            }
        }

        /**
         * 执行加载时的渲染负载
         */
        private void performLoadingRender() {
            // 在加载阶段也执行一些计算，模拟真实的渲染压力
            int complexity = 20; // 加载时的固定复杂度
            for (int i = 0; i < complexity; i++) {
                // 模拟数学计算
                float x = (float) Math.sin(i) * (float) Math.cos(i * 0.5f);
                float y = x * x;
                float z = (float) Math.sqrt(Math.abs(y));
            }
        }

        /**
         * 绘制进度条和文字
         */
        private void drawLoadingUI(GL10 gl) {
            // 获取外部变量
            String message = GameGLSurfaceView.this.loadingMessage;
            int progress = GameGLSurfaceView.this.loadingProgress;

            // 这里会在 OpenGL 中绘制进度条和文字
            // 简化实现，只记录要显示的内容
            Log.d(TAG, "Loading UI - Progress: " + progress + "%, Message: " + message);
        }
    }

    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();

        // 每秒更新一次 FPS 显示
        if (currentTime - fpsUpdateTime >= 1000) {
            currentFPS = frameCount;
            frameCount = 0;
            fpsUpdateTime = currentTime;

            if (frameUpdateListener != null) {
                post(() -> frameUpdateListener.onFrameUpdate(currentFPS));
            }
        }

        // 帧率限制
        long frameTime = currentTime - lastFrameTime;
        if (frameTime < FRAME_TIME_MS) {
            try {
                Thread.sleep(FRAME_TIME_MS - frameTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastFrameTime = System.currentTimeMillis();
    }
}