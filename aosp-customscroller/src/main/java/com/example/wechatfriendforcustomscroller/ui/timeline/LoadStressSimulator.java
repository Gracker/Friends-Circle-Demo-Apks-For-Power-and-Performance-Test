package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.view.Choreographer;

import com.example.wechatfriendforcustomscroller.LoadProfile;

import java.util.Random;

/**
 * 复用 wechatfriendforperformance 模块的负载配置，支持所有10种负载类型。
 * 包括：最轻负载、帧内负载(3种)、帧间负载(3种)、混合负载(3种)
 */
public final class LoadStressSimulator implements Choreographer.FrameCallback {

    private static final Random RANDOM = new Random(12345L);
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static Bitmap sBitmap;
    private static Canvas sCanvas;
    
    // 帧间任务配置
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;
    
    // 单例和状态管理
    private static LoadStressSimulator sInstance;
    private final Handler mHandler;
    private final Choreographer mChoreographer;
    private boolean mIsRunning = false;
    private boolean mIsScrolling = false; // 是否正在滚动
    private int mCurrentLoadType = LoadProfile.LOAD_TYPE_LIGHT;

    private LoadStressSimulator() {
        mHandler = new Handler(Looper.getMainLooper());
        mChoreographer = Choreographer.getInstance();
    }
    
    public static synchronized LoadStressSimulator getInstance() {
        if (sInstance == null) {
            sInstance = new LoadStressSimulator();
        }
        return sInstance;
    }

    private static void ensureCanvas() {
        if (sBitmap == null || sCanvas == null) {
            sBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            sCanvas = new Canvas(sBitmap);
        }
    }
    
    /**
     * 启动后台任务调度（用于帧间和混合负载）
     * 注意：这个方法现在只设置loadType，实际任务调度由滚动状态控制
     */
    public void startBackgroundTasks(@LoadProfile.LoadType int loadType) {
        mCurrentLoadType = loadType;
        mIsRunning = true;
        // 不再在这里启动任务调度，任务只在滚动时启动
    }
    
    /**
     * 停止后台任务调度
     */
    public void stopBackgroundTasks() {
        mIsRunning = false;
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * 通知列表开始滚动，启动负载任务
     */
    public void onScrollStart() {
        if (!mIsRunning) return;
        
        if (!mIsScrolling) {
            mIsScrolling = true;
            
            if (LoadProfile.isBetweenFramesLoad(mCurrentLoadType) || LoadProfile.isMixedLoad(mCurrentLoadType)) {
                scheduleNextBetweenFrameTask();
            }
            
            if (LoadProfile.isMixedLoad(mCurrentLoadType)) {
                mChoreographer.postFrameCallback(this);
                scheduleNextDoFrameTask();
            }
        }
    }
    
    /**
     * 通知列表停止滚动，停止负载任务
     */
    public void onScrollStop() {
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        // 只有在滚动时才继续帧回调
        if (mIsRunning && mIsScrolling && LoadProfile.isMixedLoad(mCurrentLoadType)) {
            mChoreographer.postFrameCallback(this);
        }
    }
    
    private void scheduleNextDoFrameTask() {
        if (!mIsRunning || !mIsScrolling || !LoadProfile.isMixedLoad(mCurrentLoadType)) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + RANDOM.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        mHandler.postDelayed(() -> {
            if (!mIsRunning || !mIsScrolling) return;
            
            Trace.beginSection("CustomScroll_doFrameLoad");
            executeDoFrameLoad(mCurrentLoadType);
            Trace.endSection();
            
            scheduleNextDoFrameTask();
        }, intervalMs);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!mIsRunning || !mIsScrolling) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + RANDOM.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        mHandler.postDelayed(() -> {
            if (!mIsRunning || !mIsScrolling) return;
            
            Trace.beginSection("CustomScroll_betweenFrameLoad");
            executeBetweenFrameLoad(mCurrentLoadType);
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeDoFrameLoad(@LoadProfile.LoadType int loadType) {
        int intensity;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                intensity = 1000;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                intensity = 2000;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                intensity = 4000;
                break;
            default:
                intensity = 0;
                return;
        }
        
        double sum = 0;
        for (int i = 0; i < intensity; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
    }
    
    private void executeBetweenFrameLoad(@LoadProfile.LoadType int loadType) {
        int intensity;
        boolean isHeavy = false;
        
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
                intensity = 200;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
                intensity = 400;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
                intensity = 800;
                isHeavy = true;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                intensity = 120;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                intensity = 160;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                intensity = 107;
                isHeavy = true;
                break;
            default:
                return;
        }
        
        // 数学计算
        double sum = 0;
        for (int i = 1; i <= intensity; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i);
            if (isHeavy) {
                sum += Math.log(i) + Math.tan(i * 0.01);
            }
        }
        
        // 简单图形绘制
        ensureCanvas();
        for (int i = 0; i < intensity / 10; i++) {
            PAINT.setColor(Color.rgb(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256)));
            sCanvas.drawCircle(RANDOM.nextFloat() * 200, RANDOM.nextFloat() * 200, 5 + RANDOM.nextFloat() * 10, PAINT);
        }
    }

    public static void runAdapterLoad(@LoadProfile.LoadType int loadType) {
        ensureCanvas();
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MINIMAL:
                iterations = 0;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM:
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                iterations = 800;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                iterations = 2000;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT:
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
            default:
                iterations = 5;
                break;
        }
        
        if (iterations == 0) return;
        
        Trace.beginSection("FriendCircleAdapter_simulateComputationalLoad");
        performIterations(iterations, loadType >= LoadProfile.LOAD_TYPE_MEDIUM || 
                loadType == LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES ||
                loadType == LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES ||
                loadType == LoadProfile.LOAD_TYPE_MEDIUM_MIXED ||
                loadType == LoadProfile.LOAD_TYPE_HEAVY_MIXED);
        Trace.endSection();
    }

    public static void runContinuousLoad(@LoadProfile.LoadType int loadType) {
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MINIMAL:
                iterations = 0;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM:
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                iterations = 200;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                iterations = 500;
                break;
            default:
                iterations = 0;
                break;
        }
        if (iterations <= 0) {
            return;
        }
        ensureCanvas();
        Trace.beginSection("FriendCircleAdapter_continuousLoad");
        performIterations(iterations, true);
        Trace.endSection();
    }

    private static void performIterations(int iterations, boolean includeExtraMath) {
        for (int i = 0; i < iterations; i++) {
            float x = RANDOM.nextFloat() * 100;
            float y = RANDOM.nextFloat() * 100;

            PAINT.setColor(Color.argb(
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256)
            ));
            sCanvas.drawCircle(x, y, 10, PAINT);

            if (includeExtraMath) {
                double sinValue = Math.sin(x) * Math.cos(y);
                double tanValue = Math.tan(x * 0.1);
                if (sinValue > 0.999 && tanValue > 100) {
                    PAINT.setStrokeWidth((float) (sinValue + tanValue));
                }
            }
        }
    }
}
