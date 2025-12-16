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
 * 游戏渲染使用的 OpenGL ES 2.0 SurfaceView（修复版）
 * 解决黑屏问题
 */
public class GameGLSurfaceView20Fixed extends GLSurfaceView {
    private static final String TAG = "GameGLSurfaceView20F";
    private static final int TARGET_FPS = 60;

    private GameRenderer renderer;
    private OnFrameUpdateListener frameUpdateListener;
    private boolean isGameStarted = false;
    private boolean isLoading = false;
    private String loadingMessage = "";
    private int loadingProgress = 0;

    public interface OnFrameUpdateListener {
        void onFrameUpdate(int fps);
    }

    public GameGLSurfaceView20Fixed(Context context) {
        super(context);
        init();
    }

    public GameGLSurfaceView20Fixed(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 设置 OpenGL ES 2.0
        setEGLContextClientVersion(2);
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

    public void stopRendering() {
        queueEvent(() -> renderer.stopRendering());
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
        private int simpleProgram;  // 使用简单的着色器进行测试
        private int backgroundProgram;
        private int progressProgram;
        private int loadingBackgroundProgram;  // 优化的加载背景
        private int loadingProgressProgram;    // 优化的进度条
        private int gameLoadingProgram;        // 完整的游戏加载界面

        // 顶点缓冲区
        private FloatBuffer quadVertices;
        private FloatBuffer quadTexCoords;

        // 矩阵
        private float[] mvpMatrix = new float[16];

        // 时间和动画
        private float time = 0.0f;
        private long startTime;

        // 加载状态
        private boolean isLoading = false;
        private String loadingMessage = "";
        private int loadingProgress = 0;
        private boolean gameStarted = false;

        // 属性和uniform位置
        private int positionLoc;
        private int texCoordLoc;
        private int mvpLoc;
        private int timeLoc;
        private int resolutionLoc;
        private int progressLoc;
        private int mouseLoc;
        private int progressColorLoc;  // 进度条颜色

        // 渲染控制
        private boolean shouldStopRendering = false;

        // 屏幕尺寸
        private int screenWidth;
        private int screenHeight;

        public GameRenderer(Context context) {
            this.context = context;
            startTime = System.currentTimeMillis();

            // 创建全屏四边形
            createFullScreenQuad();
        }

        private void createFullScreenQuad() {
            // 顶点位置 (NDC坐标)
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

            quadVertices = createFloatBuffer(vertices);
            quadTexCoords = createFloatBuffer(texCoords);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Trace.beginSection("GameRenderer_onSurfaceCreated");

            Log.d(TAG, "OpenGL ES 2.0 surface created");

            // 设置清屏颜色为深蓝色（明显可见）
            glClearColor(0.0f, 0.0f, 0.3f, 1.0f);

            // 启用混合
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // 创建最简单的着色器
            String vertexShaderCode =
                "attribute vec4 aPosition;\n" +
                "void main() {\n" +
                "    gl_Position = aPosition;\n" +
                "}\n";

            String fragmentShaderCode =
                "precision mediump float;\n" +
                "void main() {\n" +
                "    // 简单的深蓝色\n" +
                "    gl_FragColor = vec4(0.02, 0.05, 0.15, 1.0);\n" +
                "}\n";

            simpleProgram = createProgram(vertexShaderCode, fragmentShaderCode);
            if (simpleProgram == 0) {
                Log.e(TAG, "Failed to create simple program");
                return;
            }

            Log.d(TAG, "Simple program created successfully");

            // 获取属性和uniform位置
            positionLoc = glGetAttribLocation(simpleProgram, "aPosition");

            Log.d(TAG, "Shader program locations: pos=" + positionLoc);

            // 加载优化的加载界面着色器
            loadLoadingShaders();

            Trace.endSection();
        }

        private void loadLoadingShaders() {
            // 先尝试加载安全的着色器
            try {
                String vertex = loadShaderSource("vertex_shader.glsl");
                String fragment = loadShaderSource("safe_loading_fragment.glsl");

                gameLoadingProgram = createProgram(vertex, fragment);
                if (gameLoadingProgram != 0) {
                    progressLoc = glGetUniformLocation(gameLoadingProgram, "uProgress");
                    resolutionLoc = glGetUniformLocation(gameLoadingProgram, "uResolution");
                    timeLoc = glGetUniformLocation(gameLoadingProgram, "uTime");
                    Log.d(TAG, "Safe loading shader loaded successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load safe loading shaders", e);
            }

            // 如果主着色器加载失败，使用最简单的
            if (gameLoadingProgram == 0) {
                Log.w(TAG, "Main loading shader failed, using fallback");
                // Fallback 使用已经编译成功的 simple program
            }

            // 备用：加载优化的背景着色器
            try {
                String bgVertex = loadShaderSource("vertex_shader.glsl");
                String bgFragment = loadShaderSource("loading_background_fragment.glsl");

                loadingBackgroundProgram = createProgram(bgVertex, bgFragment);
                if (loadingBackgroundProgram != 0) {
                    Log.d(TAG, "Loading background shader loaded successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load loading background shaders", e);
            }

            // 备用：加载优化的进度条着色器
            try {
                String progressVertex = loadShaderSource("vertex_shader.glsl");
                String progressFragment = loadShaderSource("loading_progress_fragment.glsl");

                loadingProgressProgram = createProgram(progressVertex, progressFragment);
                if (loadingProgressProgram != 0) {
                    progressLoc = glGetUniformLocation(loadingProgressProgram, "uProgress");
                    resolutionLoc = glGetUniformLocation(loadingProgressProgram, "uResolution");
                    timeLoc = glGetUniformLocation(loadingProgressProgram, "uTime");
                    mouseLoc = glGetUniformLocation(loadingProgressProgram, "uMouse");
                    Log.d(TAG, "Loading progress shader loaded successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load loading progress shaders", e);
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Trace.beginSection("GameRenderer_onSurfaceChanged");

            screenWidth = width;
            screenHeight = height;

            // 设置视口
            glViewport(0, 0, width, height);

            // 设置MVP矩阵为单位矩阵（对于全屏四边形）
            setIdentityM(mvpMatrix, 0);

            Log.d(TAG, "Surface changed: " + width + "x" + height);

            Trace.endSection();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            Trace.beginSection("GameRenderer_onDrawFrame");

            // 如果应该停止渲染，只清空屏幕
            if (shouldStopRendering) {
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
                Trace.endSection();
                return;
            }

            // 更新时间
            time = (System.currentTimeMillis() - startTime) / 1000.0f;

            // 清屏 - 使用深蓝色背景
            glClearColor(0.02f, 0.05f, 0.15f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            // 检查 OpenGL 错误
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                Log.e(TAG, "OpenGL Error before rendering: " + error);
            }

            if (isLoading) {
                // 加载状态 - 使用简单的纯色背景
                if (simpleProgram != 0) {
                    try {
                        glUseProgram(simpleProgram);

                        // 检查 uniform 位置
                        int timeLoc = glGetUniformLocation(simpleProgram, "uTime");
                        int progressLoc = glGetUniformLocation(simpleProgram, "uProgress");
                        int resolutionLoc = glGetUniformLocation(simpleProgram, "uResolution");

                        if (timeLoc != -1) glUniform1f(timeLoc, time);
                        if (progressLoc != -1) glUniform1f(progressLoc, loadingProgress / 100.0f);
                        if (resolutionLoc != -1) glUniform2f(resolutionLoc, screenWidth, screenHeight);

                        // 设置顶点属性
                        quadVertices.position(0);
                        glVertexAttribPointer(positionLoc, 3, GL_FLOAT, false, 0, quadVertices);
                        glEnableVertexAttribArray(positionLoc);

                        // 绘制
                        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                        // 禁用属性
                        glDisableVertexAttribArray(positionLoc);

                        // 检查绘制错误
                        error = glGetError();
                        if (error != GL_NO_ERROR) {
                            Log.e(TAG, "OpenGL Error after drawing: " + error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during loading render", e);
                        // Fallback: 只清屏
                        glClearColor(0.02f, 0.05f, 0.15f, 1.0f);
                        glClear(GL_COLOR_BUFFER_BIT);
                    }
                }
            } else if (!gameStarted) {
                // 游戏未开始 - 纯色背景
                glClearColor(0.02f, 0.05f, 0.15f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
            } else if (gameStarted) {
                // 游戏已开始 - 简单的绿色背景
                glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
            }

            Trace.endSection();
        }

        private void renderLoadingScreen() {
            // 优先使用完整的游戏加载界面着色器
            if (gameLoadingProgram != 0) {
                glUseProgram(gameLoadingProgram);

                // 设置uniforms
                glUniform1f(progressLoc, loadingProgress / 100.0f);
                glUniform1f(timeLoc, time);
                glUniform2f(resolutionLoc, screenWidth, screenHeight);

                // 动态进度条颜色已在着色器中处理

                glUniformMatrix4fv(glGetUniformLocation(gameLoadingProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);

                // 设置顶点属性
                int posLoc = glGetAttribLocation(gameLoadingProgram, "aPosition");
                quadVertices.position(0);
                glVertexAttribPointer(posLoc, 3, GL_FLOAT, false, 0, quadVertices);
                glEnableVertexAttribArray(posLoc);

                // 绘制完整的加载界面
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
                glDisableVertexAttribArray(posLoc);
            } else {
                // 备用方案：分别渲染背景和进度条
                // 渲染背景
                if (loadingBackgroundProgram != 0) {
                    glUseProgram(loadingBackgroundProgram);

                    // 设置uniforms
                    int timeLoc = glGetUniformLocation(loadingBackgroundProgram, "uTime");
                    int resLoc = glGetUniformLocation(loadingBackgroundProgram, "uResolution");
                    int progressLoc = glGetUniformLocation(loadingBackgroundProgram, "uProgress");
                    int mvpLoc = glGetUniformLocation(loadingBackgroundProgram, "uMVPMatrix");

                    glUniform1f(timeLoc, time);
                    glUniform2f(resLoc, screenWidth, screenHeight);
                    glUniform1f(progressLoc, loadingProgress / 100.0f);
                    glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);

                    // 设置顶点属性
                    int posLoc = glGetAttribLocation(loadingBackgroundProgram, "aPosition");
                    quadVertices.position(0);
                    glVertexAttribPointer(posLoc, 3, GL_FLOAT, false, 0, quadVertices);
                    glEnableVertexAttribArray(posLoc);

                    // 绘制背景
                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
                    glDisableVertexAttribArray(posLoc);
                }

                // 渲染进度条
                if (loadingProgressProgram != 0) {
                    glUseProgram(loadingProgressProgram);

                    // 设置uniforms
                    glUniform1f(this.progressLoc, loadingProgress / 100.0f);
                    glUniform1f(timeLoc, time);
                    glUniform2f(resolutionLoc, screenWidth, screenHeight);
                    glUniform2f(mouseLoc, screenWidth * 0.5f, screenHeight * 0.5f);
                    glUniformMatrix4fv(glGetUniformLocation(loadingProgressProgram, "uMVPMatrix"), 1, false, mvpMatrix, 0);

                    // 设置顶点属性
                    int posLoc = glGetAttribLocation(loadingProgressProgram, "aPosition");
                    int texLoc = glGetAttribLocation(loadingProgressProgram, "aTexCoord");

                    quadVertices.position(0);
                    glVertexAttribPointer(posLoc, 3, GL_FLOAT, false, 0, quadVertices);
                    glEnableVertexAttribArray(posLoc);

                    if (texLoc != -1) {
                        quadTexCoords.position(0);
                        glVertexAttribPointer(texLoc, 2, GL_FLOAT, false, 0, quadTexCoords);
                        glEnableVertexAttribArray(texLoc);
                    }

                    // 绘制进度条
                    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                    glDisableVertexAttribArray(posLoc);
                    if (texLoc != -1) {
                        glDisableVertexAttribArray(texLoc);
                    }
                }
            }
        }

        private void renderSimpleBackground() {
            if (simpleProgram != 0) {
                glUseProgram(simpleProgram);

                // 设置顶点属性
                quadVertices.position(0);
                glVertexAttribPointer(positionLoc, 3, GL_FLOAT, false, 0, quadVertices);
                glEnableVertexAttribArray(positionLoc);

                // 绘制
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                // 禁用属性
                glDisableVertexAttribArray(positionLoc);
            }
        }

        private void renderGameScene() {
            // 简单的游戏场景 - 绿色背景
            glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            // 这里可以添加实际的游戏渲染逻辑
        }

        private String loadShaderSource(String filename) {
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
                return null;
            }
            return shaderSource.toString();
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            if (vertexSource == null || fragmentSource == null) {
                Log.e(TAG, "Vertex or fragment source is null");
                return 0;
            }

            Log.d(TAG, "Compiling vertex shader...");
            int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                Log.e(TAG, "Failed to compile vertex shader");
                return 0;
            }

            Log.d(TAG, "Compiling fragment shader...");
            int fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
            if (fragmentShader == 0) {
                Log.e(TAG, "Failed to compile fragment shader");
                glDeleteShader(vertexShader);
                return 0;
            }

            Log.d(TAG, "Creating and linking program...");
            int program = glCreateProgram();
            if (program == 0) {
                Log.e(TAG, "Failed to create program");
                return 0;
            }

            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);

            int[] linkStatus = new int[1];
            glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GL_TRUE) {
                String error = glGetProgramInfoLog(program);
                Log.e(TAG, "Could not link program: " + error);
                glDeleteProgram(program);
                return 0;
            }

            // 验证程序
            glValidateProgram(program);
            int[] validateStatus = new int[1];
            glGetProgramiv(program, GL_VALIDATE_STATUS, validateStatus, 0);
            if (validateStatus[0] != GL_TRUE) {
                Log.w(TAG, "Program validation failed: " + glGetProgramInfoLog(program));
            }

            // 删除着色器（已经链接到程序）
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);

            Log.d(TAG, "Program created successfully");
            return program;
        }

        private int loadShader(int type, String source) {
            int shader = glCreateShader(type);
            glShaderSource(shader, source);
            glCompileShader(shader);

            int[] compiled = new int[1];
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + type + ": " + glGetShaderInfoLog(shader));
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

        public void stopRendering() {
            shouldStopRendering = true;
            Log.d(TAG, "Rendering stopped");
        }

        public void cleanup() {
            if (simpleProgram != 0) {
                glDeleteProgram(simpleProgram);
            }
            if (backgroundProgram != 0) {
                glDeleteProgram(backgroundProgram);
            }
            if (progressProgram != 0) {
                glDeleteProgram(progressProgram);
            }
        }
    }
}