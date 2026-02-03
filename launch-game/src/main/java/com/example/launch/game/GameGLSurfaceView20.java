package com.example.launch.game;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;
import static android.opengl.Matrix.*;

/**
 * 游戏渲染使用的 OpenGL ES 2.0 SurfaceView
 * 实现专业的加载界面和游戏场景渲染
 */
public class GameGLSurfaceView20 extends GLSurfaceView {
    private static final String TAG = "GameGLSurfaceView20";
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

    public interface OnFrameUpdateListener {
        void onFrameUpdate(int fps);
    }

    public GameGLSurfaceView20(Context context) {
        super(context);
        init();
    }

    public GameGLSurfaceView20(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 设置 OpenGL ES 2.0
        setEGLContextClientVersion(2);

        // 设置EGL配置
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        // 创建渲染器
        renderer = new GameRenderer(getContext());
        setRenderer(renderer);

        // 设置渲染模式为持续渲染
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Log.d(TAG, "GLSurfaceView initialized with OpenGL ES 2.0");
    }

    public void startGame() {
        Log.d(TAG, "Starting game rendering");
        isGameStarted = true;
        queueEvent(() -> renderer.startGame());
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
        queueEvent(() -> renderer.setLoadingState(loading));
    }

    public void updateLoadingProgress(String message, int progress) {
        this.loadingMessage = message;
        this.loadingProgress = progress;
        queueEvent(() -> renderer.updateLoadingProgress(message, progress));
    }

    public void setLoadingComplete() {
        this.isLoading = false;
        this.loadingMessage = "";
        this.loadingProgress = 100;
        queueEvent(() -> renderer.setLoadingComplete());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopGame();
        queueEvent(() -> renderer.cleanup());
    }

    /**
     * OpenGL ES 2.0 渲染器类
     */
    private class GameRenderer implements GLSurfaceView.Renderer {
        private Context context;

        // 着色器程序
        private int backgroundProgram;
        private int progressProgram;
        private int particleProgram;

        // OpenGL 对象
        private int[] textures = new int[1];

        // 矩阵
        private float[] projectionMatrix = new float[16];
        private float[] viewMatrix = new float[16];
        private float[] modelMatrix = new float[16];
        private float[] mvpMatrix = new float[16];
        private float[] tempMatrix = new float[16];

        // 属性位置
        private int positionAttribute;
        private int texCoordAttribute;

        // Uniform 位置
        private int mvpMatrixUniform;
        private int timeUniform;
        private int resolutionUniform;
        private int progressUniform;

        // 时间和动画
        private float time = 0.0f;
        private long startTime;

        // 粒子系统
        private static final int PARTICLE_COUNT = 50;
        private FloatBuffer particleVertices;
        private FloatBuffer particleColors;
        private FloatBuffer particleVelocities;

        // 加载状态
        private boolean isLoading = false;
        private String loadingMessage = "";
        private int loadingProgress = 0;
        private boolean gameStarted = false;

        // 屏幕尺寸
        private int screenWidth;
        private int screenHeight;

        public GameRenderer(Context context) {
            this.context = context;
            startTime = System.currentTimeMillis();

            // 初始化粒子数据
            initParticles();
        }

