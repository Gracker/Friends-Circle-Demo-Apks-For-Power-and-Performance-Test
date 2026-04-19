package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.view.Choreographer;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * 负载模拟器，支持所有11种负载类型。
 * 使用统一的 LoadConfig 配置负载参数，帧间隔由 LoadSimulator 统一控制。
 */
public final class LoadStressSimulator implements Choreographer.FrameCallback {

    private static final String TAG = "LoadStressSimulator";

    // 单例和状态管理
    private static LoadStressSimulator sInstance;
    private final Handler mHandler;
    private final Choreographer mChoreographer;
    private LoadSimulator mLoadSimulator;
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
        mLoadSimulator = new LoadSimulator();
    }

    public static synchronized LoadStressSimulator getInstance() {
        if (sInstance == null) {
            sInstance = new LoadStressSimulator();
        }
        return sInstance;
    }

    /**
     * 启动后台任务调度
     */
    public void startBackgroundTasks(@LoadType.Type int loadType) {
        mCurrentLoadType = loadType;
        mIsRunning = true;
        // 每次开始测试都重置伪随机状态，保证可复现
        mLoadSimulator.resetRandomState();
        resetStaticLoadSimulatorState();
        resetLongFrameState();
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
            }
            // 启动帧回调，由 LoadSimulator 统一控制伪随机帧间隔
            mChoreographer.postFrameCallback(this);
        }
    }

    /**
     * 通知列表停止滚动，停止负载任务
     */
    public void onScrollStop() {
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        mChoreographer.removeFrameCallback(this);
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
        } else if (LoadType.isBetweenFramesLoad(mCurrentLoadType)) {
            // 纯帧间负载：每帧调用，由 LoadSimulator 统一控制伪随机帧间隔
            Trace.beginSection("CustomScroll_betweenFrameLoad");
            executeBetweenFrameLoad(mCurrentLoadType);
            Trace.endSection();
        } else if (LoadType.isMixedLoad(mCurrentLoadType)) {
            // 混合负载：每帧调用 doFrame 和 betweenFrame，由 LoadSimulator 统一控制伪随机帧间隔
            Trace.beginSection("CustomScroll_doFrameLoad");
            executeDoFrameLoad(mCurrentLoadType);
            Trace.endSection();

            mHandler.post(() -> {
                Trace.beginSection("CustomScroll_betweenFrameLoad");
                executeBetweenFrameLoad(mCurrentLoadType);
                Trace.endSection();
            });
        }

        mChoreographer.postFrameCallback(this);
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
        mLoadSimulator.executeLongFrameLoad("CustomScroll_longFrameLoad");
    }

    private void executeDoFrameLoad(@LoadType.Type int loadType) {
        mLoadSimulator.executeDoFrameLoad(loadType, "CustomScroll_doFrameLoad");
    }

    private void executeBetweenFrameLoad(@LoadType.Type int loadType) {
        if (LoadType.isBetweenFramesLoad(loadType)) {
            mLoadSimulator.executePureBetweenFrameLoad(loadType, "CustomScroll_betweenFrameLoad");
        } else if (LoadType.isMixedLoad(loadType)) {
            mLoadSimulator.executeBetweenFrameLoad(loadType, "CustomScroll_betweenFrameLoad");
        }
    }

    private static LoadSimulator sStaticLoadSimulator;

    private static LoadSimulator getStaticLoadSimulator() {
        if (sStaticLoadSimulator == null) {
            sStaticLoadSimulator = new LoadSimulator();
        }
        return sStaticLoadSimulator;
    }

    private static void resetStaticLoadSimulatorState() {
        if (sStaticLoadSimulator != null) {
            sStaticLoadSimulator.resetRandomState();
        }
    }

    public static void runAdapterLoad(@LoadType.Type int loadType) {
        getStaticLoadSimulator().executeInFrameLoad(loadType, "CustomScrollAdapter_bindLoad");
    }

    public static void runContinuousLoad(@LoadType.Type int loadType) {
        getStaticLoadSimulator().executeInFrameLoad(loadType, "CustomScrollAdapter_continuousLoad");
    }
}
