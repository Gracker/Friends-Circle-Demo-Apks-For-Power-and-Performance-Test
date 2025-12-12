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

/**
 * Heavy Mixed Load Activity, combines heavy doFrame load + heavy between-frame load
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：在Handler.postDelayed中执行，在帧之间执行
 * 
 * 使用统一的 LoadSimulator 执行负载，便于维护和修改
 */
public class HeavyMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyMixedLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = LoadType.HEAVY_MIXED;

    // 调度器
    private Choreographer mChoreographer;
    private Handler mHandler;

    // 统一负载模拟器
    private LoadSimulator mLoadSimulator;

    // 控制变量
    private boolean mIsTaskSchedulingEnabled = true;
    private boolean mIsScrolling = false;

    // 滚动监听器
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_mixed_load);

        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, LoadType.HEAVY_MIXED);
        }

        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();

        // 初始化负载模拟器
        mLoadSimulator = new LoadSimulator();

        // 注册Choreographer帧回调
        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());

        // 初始化滚动监听器
        initScrollListener();

        Log.d(TAG, "onCreate: 等待列表滚动时启动负载任务");
    }

    /**
     * 初始化滚动监听器
     * 只有在列表滚动时才执行帧间负载和doFrame负载
     */
    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                    mHandler.removeCallbacksAndMessages(null);
                    Log.d(TAG, "列表停止滚动，停止负载任务");
                } else {
                    if (!mIsScrolling) {
                        mIsScrolling = true;
                        mChoreographer.postFrameCallback(HeavyMixedLoadActivity.this);
                        Log.d(TAG, "列表开始滚动，启动负载任务");
                    }
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    /**
     * Choreographer的doFrame回调
     * 帧间隔由 LoadSimulator 统一控制
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsTaskSchedulingEnabled && mIsScrolling) {
            // 帧间隔由 LoadSimulator 统一控制
            mLoadSimulator.executeDoFrameLoad(mLoadType, "MixedLoad_doFrame");
            mHandler.post(() -> mLoadSimulator.executeBetweenFrameLoad(mLoadType, "MixedLoad_betweenFrame"));
            mChoreographer.postFrameCallback(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 清空缓存，确保使用正确的负载类型
        PerformanceDataCenter.getInstance().clearCachedData();

        // 确保数据已根据正确的负载类型生成
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
        Log.d(TAG, "onPause: 已暂停Task调度");
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (adapter == null) {
            adapter = new PerformanceFriendCircleAdapter(this, recyclerView, mLoadType);
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        } else {
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

        // 释放负载模拟器资源
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }

        Log.d(TAG, "onDestroy: 资源已清理");
    }
}
