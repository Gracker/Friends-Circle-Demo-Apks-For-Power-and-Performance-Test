package com.example.wechatfriendforperformance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;

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
 * 超长帧负载 Activity
 * 使用统一的 LoadSimulator 执行负载
 */
public class LongFrameLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LongFrameLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = LoadType.LONG_FRAME;

    private Choreographer mChoreographer;
    private Handler mHandler;
    private LoadSimulator mLoadSimulator;
    private boolean mIsEnabled = true;
    private boolean mIsScrolling = false;

    // 超长帧触发控制
    private long mScrollStartTime = 0;
    private int mTriggerCount = 0;
    private int mCurrentTriggerIndex = 0;
    private long[] mTriggerTimes;
    private long mLastTriggerTime = 0;

    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, LoadType.LONG_FRAME);
        }

        Log.d(TAG, "onCreate: 超长帧负载模式");

        imageLoader = Glide.with(this).asDrawable().apply(new RequestOptions().centerCrop());

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
                    resetScrollCycle();
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    startNewScrollCycle();
                    mChoreographer.postFrameCallback(LongFrameLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    private void startNewScrollCycle() {
        mScrollStartTime = System.currentTimeMillis();
        mTriggerCount = LoadConfig.getLongFrameTriggerCount();
        mTriggerTimes = LoadConfig.getLongFrameTriggerTimes(mTriggerCount);
        mCurrentTriggerIndex = 0;
        mLastTriggerTime = 0;
        Log.d(TAG, "startNewScrollCycle: 计划触发" + mTriggerCount + "次超长帧");
    }

    private void resetScrollCycle() {
        mScrollStartTime = 0;
        mCurrentTriggerIndex = 0;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsEnabled && mIsScrolling) {
            checkAndExecuteLongFrame();
            mChoreographer.postFrameCallback(this);
        }
    }

    private void checkAndExecuteLongFrame() {
        if (mCurrentTriggerIndex >= mTriggerCount || mTriggerTimes == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - mScrollStartTime;

        if (elapsedTime >= mTriggerTimes[mCurrentTriggerIndex]) {
            if (currentTime - mLastTriggerTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                Log.d(TAG, "触发超长帧 #" + (mCurrentTriggerIndex + 1) + "/" + mTriggerCount);
                mLoadSimulator.executeLongFrameLoad("LongFrameLoad_execute_" + (mCurrentTriggerIndex + 1));
                mLastTriggerTime = currentTime;
                mCurrentTriggerIndex++;
            }
        }

        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startNewScrollCycle();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PerformanceDataCenter.getInstance().clearCachedData();
        if (adapter != null) {
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }
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
        imageLoader = null;
        mIsEnabled = false;
        mHandler.removeCallbacksAndMessages(null);
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
