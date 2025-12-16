package com.example.launch.game;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 游戏引擎核心
 * 管理 VSYNC、渲染线程和逻辑线程
 */
public class GameEngine {
    private static final String TAG = "GameEngine";

    // 游戏状态
    public enum GameState {
        LOADING,
        READY,
        RUNNING,
        PAUSED
    }

    // 帧率控制
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1000000000L / TARGET_FPS;

    // 组件
    private Handler mainHandler;
    private Choreographer choreographer;
    private GameRenderer renderer;
    private LogicThread logicThread;

    // 同步控制
    private final Lock engineLock = new ReentrantLock();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger currentFrame = new AtomicInteger(0);
    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong lastFrameTimeNanos = new AtomicLong(0);

    // 线程间通信
    private final BlockingQueue<LogicTask> logicTaskQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean resourcesLoaded = new AtomicBoolean(false);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    // 回调接口
    public interface GameRenderer {
        void onFrame(long frameCount, float deltaTime);
        void onStart();
        void onStop();
        Canvas getCanvas();
        void swapBuffers();
    }

    public interface LoadingListener {
        void onLoadingProgress(int progress, String message);
        void onLoadingComplete();
    }

    public interface FPSListener {
        void onFPSUpdate(int fps);
    }

    private LoadingListener loadingListener;
    private FPSListener fpsListener;

