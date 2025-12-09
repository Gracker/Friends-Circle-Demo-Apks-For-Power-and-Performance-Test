package com.example.wechatfriendforvideo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechatfriendforvideo.adapters.VideoFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * Light Mixed Load Activity
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：在Handler.postDelayed中执行，在帧之间执行
 */
public class LightMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LightMixedLoad";
    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private int mLoadType = LoadType.LIGHT_MIXED;
    
    private static final int MIN_TASK_INTERVAL_MS = LoadConfig.MIN_TASK_INTERVAL_MS;
    private static final int MAX_TASK_INTERVAL_MS = LoadConfig.MAX_TASK_INTERVAL_MS;
    
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mTaskIntervalRandom = new Random(LoadConfig.TASK_INTERVAL_SEED);
    
    private LoadSimulator mLoadSimulator;
    
    private boolean mIsTaskSchedulingEnabled = true;
    private boolean mIsScrolling = false;
    private int mFrameCount = 0;
    private static final int DOFRAME_INTERVAL = 4;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_mixed_load);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(VideoMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(VideoMainActivity.EXTRA_LOAD_TYPE, LoadType.LIGHT_MIXED);
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
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                    mHandler.removeCallbacksAndMessages(null);
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mFrameCount = 0;
                    mChoreographer.postFrameCallback(LightMixedLoadActivity.this);
                    scheduleNextBetweenFrameTask();
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsTaskSchedulingEnabled && mIsScrolling) {
            mFrameCount++;
            if (mFrameCount % DOFRAME_INTERVAL == 0) {
                mLoadSimulator.executeDoFrameLoad(mLoadType, "LightMixedLoad_doFrame");
            }
            mChoreographer.postFrameCallback(this);
        }
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!mIsTaskSchedulingEnabled || !mIsScrolling) return;
        int intervalMs = MIN_TASK_INTERVAL_MS + mTaskIntervalRandom.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        mHandler.postDelayed(() -> {
            if (!mIsScrolling) return;
            mLoadSimulator.executeBetweenFrameLoad(mLoadType, "LightMixedLoad_betweenFrame");
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
        Log.d(TAG, "onResume: " + LoadConfig.getDescription(mLoadType));
        mIsTaskSchedulingEnabled = true;
        mIsScrolling = false;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mIsTaskSchedulingEnabled = false;
        mHandler.removeCallbacksAndMessages(null);
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (adapter == null) {
            adapter = new VideoFriendCircleAdapter(this, recyclerView, mLoadType);
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
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
        mIsTaskSchedulingEnabled = false;
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
