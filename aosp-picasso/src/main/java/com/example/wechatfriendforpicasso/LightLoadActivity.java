package com.example.wechatfriendforpicasso;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.wechatfriendforpicasso.adapters.PerformanceFriendCircleAdapter;

/**
 * Light Load Activity - 帧内轻负载
 * 使用统一的 LoadSimulator 执行负载
 */
public class LightLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LightLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private int mLoadType = LoadType.LIGHT;
    
    private Choreographer mChoreographer;
    private LoadSimulator mLoadSimulator;
    private boolean mIsEnabled = true;
    private boolean mIsScrolling = false;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_load);
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, LoadType.LIGHT);
        }

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        // 初始化负载模拟器和 Choreographer
        mLoadSimulator = new LoadSimulator();
        mChoreographer = Choreographer.getInstance();
        initScrollListener();
    }
    
    /**
     * 初始化滚动监听器
     * 只有在列表滚动时才执行帧内负载
     */
    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mChoreographer.postFrameCallback(LightLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    /**
     * Choreographer 的 doFrame 回调
     * 帧间隔由 LoadSimulator 统一控制
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsEnabled && mIsScrolling) {
            // 帧间隔由 LoadSimulator 统一控制
            mLoadSimulator.executeInFrameLoad(mLoadType, "LightLoad_doFrame");
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

        Log.d(TAG, "onResume: " + LoadType.toLabel(mLoadType));
        mIsEnabled = true;
        mIsScrolling = false;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mIsEnabled = false;
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
        mIsEnabled = false;
        
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
