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
import com.example.loadconfig.LoadScheduler;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * Medium Mixed Load Activity
 * - doFrame负载：在Choreographer的doFrame回调中执行，影响帧渲染
 * - 帧间负载：在Handler.postDelayed中执行，在帧之间执行
 * 
 * 使用统一的 LoadSimulator 执行负载
 */
public class MediumMixedLoadActivity extends AppCompatActivity {

    private static final String TAG = "MediumMixedLoad";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = LoadType.MEDIUM_MIXED;

    private LoadSimulator mLoadSimulator;
    private LoadScheduler mLoadScheduler;

    // Removed mIsTaskSchedulingEnabled, mIsScrolling, mScrollListener

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

        // Use LoadScheduler
        mLoadScheduler = new LoadScheduler();
        mLoadScheduler.attach(this, recyclerView, mLoadType);

        Log.d(TAG, "onCreate: LoadScheduler attached");
    }

    // Removed initScrollListener() and doFrame() manually methods

    @Override
    protected void onResume() {
        super.onResume();
        PerformanceDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
        Log.d(TAG, "onResume: " + LoadConfig.getDescription(mLoadType));
    }

    @Override
    protected void onPause() {
        super.onPause();
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

        // Removed scroll listener logic, LoadScheduler handles it
        // Removed manual handler callbacks logic

        recyclerView.setAdapter(null);
        adapter = null;
        imageLoader = null;

        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}
