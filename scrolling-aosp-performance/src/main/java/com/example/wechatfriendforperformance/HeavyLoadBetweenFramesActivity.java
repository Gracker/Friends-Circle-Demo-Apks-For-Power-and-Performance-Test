package com.example.wechatfriendforperformance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.example.wechatfriendforperformance.adapters.PerformanceFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * Heavy Load Between Frames Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class HeavyLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyBetweenFrames";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = LoadType.HEAVY_BETWEEN_FRAMES;

    private Choreographer mChoreographer;
    private Handler mHandler;

    private LoadSimulator mLoadSimulator;

    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false;

    // 帧间隔配置：使用 LoadConfig 中的统一配置（重负载 2-4 帧）
    private Random mFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
    private int mFrameCount = 0;
    private int mNextTriggerFrame = 0;

    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load_between_frames);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, LoadType.HEAVY_BETWEEN_FRAMES);
        }

        imageLoader = Glide.with(this).asDrawable().apply(new RequestOptions().centerCrop());

        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();

        mLoadSimulator = new LoadSimulator();

        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());

        initScrollListener();
        Log.d(TAG, "onCreate: 等待列表滚动时启动负载任务");
    }

    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mFrameCount = 0;
                    mNextTriggerFrame = getNextFrameInterval();
                    mChoreographer.postFrameCallback(HeavyLoadBetweenFramesActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    /**
     * 获取下一次触发的帧间隔（伪随机，使用固定种子确保可重现）
     */
    private int getNextFrameInterval() {
        int min = LoadConfig.getBetweenFrameMinInterval(mLoadType);
        int max = LoadConfig.getBetweenFrameMaxInterval(mLoadType);
        return min + mFrameIntervalRandom.nextInt(max - min + 1);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsBetweenFrameLoadEnabled && mIsScrolling) {
            mFrameCount++;

            // 当达到下一次触发帧时，执行帧间负载（使用 Handler.post 确保在帧间执行）
            if (mFrameCount >= mNextTriggerFrame) {
                mHandler.post(() -> {
                    mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "BetweenFrames_load");
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
        PerformanceDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
        Log.d(TAG, "onResume: " + LoadType.toLabel(mLoadType));
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
        imageLoader = null;
        mIsBetweenFrameLoadEnabled = false;
        mIsScrolling = false;
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