        private void initParticles() {
            float[] vertices = new float[PARTICLE_COUNT * 3];
            float[] colors = new float[PARTICLE_COUNT * 3];
            float[] velocities = new float[PARTICLE_COUNT * 3];

            for (int i = 0; i < PARTICLE_COUNT; i++) {
                // 位置
                vertices[i * 3] = (float) (Math.random() * 2.0 - 1.0);
                vertices[i * 3 + 1] = (float) (Math.random() * 2.0 - 1.0);
                vertices[i * 3 + 2] = 0.0f;

                // 速度
                velocities[i * 3] = (float) (Math.random() * 0.02 - 0.01);
                velocities[i * 3 + 1] = (float) (Math.random() * 0.02 - 0.01);
                velocities[i * 3 + 2] = 0.0f;

                // 颜色（偏向绿色）
                colors[i * 3] = 0.2f + (float) Math.random() * 0.3f;      // R
                colors[i * 3 + 1] = 0.5f + (float) Math.random() * 0.5f;  // G
                colors[i * 3 + 2] = 0.2f + (float) Math.random() * 0.3f;  // B
            }

            particleVertices = createFloatBuffer(vertices);
            particleColors = createFloatBuffer(colors);
            particleVelocities = createFloatBuffer(velocities);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Trace.beginSection("GameRenderer_onSurfaceCreated");

            Log.d(TAG, "OpenGL ES 2.0 surface created");

            // 设置清屏颜色
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // 启用混合
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // 编译着色器程序
            backgroundProgram = createProgram(
                loadShaderSource(GL_VERTEX_SHADER, "vertex_shader.glsl"),
                loadShaderSource(GL_FRAGMENT_SHADER, "fragment_shader.glsl")
            );

            progressProgram = createProgram(
                loadShaderSource(GL_VERTEX_SHADER, "vertex_shader.glsl"),
                loadShaderSource(GL_FRAGMENT_SHADER, "progress_fragment_shader.glsl")
            );

            particleProgram = createProgram(
                loadShaderSource(GL_VERTEX_SHADER, "particle_vertex_shader.glsl"),
                loadShaderSource(GL_FRAGMENT_SHADER, "particle_fragment_shader.glsl")
            );

            // 获取属性和uniform位置
            positionAttribute = glGetAttribLocation(backgroundProgram, "aPosition");
            texCoordAttribute = glGetAttribLocation(backgroundProgram, "aTexCoord");

            mvpMatrixUniform = glGetUniformLocation(backgroundProgram, "uMVPMatrix");
            timeUniform = glGetUniformLocation(backgroundProgram, "uTime");
            resolutionUniform = glGetUniformLocation(backgroundProgram, "uResolution");
            progressUniform = glGetUniformLocation(progressProgram, "uProgress");

            // 生成全屏四边形
            createFullScreenQuad();

            Trace.endSection();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Trace.beginSection("GameRenderer_onSurfaceChanged");

            screenWidth = width;
            screenHeight = height;

            // 设置视口
            glViewport(0, 0, width, height);

            // 设置投影矩阵（正交投影）
            orthoM(projectionMatrix, 0, -1, 1, -1, 1, -1, 1);

            // 设置视图矩阵（单位矩阵）
            setIdentityM(viewMatrix, 0);

            Trace.endSection();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            Trace.beginSection("GameRenderer_onDrawFrame");

            // 更新时间
            long currentTime = System.currentTimeMillis();
            time = (currentTime - startTime) / 1000.0f;

            // 清屏
            glClear(GL_COLOR_BUFFER_BIT);

            if (!gameStarted || isLoading) {
                // 渲染加载界面
                renderBackground();
                renderParticles();
                renderProgress();
            } else {
                // 渲染游戏场景
                renderGameScene();
            }

            // 更新 FPS
            updateFPS();

            Trace.endSection();
        }

        private void renderBackground() {
            glUseProgram(backgroundProgram);

            // 设置 uniforms
            glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
            glUniform1f(timeUniform, time);
            glUniform2f(resolutionUniform, screenWidth, screenHeight);

            // 绘制全屏四边形
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }

        private void renderProgress() {
            glUseProgram(progressProgram);

            // 设置 uniforms
            glUniform2f(resolutionUniform, screenWidth, screenHeight);
            glUniform1f(progressUniform, loadingProgress / 100.0f);
            glUniform1f(timeUniform, time);

            // 绘制进度条（使用全屏四边形，在着色器中裁剪）
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }

        private void renderParticles() {
            glUseProgram(particleProgram);

            // 设置属性
            int positionLoc = glGetAttribLocation(particleProgram, "aPosition");
            int velocityLoc = glGetAttribLocation(particleProgram, "aVelocity");
            int colorLoc = glGetAttribLocation(particleProgram, "aColor");
            int mvpLoc = glGetUniformLocation(particleProgram, "uMVPMatrix");
            int timeLoc = glGetUniformLocation(particleProgram, "uTime");

            glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
            glUniform1f(timeLoc, time);

            // 设置顶点数据
            particleVertices.position(0);
            glVertexAttribPointer(positionLoc, 3, GL_FLOAT, false, 0, particleVertices);
            glEnableVertexAttribArray(positionLoc);

            particleVelocities.position(0);
            glVertexAttribPointer(velocityLoc, 3, GL_FLOAT, false, 0, particleVelocities);
            glEnableVertexAttribArray(velocityLoc);

            particleColors.position(0);
            glVertexAttribPointer(colorLoc, 3, GL_FLOAT, false, 0, particleColors);
            glEnableVertexAttribArray(colorLoc);

            // 绘制粒子
            glDrawArrays(GL_POINTS, 0, PARTICLE_COUNT);

            // 禁用属性
            glDisableVertexAttribArray(positionLoc);
            glDisableVertexAttribArray(velocityLoc);
            glDisableVertexAttribArray(colorLoc);
        }

