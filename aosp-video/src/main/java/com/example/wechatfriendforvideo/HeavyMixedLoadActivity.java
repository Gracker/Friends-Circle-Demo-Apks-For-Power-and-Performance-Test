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

/**
 * Heavy Mixed Load Activity
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：在Handler.postDelayed中执行，在帧之间执行
 */
public class HeavyMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyMixedLoad";
    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private int mLoadType = LoadType.HEAVY_MIXED;
    
    private Choreographer mChoreographer;
    private Handler mHandler;
    
    private LoadSimulator mLoadSimulator;
    
    private boolean mIsTaskSchedulingEnabled = true;
    private boolean mIsScrolling = false;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_mixed_load);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(VideoMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(VideoMainActivity.EXTRA_LOAD_TYPE, LoadType.HEAVY_MIXED);
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
                    mChoreographer.postFrameCallback(HeavyMixedLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsTaskSchedulingEnabled && mIsScrolling) {
            // 帧间隔由 LoadSimulator 统一控制
            mLoadSimulator.executeDoFrameLoad(mLoadType, "HeavyMixedLoad_doFrame");
            mHandler.post(() -> mLoadSimulator.executeBetweenFrameLoad(mLoadType, "HeavyMixedLoad_betweenFrame"));
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
