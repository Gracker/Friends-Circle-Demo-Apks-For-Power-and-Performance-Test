package com.example.wechatfriendforpicasso;

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

import com.example.wechatfriendforpicasso.adapters.PerformanceFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.loadconfig.ScrollLoadGate;

import java.util.Random;

/**
 * Light Load Between Frames Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class LightLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private int mLoadType = LoadType.LIGHT_BETWEEN_FRAMES;

    private Choreographer mChoreographer;
    private Handler mHandler;

    private LoadSimulator mLoadSimulator;

    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false;

    // 帧间隔配置：使用统一配置（轻 4-6 帧）
    private Random mFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
    private int mFrameCount = 0;
    private int mNextTriggerFrame = 0;

    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_load_between_frames);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, LoadType.LIGHT_BETWEEN_FRAMES);
        }

        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();

        mLoadSimulator = new LoadSimulator();

        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());

        initScrollListener();
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

    /** 获取下一次触发的帧间隔（伪随机，固定种子可重现） */
    private int getNextFrameInterval() {
        int min = LoadConfig.getBetweenFrameMinInterval(mLoadType);
        int max = LoadConfig.getBetweenFrameMaxInterval(mLoadType);
        return min + mFrameIntervalRandom.nextInt(max - min + 1);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsBetweenFrameLoadEnabled && mIsScrolling) {
            mFrameCount++;
            if (mFrameCount >= mNextTriggerFrame) {
                mHandler.post(() -> { if (mIsScrolling) { mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "LightBetweenFrames_load"); } });
                mFrameCount = 0;
                mNextTriggerFrame = getNextFrameInterval();
            }
            mChoreographer.postFrameCallback(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PerformanceDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
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
            adapter = new PerformanceFriendCircleAdapter(this, recyclerView, mLoadType);
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
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
