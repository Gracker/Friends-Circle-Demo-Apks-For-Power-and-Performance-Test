package com.example.wechatfriendforpicasso;

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

import com.example.wechatfriendforpicasso.adapters.PerformanceFriendCircleAdapter;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * Medium Mixed Load Activity
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：在Handler.postDelayed中执行，在帧之间执行
 */
public class MediumMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "MediumMixedLoad";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private int mLoadType = LoadType.MEDIUM_MIXED;
    
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mBetweenFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
    
    private LoadSimulator mLoadSimulator;
    
    private boolean mIsTaskSchedulingEnabled = true;
    private boolean mIsScrolling = false;
    private int mFrameCount = 0;
    private int mBetweenFrameCount = 0;
    private int mNextBetweenFrameTrigger = 0;
    private static final int DOFRAME_INTERVAL = 3;
    
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
                    mBetweenFrameCount = 0;
                    mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
                    mChoreographer.postFrameCallback(MediumMixedLoadActivity.this);
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
            if (mFrameCount % DOFRAME_INTERVAL == 0) {
                mLoadSimulator.executeDoFrameLoad(mLoadType, "MediumMixedLoad_doFrame");
            }
            if (mBetweenFrameCount >= mNextBetweenFrameTrigger) {
                mHandler.post(() -> mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MediumMixedLoad_betweenFrame"));
                mBetweenFrameCount = 0;
                mNextBetweenFrameTrigger = getNextBetweenFrameInterval();
            }
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /** 获取下一次帧间任务触发的帧间隔（伪随机，固定种子可重现） */
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
        mIsTaskSchedulingEnabled = false;
        mIsScrolling = false;
        mHandler.removeCallbacksAndMessages(null);
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