        private void renderGameScene() {
            // 简单的游戏场景 - 绿色背景
            glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            // 这里可以添加实际的游戏渲染逻辑
        }

        private void createFullScreenQuad() {
            // 顶点位置和纹理坐标
            float[] vertices = {
                -1.0f, -1.0f, 0.0f,  // 左下
                 1.0f, -1.0f, 0.0f,  // 右下
                -1.0f,  1.0f, 0.0f,  // 左上
                 1.0f,  1.0f, 0.0f   // 右上
            };

            float[] texCoords = {
                0.0f, 1.0f,  // 左下
                1.0f, 1.0f,  // 右下
                0.0f, 0.0f,  // 左上
                1.0f, 0.0f   // 右上
            };

            FloatBuffer vertexBuffer = createFloatBuffer(vertices);
            FloatBuffer texBuffer = createFloatBuffer(texCoords);

            glVertexAttribPointer(positionAttribute, 3, GL_FLOAT, false, 0, vertexBuffer);
            glEnableVertexAttribArray(positionAttribute);

            glVertexAttribPointer(texCoordAttribute, 2, GL_FLOAT, false, 0, texBuffer);
            glEnableVertexAttribArray(texCoordAttribute);
        }

        private String loadShaderSource(int type, String filename) {
            StringBuilder shaderSource = new StringBuilder();
            try {
                InputStream inputStream = context.getResources().openRawResource(
                    context.getResources().getIdentifier(
                        filename.split("\\.")[0],
                        "raw",
                        context.getPackageName()
                    )
                );
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    shaderSource.append(line).append("\n");
                }
                reader.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to load shader: " + filename, e);
            }
            return shaderSource.toString();
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }

            int fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
            if (fragmentShader == 0) {
                glDeleteShader(vertexShader);
                return 0;
            }

            int program = glCreateProgram();
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);

            int[] linkStatus = new int[1];
            glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, glGetProgramInfoLog(program));
                glDeleteProgram(program);
                return 0;
            }

            return program;
        }

        private int loadShader(int type, String source) {
            int shader = glCreateShader(type);
            glShaderSource(shader, source);
            glCompileShader(shader);

            int[] compiled = new int[1];
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + type + ":");
                Log.e(TAG, glGetShaderInfoLog(shader));
                glDeleteShader(shader);
                return 0;
            }

            return shader;
        }

        private FloatBuffer createFloatBuffer(float[] array) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(array.length * 4);
            buffer.order(ByteOrder.nativeOrder());
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            floatBuffer.put(array);
            floatBuffer.position(0);
            return floatBuffer;
        }

        public void startGame() {
            gameStarted = true;
            isLoading = false;
            Log.d(TAG, "Game started");
        }

        public void stopGame() {
            gameStarted = false;
            Log.d(TAG, "Game stopped");
        }

        public void setLoadingState(boolean loading) {
            this.isLoading = loading;
        }

        public void updateLoadingProgress(String message, int progress) {
            this.loadingMessage = message;
            this.loadingProgress = progress;
        }

        public void setLoadingComplete() {
            this.isLoading = false;
            this.loadingProgress = 100;
        }

        public void cleanup() {
            // 删除着色器程序
            if (backgroundProgram != 0) {
                glDeleteProgram(backgroundProgram);
                backgroundProgram = 0;
            }
            if (progressProgram != 0) {
                glDeleteProgram(progressProgram);
                progressProgram = 0;
            }
            if (particleProgram != 0) {
                glDeleteProgram(particleProgram);
                particleProgram = 0;
            }

            // 删除纹理
            if (textures[0] != 0) {
                glDeleteTextures(1, textures, 0);
                textures[0] = 0;
            }

            // 释放FloatBuffer引用，让GC回收Native内存
            particleVertices = null;
            particleColors = null;
            particleVelocities = null;

            // 重置状态
            gameStarted = false;
            isLoading = false;

            Log.d(TAG, "GameRenderer resources cleaned up");
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
}