    // VSYNC 回调
    private final FrameCallback frameCallback = new FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isRunning.get()) {
                return;
            }

            // 计算帧时间
            long currentTime = SystemClock.elapsedRealtimeNanos();
            float deltaTime = (currentTime - lastFrameTimeNanos.get()) / 1000000000.0f;
            lastFrameTimeNanos.set(currentTime);

            // 执行逻辑线程任务
            processLogicTasks();

            // 渲染
            if (renderer != null) {
                renderer.onFrame(frameCount.incrementAndGet(), deltaTime);

                // 计算FPS
                updateFPS();
            }

            // 继续下一帧
            if (isRunning.get()) {
                choreographer.postFrameCallback(this);
            }
        }
    };

    public GameEngine() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.choreographer = Choreographer.getInstance();
        this.logicThread = new LogicThread();
    }

    /**
     * 设置渲染器
     */
    public void setRenderer(GameRenderer renderer) {
        engineLock.lock();
        try {
            this.renderer = renderer;
        } finally {
            engineLock.unlock();
        }
    }

    /**
     * 设置加载监听器
     */
    public void setLoadingListener(LoadingListener listener) {
        this.loadingListener = listener;
        logicThread.setLoadingListener(listener);
    }

    /**
     * 设置FPS监听器
     */
    public void setFPSListener(FPSListener listener) {
        this.fpsListener = listener;
    }

    /**
     * 开始游戏引擎
     */
    public void start() {
        engineLock.lock();
        try {
            if (!isRunning.get()) {
                isRunning.set(true);
                lastFrameTimeNanos.set(SystemClock.elapsedRealtimeNanos());

                // 启动逻辑线程
                if (!logicThread.isAlive()) {
                    logicThread.start();
                    Log.d(TAG, "Logic thread started");
                } else {
                    Log.d(TAG, "Logic thread already running");
                }

                // 启动VSYNC回调
                choreographer.postFrameCallback(frameCallback);

                // 通知渲染器
                if (renderer != null) {
                    renderer.onStart();
                }

                Log.d(TAG, "Game engine started");
            }
        } finally {
            engineLock.unlock();
        }
    }

    /**
     * 停止游戏引擎
     */
    public void stop() {
        engineLock.lock();
        try {
            if (isRunning.get()) {
                isRunning.set(false);

                // 停止VSYNC回调
                choreographer.removeFrameCallback(frameCallback);

                // 停止逻辑线程
                logicThread.shutdown();

                // 通知渲染器
                if (renderer != null) {
                    renderer.onStop();
                }

                Log.d(TAG, "Game engine stopped");
            }
        } finally {
            engineLock.unlock();
        }
    }

    /**
     * 开始加载资源
     */
    public void startLoading(String loadType) {
        if (isLoading.compareAndSet(false, true)) {
            resourcesLoaded.set(false);

            // 确保逻辑线程已经启动
            if (!logicThread.isAlive()) {
                logicThread.start();
                try {
                    Thread.sleep(100); // 等待线程启动
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 提交加载任务到逻辑线程
            LogicTask loadTask = new LogicTask() {
                @Override
                public void execute(LoadSimulator loadSimulator) {
                    Log.d(TAG, "Executing resource loading task");
                    logicThread.performResourceLoading(loadSimulator);
                }
            };

            logicTaskQueue.offer(loadTask);
            Log.d(TAG, "Resource loading task queued");
        } else {
            Log.d(TAG, "Loading already in progress");
        }
    }

    /**
     * 检查资源是否已加载
     */
    public boolean areResourcesLoaded() {
        return resourcesLoaded.get();
    }

    /**
     * 获取当前游戏状态
     */
    public GameState getGameState() {
        if (isLoading.get()) {
            return GameState.LOADING;
        } else if (resourcesLoaded.get()) {
            return isRunning.get() ? GameState.RUNNING : GameState.READY;
        } else {
            return GameState.PAUSED;
        }
    }

    /**
     * 处理逻辑线程任务
     */
    private void processLogicTasks() {
        // 处理队列中的任务，但限制每帧的处理时间
        long startTime = System.nanoTime();
        long maxProcessTime = FRAME_TIME_NS / 4; // 最多使用1/4帧时间处理逻辑

        while (!logicTaskQueue.isEmpty() &&
               (System.nanoTime() - startTime) < maxProcessTime) {
            LogicTask task = logicTaskQueue.poll();
            if (task != null) {
                task.execute(logicThread.getLoadSimulator());
            }
        }
    }

    /**
     * 更新FPS
     */
    private void updateFPS() {
        if (fpsListener != null) {
            // 每秒更新一次FPS
            int frame = currentFrame.get();
            if (frame % TARGET_FPS == 0) {
                long currentTime = System.currentTimeMillis();
                long lastTime = currentTime - 1000; // 1秒前

                // 计算FPS
                int fps = calculateFPS(lastTime, currentTime);
                fpsListener.onFPSUpdate(fps);
            }
        }
    }

    private int calculateFPS(long startTime, long endTime) {
        // 简化的FPS计算
        return TARGET_FPS; // 在VSYNC模式下通常是稳定的60FPS
    }

    /**
     * 逻辑任务接口
     */
    public interface LogicTask {
        void execute(LoadSimulator loadSimulator);
    }

    /**
     * 提交逻辑任务
     */
    public void submitLogicTask(LogicTask task) {
        if (isRunning.get()) {
            logicTaskQueue.offer(task);
        }
    }

    /**
     * 逻辑线程
     */
    private class LogicThread extends Thread {
        private volatile boolean running = false;
        private LoadSimulator loadSimulator;
        private LoadingListener loadingListener;

        public LogicThread() {
            super("GameLogicThread");
            setDaemon(true);
        }

        public void setLoadingListener(LoadingListener listener) {
            this.loadingListener = listener;
        }

        public LoadSimulator getLoadSimulator() {
            if (loadSimulator == null) {
                loadSimulator = new LoadSimulator("logic_thread");
            }
            return loadSimulator;
        }

        @Override
        public void run() {
            running = true;
            Log.d(TAG, "Logic thread started");

            while (running) {
                try {
                    // 处理任务队列
                    LogicTask task = logicTaskQueue.take();
                    if (task != null) {
                        Log.d(TAG, "Processing task from logic thread");
                        task.execute(getLoadSimulator());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.d(TAG, "Logic thread interrupted");
                    break;
                }
            }

            Log.d(TAG, "Logic thread stopped");
        }

        public void shutdown() {
            running = false;
            this.interrupt();
        }

        /**
         * 执行资源加载
         */
        public void performResourceLoading(LoadSimulator loadSimulator) {
            try {
                // 模拟资源加载过程
                simulateResourceLoading(loadSimulator);

                // 标记加载完成
                resourcesLoaded.set(true);
                isLoading.set(false);

                // 通知主线程
                if (loadingListener != null) {
                    mainHandler.post(() -> {
                        loadingListener.onLoadingComplete();
                    });
                }

                Log.d(TAG, "Resource loading completed");
            } catch (Exception e) {
                Log.e(TAG, "Error during resource loading", e);
                isLoading.set(false);
            }
        }

        private void simulateResourceLoading(LoadSimulator loadSimulator) {
            // 模拟不同类型的资源加载
            String loadType = "com.example.launch.game";
            int loadLevel = 1; // 默认轻负载

            if (loadType.contains("light")) {
                loadLevel = 1;
            } else if (loadType.contains("medium")) {
                loadLevel = 3;
            } else if (loadType.contains("heavy")) {
                loadLevel = 6;
            }

            Log.d(TAG, "Load level: " + loadLevel + " for type: " + loadType);
            final int finalLoadLevel = loadLevel;

            // 初始化阶段
            postProgress("初始化游戏引擎...", 5);
            loadSimulator.executeLoad(20 * finalLoadLevel);

            // 着色器编译
            postProgress("编译着色器...", 15);
            for (int i = 0; i < finalLoadLevel; i++) {
                loadSimulator.simulateShaderCompilation();
                loadSimulator.executeLoad(10);
            }

            // 纹理加载
            postProgress("加载纹理资源...", 30);
            for (int i = 0; i < finalLoadLevel * 2; i++) {
                loadSimulator.simulateTextureLoading();
                loadSimulator.executeLoad(15);
            }

            // 音频资源
            postProgress("加载音频资源...", 50);
            loadSimulator.executeLoad(40 * finalLoadLevel);

            // 3D模型和场景
            postProgress("加载3D模型...", 70);
            loadSimulator.executeLoad(60 * finalLoadLevel);

            // 物理引擎
            postProgress("初始化物理引擎...", 85);
            loadSimulator.simulatePhysicsEngineInit();

            // 最终优化
            postProgress("最终优化...", 95);
            loadSimulator.executeLoad(30);

            // 完成
            postProgress("加载完成!", 100);
        }

        private void postProgress(String message, int progress) {
            if (loadingListener != null) {
                mainHandler.post(() -> {
                    loadingListener.onLoadingProgress(progress, message);
                });
            }

            // 模拟一些IO延迟
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}