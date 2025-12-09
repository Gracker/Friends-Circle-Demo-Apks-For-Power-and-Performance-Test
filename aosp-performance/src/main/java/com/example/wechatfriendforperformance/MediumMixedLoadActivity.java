package com.example.wechatfriendforperformance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
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
 * Medium Mixed Load Activity
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：在Handler.postDelayed中执行，在帧之间执行
 * 
 * 使用统一的 LoadSimulator 执行负载
 */
public class MediumMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "MediumMixedLoad";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = LoadType.MEDIUM_MIXED;
    
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mBetweenFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
    
    private LoadSimulator mLoadSimulator;
    
    private boolean mIsTaskSchedulingEnabled = true;
    private boolean mIsScrolling = false;
    private long mBetweenFrameTaskCount = 0;
    private long mDoFrameTaskCount = 0;
    private int mFrameCount = 0;
    private int mBetweenFrameCount = 0;
    private int mNextBetweenFrameTrigger = 0;
    private static final int DOFRAME_INTERVAL = 3; // 每3帧执行一次doFrame负载（中负载）
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium_mixed_load);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, LoadType.MEDIUM_MIXED);
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
                    mHandler.removeCallbacksAndMessages(null);
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mFrameCount = 0;
                    mBetweenFrameCount = 0;
                    mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
                    mChoreographer.postFrameCallback(MediumMixedLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    /**
     * Choreographer的doFrame回调
     * 混合负载：doFrame负载在帧渲染期间执行
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsTaskSchedulingEnabled && mIsScrolling) {
            mFrameCount++;
            mBetweenFrameCount++;
            
            // 每N帧执行一次doFrame负载，在帧渲染期间执行
            if (mFrameCount % DOFRAME_INTERVAL == 0) {
                mDoFrameTaskCount++;
                mLoadSimulator.executeDoFrameLoad(mLoadType, "MixedLoad_doFrame");
            }
            
            // 帧间负载：达到伪随机触发帧后，在帧间执行一次
            if (mBetweenFrameCount >= mNextBetweenFrameTrigger) {
                mBetweenFrameTaskCount++;
                mHandler.post(() -> mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MixedLoad_betweenFrame"));
                mBetweenFrameCount = 0;
                mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
            }
            
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 获取下一次帧间任务触发的帧间隔（伪随机，使用固定种子确保可重现）
     */
    private int getNextBetweenFrameInterval() {
        int min = LoadConfig.getBetweenFrameMinInterval(mLoadType);
        int max = LoadConfig.getBetweenFrameMaxInterval(mLoadType);
        return min + mBetweenFrameIntervalRandom.nextInt(max - min + 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PerformanceDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
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
        mIsTaskSchedulingEnabled = false;
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
