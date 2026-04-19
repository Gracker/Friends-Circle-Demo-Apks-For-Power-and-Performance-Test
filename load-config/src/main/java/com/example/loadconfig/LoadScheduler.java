package com.example.loadconfig;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 统一的负载调度器
 * 
 * 负责管理 Frame Loop 相关的负载调度，包括：
 * 1. Choreographer doFrame 回调 (帧首负载)
 * 2. Handler post/postDelayed (帧间负载)
 * 3. 滚动状态感知 (只在滚动时触发)
 * 4. 生命周期管理 (自动暂停/恢复)
 */
public class LoadScheduler implements DefaultLifecycleObserver, Choreographer.FrameCallback {

    private static final String TAG = "LoadScheduler";

    private final LoadSimulator mLoadSimulator;
    private final Handler mHandler;
    private final Choreographer mChoreographer;

    private RecyclerView mRecyclerView;
    private int mLoadType;
    private String mInFrameTraceTag;
    private String mBetweenFrameTraceTag;

    private boolean mIsActive = false;
    private boolean mIsScrolling = false;
    private boolean mIsLifecycleResumed = false;

    private RecyclerView.OnScrollListener mScrollListener;

    public LoadScheduler() {
        mLoadSimulator = new LoadSimulator();
        mHandler = new Handler(Looper.getMainLooper());
        mChoreographer = Choreographer.getInstance();
    }

    /**
     * 绑定到 RecyclerView 和 Lifecycle
     * 
     * @param owner        LifecycleOwner (通常是 Activity)
     * @param recyclerView 目标 RecyclerView
     * @param loadType     负载类型
     */
    public void attach(@NonNull LifecycleOwner owner, @NonNull RecyclerView recyclerView, @LoadType.Type int loadType) {
        mRecyclerView = recyclerView;
        mLoadType = loadType;

        mInFrameTraceTag = "Load_" + LoadType.toLabelEn(loadType) + "_DoFrame";
        mBetweenFrameTraceTag = "Load_" + LoadType.toLabelEn(loadType) + "_BetweenFrame";

        owner.getLifecycle().addObserver(this);

        setupScrollListener();
    }

    private void setupScrollListener() {
        if (mRecyclerView == null)
            return;

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    setScrolling(false);
                } else {
                    setScrolling(true);
                }
            }
        };
        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    private void setScrolling(boolean scrolling) {
        if (mIsScrolling == scrolling)
            return;
        mIsScrolling = scrolling;
        updateActiveState();
    }

    private void updateActiveState() {
        // 只有当 (生命周期Resumed) AND (正在滚动 OR 需要强制执行) 时才激活
        // 目前策略：只在滚动时执行负载，模拟列表滑动卡顿
        boolean shouldBeActive = mIsLifecycleResumed && mIsScrolling;

        if (mIsActive == shouldBeActive)
            return;

        mIsActive = shouldBeActive;
        if (mIsActive) {
            Log.d(TAG, "Starting load scheduling for type: " + mLoadType);
            mChoreographer.postFrameCallback(this);
        } else {
            Log.d(TAG, "Stopping load scheduling");
            mChoreographer.removeFrameCallback(this);
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsActive)
            return;

        // 1. 执行 doFrame 负载 (帧首)
        if (LoadType.needsDoFrameTask(mLoadType)) {
            mLoadSimulator.executeDoFrameLoad(mLoadType, mInFrameTraceTag);
        }

        // 2. 调度 帧间 负载
        if (LoadType.needsBetweenFrameTask(mLoadType)) {
            // 使用 Handler.post 将任务排在 MessageQueue 后面，尽量在 doFrame 处理完后执行
            mHandler.post(() -> {
                if (mIsActive) {
                    mLoadSimulator.executeBetweenFrameLoad(mLoadType, mBetweenFrameTraceTag);
                }
            });
        }

        // 3. 请求下一帧
        mChoreographer.postFrameCallback(this);
    }

    // ==================== Lifecycle Events ====================

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mIsLifecycleResumed = true;
        // 每次恢复时重置伪随机状态，保证测试可复现
        mLoadSimulator.resetRandomState();
        // 重置调度状态
        updateActiveState();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mIsLifecycleResumed = false;
        updateActiveState();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        owner.getLifecycle().removeObserver(this);
        if (mRecyclerView != null && mScrollListener != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
        }
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
        }
        mHandler.removeCallbacksAndMessages(null);
    }
}
