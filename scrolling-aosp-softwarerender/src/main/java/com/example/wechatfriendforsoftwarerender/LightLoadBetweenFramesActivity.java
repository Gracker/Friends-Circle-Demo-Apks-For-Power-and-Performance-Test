package com.example.wechatfriendforsoftwarerender;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechatfriendforsoftwarerender.adapters.SoftwareRenderFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.loadconfig.ScrollLoadGate;

import java.util.Random;

/**
 * Light Load Between Frames Activity
 * 使用统一的 LoadSimulator 执行负载
 * 帧间隔使用伪随机，通过 LoadConfig 配置
 */
public class LightLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private RecyclerView recyclerView;
    private SoftwareRenderFriendCircleAdapter adapter;
    private int mLoadType = LoadType.LIGHT_BETWEEN_FRAMES;

    private Choreographer mChoreographer;
    private Handler mHandler;

    // 伪随机帧间隔
    private Random mFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
    private int mFrameCount = 0;
    private int mNextTriggerFrame;

    private LoadSimulator mLoadSimulator;

    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false;

    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_load_between_frames);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SoftwareRenderMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(SoftwareRenderMainActivity.EXTRA_LOAD_TYPE, LoadType.LIGHT_BETWEEN_FRAMES);
        }

        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();

        mLoadSimulator = new LoadSimulator();

        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());

        // 初始化第一个触发帧
        mNextTriggerFrame = getNextFrameInterval();

        initScrollListener();
    }

    /**
     * 获取下一个帧间隔（伪随机）
     */
    private int getNextFrameInterval() {
        int minInterval = LoadConfig.getBetweenFrameMinInterval(mLoadType);
        int maxInterval = LoadConfig.getBetweenFrameMaxInterval(mLoadType);
        return minInterval + mFrameIntervalRandom.nextInt(maxInterval - minInterval + 1);
    }

    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (!ScrollLoadGate.isInertiaState(newState)) {
                    mIsScrolling = false;
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mFrameCount = 0;
                    mNextTriggerFrame = getNextFrameInterval();
                    mChoreographer.postFrameCallback(LightLoadBetweenFramesActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsBetweenFrameLoadEnabled && mIsScrolling) {
            mFrameCount++;
            if (mFrameCount >= mNextTriggerFrame) {
                mHandler.post(() -> {
                    if (mIsScrolling) {
                        mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "LightBetweenFrames_load");
                    }
                });
                mFrameCount = 0;
                mNextTriggerFrame = getNextFrameInterval();
            }
            mChoreographer.postFrameCallback(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SoftwareRenderDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(SoftwareRenderDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
        mIsBetweenFrameLoadEnabled = true;
        mIsScrolling = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsBetweenFrameLoadEnabled = false;
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (adapter == null) {
            adapter = new SoftwareRenderFriendCircleAdapter(this, recyclerView, mLoadType);
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            adapter.setFriendCircleBeans(SoftwareRenderDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recyclerView != null && mScrollListener != null) {
            recyclerView.removeOnScrollListener(mScrollListener);
        }
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
        recyclerView.setAdapter(null);
        adapter = null;
        mIsBetweenFrameLoadEnabled = false;
        mIsScrolling = false;
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
