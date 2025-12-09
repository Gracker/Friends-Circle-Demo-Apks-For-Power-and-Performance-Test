package com.example.wechatfriendforsoftwarerender;

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

import com.example.wechatfriendforsoftwarerender.adapters.SoftwareRenderFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * Heavy Mixed Load Activity
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：通过Handler.post在doFrame之后执行，使用伪随机帧间隔
 */
public class HeavyMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyMixedLoad";
    private RecyclerView recyclerView;
    private SoftwareRenderFriendCircleAdapter adapter;
    private int mLoadType = LoadType.HEAVY_MIXED;
    
    private Choreographer mChoreographer;
    private Handler mHandler;
    
    // 帧间负载的伪随机帧间隔
    private Random mBetweenFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
    private int mBetweenFrameCount = 0;
    private int mNextBetweenFrameTrigger;
    
    private LoadSimulator mLoadSimulator;
    
    private boolean mIsTaskSchedulingEnabled = true;
    private boolean mIsScrolling = false;
    private int mFrameCount = 0;
    private static final int DOFRAME_INTERVAL = 2;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_mixed_load);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SoftwareRenderMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(SoftwareRenderMainActivity.EXTRA_LOAD_TYPE, LoadType.HEAVY_MIXED);
        }

        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        mLoadSimulator = new LoadSimulator();
        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());
        
        // 初始化第一个帧间负载触发帧
        mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
        
        initScrollListener();
    }
    
    /**
     * 获取下一个帧间负载的帧间隔（伪随机）
     */
    private int getNextBetweenFrameInterval() {
        int minInterval = LoadConfig.getBetweenFrameMinInterval(mLoadType);
        int maxInterval = LoadConfig.getBetweenFrameMaxInterval(mLoadType);
        return minInterval + mBetweenFrameIntervalRandom.nextInt(maxInterval - minInterval + 1);
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
                    mBetweenFrameCount = 0;
                    mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
                    mChoreographer.postFrameCallback(HeavyMixedLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsTaskSchedulingEnabled && mIsScrolling) {
            mFrameCount++;
            mBetweenFrameCount++;
            
            // 帧内负载：固定间隔
            if (mFrameCount % DOFRAME_INTERVAL == 0) {
                mLoadSimulator.executeDoFrameLoad(mLoadType, "HeavyMixedLoad_doFrame");
            }
            
            // 帧间负载：伪随机间隔，通过 Handler.post 执行
            if (mBetweenFrameCount >= mNextBetweenFrameTrigger) {
                mHandler.post(() -> {
                    if (mIsScrolling) {
                        mLoadSimulator.executeBetweenFrameLoad(mLoadType, "HeavyMixedLoad_betweenFrame");
                    }
                });
                mBetweenFrameCount = 0;
                mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
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
        mIsTaskSchedulingEnabled = false;
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
