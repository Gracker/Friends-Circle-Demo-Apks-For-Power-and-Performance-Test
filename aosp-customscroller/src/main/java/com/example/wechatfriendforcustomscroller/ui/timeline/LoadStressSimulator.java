package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.view.Choreographer;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * 负载模拟器，支持所有11种负载类型。
 * 使用统一的 LoadConfig 配置负载参数。
 */
public final class LoadStressSimulator implements Choreographer.FrameCallback {

    private static final String TAG = "LoadStressSimulator";
    
    // 使用 LoadConfig 中的配置
    private static final Random RANDOM = new Random(LoadConfig.COMPUTATION_SEED);
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static Bitmap sBitmap;
    private static Canvas sCanvas;
    
    // 单例和状态管理
    private static LoadStressSimulator sInstance;
    private final Handler mHandler;
    private final Choreographer mChoreographer;
    private boolean mIsRunning = false;
    private boolean mIsScrolling = false;
    private int mCurrentLoadType = LoadType.LIGHT;
    
    // 超长帧相关
    private long mScrollStartTime = 0;
    private int mLongFrameTriggerCount = 0;
    private int mCurrentLongFrameIndex = 0;
    private long[] mLongFrameTriggerTimes;
    private long mLastLongFrameTime = 0;

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
            int size = LoadConfig.getBitmapSize(LoadType.MEDIUM);
            sBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            sCanvas = new Canvas(sBitmap);
        }
    }
    
    /**
     * 启动后台任务调度
     */
    public void startBackgroundTasks(@LoadType.Type int loadType) {
        mCurrentLoadType = loadType;
        mIsRunning = true;
    }
    
    /**
     * 停止后台任务调度
     */
    public void stopBackgroundTasks() {
        mIsRunning = false;
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        resetLongFrameState();
    }
    
    /**
     * 通知列表开始滚动，启动负载任务
     */
    public void onScrollStart() {
        if (!mIsRunning) return;
        
        if (!mIsScrolling) {
            mIsScrolling = true;
            
            if (LoadType.isLongFrameLoad(mCurrentLoadType)) {
                startLongFrameCycle();
                mChoreographer.postFrameCallback(this);
            } else if (LoadType.isBetweenFramesLoad(mCurrentLoadType) || LoadType.isMixedLoad(mCurrentLoadType)) {
                scheduleNextBetweenFrameTask();
                if (LoadType.isMixedLoad(mCurrentLoadType)) {
                    mChoreographer.postFrameCallback(this);
                    scheduleNextDoFrameTask();
                }
            }
        }
    }
    
    /**
     * 通知列表停止滚动，停止负载任务
     */
    public void onScrollStop() {
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        resetLongFrameState();
    }
    
    private void startLongFrameCycle() {
        mScrollStartTime = System.currentTimeMillis();
        mLongFrameTriggerCount = LoadConfig.getLongFrameTriggerCount();
        mLongFrameTriggerTimes = LoadConfig.getLongFrameTriggerTimes(mLongFrameTriggerCount);
        mCurrentLongFrameIndex = 0;
        mLastLongFrameTime = 0;
        Log.d(TAG, "startLongFrameCycle: 计划触发" + mLongFrameTriggerCount + "次超长帧");
    }
    
    private void resetLongFrameState() {
        mScrollStartTime = 0;
        mCurrentLongFrameIndex = 0;
        mLongFrameTriggerTimes = null;
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsRunning || !mIsScrolling) return;
        
        if (LoadType.isLongFrameLoad(mCurrentLoadType)) {
            checkAndExecuteLongFrame();
            mChoreographer.postFrameCallback(this);
        } else if (LoadType.isMixedLoad(mCurrentLoadType)) {
            mChoreographer.postFrameCallback(this);
        }
    }
    
    private void checkAndExecuteLongFrame() {
        if (mCurrentLongFrameIndex >= mLongFrameTriggerCount || mLongFrameTriggerTimes == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - mScrollStartTime;
        
        if (elapsedTime >= mLongFrameTriggerTimes[mCurrentLongFrameIndex]) {
            if (currentTime - mLastLongFrameTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                Log.d(TAG, "触发超长帧 #" + (mCurrentLongFrameIndex + 1) + "/" + mLongFrameTriggerCount);
                
                Trace.beginSection("CustomScroll_longFrameLoad_" + (mCurrentLongFrameIndex + 1));
                executeLongFrameLoad();
                Trace.endSection();
                
                mLastLongFrameTime = currentTime;
                mCurrentLongFrameIndex++;
            }
        }
        
        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startLongFrameCycle();
        }
    }
    
    private void executeLongFrameLoad() {
        int intensity = LoadConfig.LONG_FRAME_INTENSITY;
        ensureCanvas();
        
        for (int i = 0; i < intensity; i++) {
            float x = RANDOM.nextFloat() * 400;
            float y = RANDOM.nextFloat() * 400;
            
            PAINT.setColor(Color.argb(
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256)
            ));
            
            sCanvas.drawCircle(x, y, 10 + RANDOM.nextFloat() * 20, PAINT);
            
            double sinValue = Math.sin(x) * Math.cos(y);
            double sqrtValue = Math.sqrt(x * x + y * y);
            double logValue = Math.log(i + 1) + Math.log10(i + 1);
            
            if (sqrtValue > 300 && logValue > 5) {
                PAINT.setARGB((int) sqrtValue % 256, (int) logValue * 25 % 256, 
                               (int) (sinValue * 100) % 256, 255);
            }
        }
    }
    
    private void scheduleNextDoFrameTask() {
        if (!mIsRunning || !mIsScrolling || !LoadType.isMixedLoad(mCurrentLoadType)) return;
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + 
                RANDOM.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
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
        if (!LoadType.isBetweenFramesLoad(mCurrentLoadType) && !LoadType.isMixedLoad(mCurrentLoadType)) return;
        
        int intervalMs = LoadConfig.MIN_TASK_INTERVAL_MS + 
                RANDOM.nextInt(LoadConfig.MAX_TASK_INTERVAL_MS - LoadConfig.MIN_TASK_INTERVAL_MS);
        
        mHandler.postDelayed(() -> {
            if (!mIsRunning || !mIsScrolling) return;
            
            Trace.beginSection("CustomScroll_betweenFrameLoad");
            executeBetweenFrameLoad(mCurrentLoadType);
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeDoFrameLoad(@LoadType.Type int loadType) {
        // 使用 LoadConfig 获取强度
        int intensity = LoadConfig.getDoFrameIntensity(convertToLoadType(loadType));
        if (intensity == 0) return;
        
        double sum = 0;
        for (int i = 0; i < intensity; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
    }
    
    private void executeBetweenFrameLoad(@LoadType.Type int loadType) {
        int intensity;
        boolean isHeavy = false;
        
        // 使用 LoadConfig 获取强度
        int convertedType = convertToLoadType(loadType);
        if (LoadType.isBetweenFramesLoad(convertedType)) {
            intensity = LoadConfig.getBetweenFrameIntensity(convertedType);
            isHeavy = (convertedType == LoadType.HEAVY_BETWEEN_FRAMES);
        } else if (LoadType.isMixedLoad(convertedType)) {
            intensity = LoadConfig.getMixedBetweenFrameIntensity(convertedType);
            isHeavy = (convertedType == LoadType.HEAVY_MIXED);
        } else {
            return;
        }
        
        double sum = 0;
        for (int i = 1; i <= intensity; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i);
            if (isHeavy) {
                sum += Math.log(i) + Math.tan(i * 0.01);
            }
        }
        
        ensureCanvas();
        int drawCount = Math.min(intensity / 10, 100);
        for (int i = 0; i < drawCount; i++) {
            PAINT.setColor(Color.rgb(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256)));
            sCanvas.drawCircle(RANDOM.nextFloat() * 200, RANDOM.nextFloat() * 200, 5 + RANDOM.nextFloat() * 10, PAINT);
        }
    }
    
    /**
     * 将 LoadProfile 类型转换为 LoadType
     */
    private int convertToLoadType(int loadProfileType) {
        switch (loadProfileType) {
            case LoadType.MINIMAL: return LoadType.MINIMAL;
            case LoadType.LIGHT: return LoadType.LIGHT;
            case LoadType.MEDIUM: return LoadType.MEDIUM;
            case LoadType.HEAVY: return LoadType.HEAVY;
            case LoadType.LIGHT_BETWEEN_FRAMES: return LoadType.LIGHT_BETWEEN_FRAMES;
            case LoadType.MEDIUM_BETWEEN_FRAMES: return LoadType.MEDIUM_BETWEEN_FRAMES;
            case LoadType.HEAVY_BETWEEN_FRAMES: return LoadType.HEAVY_BETWEEN_FRAMES;
            case LoadType.LIGHT_MIXED: return LoadType.LIGHT_MIXED;
            case LoadType.MEDIUM_MIXED: return LoadType.MEDIUM_MIXED;
            case LoadType.HEAVY_MIXED: return LoadType.HEAVY_MIXED;
            case LoadType.LONG_FRAME: return LoadType.LONG_FRAME;
            default: return LoadType.MINIMAL;
        }
    }

    public static void runAdapterLoad(@LoadType.Type int loadType) {
        ensureCanvas();
        
        // 使用 LoadConfig 获取强度
        int intensity;
        switch (loadType) {
            case LoadType.MINIMAL:
                intensity = 0;
                break;
            case LoadType.LIGHT:
            case LoadType.LIGHT_BETWEEN_FRAMES:
            case LoadType.LIGHT_MIXED:
                intensity = LoadConfig.IN_FRAME_LIGHT_INTENSITY;
                break;
            case LoadType.MEDIUM:
            case LoadType.MEDIUM_BETWEEN_FRAMES:
            case LoadType.MEDIUM_MIXED:
                intensity = LoadConfig.IN_FRAME_MEDIUM_INTENSITY;
                break;
            case LoadType.HEAVY:
            case LoadType.HEAVY_BETWEEN_FRAMES:
            case LoadType.HEAVY_MIXED:
                intensity = LoadConfig.IN_FRAME_HEAVY_INTENSITY;
                break;
            case LoadType.LONG_FRAME:
                // 超长帧负载由 doFrame 回调处理，Adapter 使用 HEAVY 强度
                intensity = LoadConfig.IN_FRAME_HEAVY_INTENSITY;
                break;
            default:
                intensity = 0;
                break;
        }
        
        if (intensity == 0) return;
        
        Trace.beginSection("FriendCircleAdapter_simulateComputationalLoad");
        performIterations(intensity, loadType >= LoadType.MEDIUM);
        Trace.endSection();
    }

    public static void runContinuousLoad(@LoadType.Type int loadType) {
        int intensity;
        switch (loadType) {
            case LoadType.MINIMAL:
            case LoadType.LIGHT:
            case LoadType.LIGHT_BETWEEN_FRAMES:
            case LoadType.LIGHT_MIXED:
                intensity = 0;
                break;
            case LoadType.MEDIUM:
            case LoadType.MEDIUM_BETWEEN_FRAMES:
            case LoadType.MEDIUM_MIXED:
                intensity = LoadConfig.IN_FRAME_MEDIUM_INTENSITY;
                break;
            case LoadType.HEAVY:
            case LoadType.HEAVY_BETWEEN_FRAMES:
            case LoadType.HEAVY_MIXED:
            case LoadType.LONG_FRAME:
                intensity = LoadConfig.IN_FRAME_HEAVY_INTENSITY;
                break;
            default:
                intensity = 0;
                break;
        }
        if (intensity <= 0) return;
        
        ensureCanvas();
        Trace.beginSection("FriendCircleAdapter_continuousLoad");
        performIterations(intensity, true);
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
