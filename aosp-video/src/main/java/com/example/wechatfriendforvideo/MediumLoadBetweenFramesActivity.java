package com.example.wechatfriendforvideo;

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

import com.example.wechatfriendforvideo.adapters.VideoFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * Medium Load Between Frames Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class MediumLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private int mLoadType = LoadType.MEDIUM_BETWEEN_FRAMES;
    
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mTaskDecisionRandom = new Random(LoadConfig.COMPUTATION_SEED);
    private float mTaskExecutionProbability = LoadConfig.MEDIUM_TASK_PROBABILITY;
    
    private LoadSimulator mLoadSimulator;
    
    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium_load_between_frames);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(VideoMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(VideoMainActivity.EXTRA_LOAD_TYPE, LoadType.MEDIUM_BETWEEN_FRAMES);
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
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mChoreographer.postFrameCallback(MediumLoadBetweenFramesActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsBetweenFrameLoadEnabled && mIsScrolling) {
            if (mTaskDecisionRandom.nextFloat() < mTaskExecutionProbability) {
                mHandler.post(() -> {
                    if (mIsScrolling) {
                        mLoadSimulator.executePureBetweenFrameLoad(mLoadType, "MediumBetweenFrames_load");
                    }
                });
            }
            mChoreographer.postFrameCallback(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().getFriendCircleBeans(mLoadType));
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
        mIsBetweenFrameLoadEnabled = false;
        mIsScrolling = false;
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
