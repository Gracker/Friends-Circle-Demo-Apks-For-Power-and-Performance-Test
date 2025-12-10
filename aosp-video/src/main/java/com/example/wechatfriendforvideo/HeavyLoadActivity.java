package com.example.wechatfriendforvideo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.View;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.wechatfriendforvideo.adapters.VideoFriendCircleAdapter;

/**
 * Heavy Load Activity - 帧内高负载
 * 使用统一的 LoadSimulator 执行负载
 */
public class HeavyLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyLoadActivity";
    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = LoadType.HEAVY;
    
    private Choreographer mChoreographer;
    private LoadSimulator mLoadSimulator;
    private boolean mIsEnabled = true;
    private boolean mIsScrolling = false;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load);
        
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(VideoMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(VideoMainActivity.EXTRA_LOAD_TYPE, LoadType.HEAVY);
        }

        imageLoader = Glide.with(this).asDrawable().apply(new RequestOptions().centerCrop());

        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        mLoadSimulator = new LoadSimulator();
        mChoreographer = Choreographer.getInstance();
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
                    mChoreographer.postFrameCallback(HeavyLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsEnabled && mIsScrolling) {
            mLoadSimulator.executeInFrameLoad(mLoadType, "HeavyLoad_doFrame");
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
        imageLoader = null;
        mIsEnabled = false;
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
