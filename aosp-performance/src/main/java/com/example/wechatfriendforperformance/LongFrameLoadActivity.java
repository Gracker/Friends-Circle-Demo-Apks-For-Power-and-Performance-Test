package com.example.wechatfriendforperformance;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
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
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * 超长帧负载 Activity
 * 
 * 特点：
 * - 在滑动过程中（0-2秒内）随机出现 2-3 次超长帧
 * - 每次超长帧的执行时间是 HEAVY 负载的 10 倍
 * - 用于模拟偶发的严重卡顿场景
 */
public class LongFrameLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LongFrameLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = com.example.loadconfig.LoadType.HEAVY;
    
    // 超长帧相关成员变量
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mRandom;
    private Paint mPaint;
    private Canvas mCanvas;
    private Bitmap mBitmap;
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
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, 
                    com.example.loadconfig.LoadType.HEAVY);
        }
        
        Log.d(TAG, "onCreate: 超长帧负载模式 - 强度=" + LoadConfig.LONG_FRAME_INTENSITY + 
                " (HEAVY×10), 每" + LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS + "ms内触发" + 
                LoadConfig.LONG_FRAME_MIN_TRIGGERS + "-" + LoadConfig.LONG_FRAME_MAX_TRIGGERS + "次");

        // Process image name, remove possible file extension
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        initLongFrameComponents();
        
        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());
        initScrollListener();
        
        Log.d(TAG, "onCreate: 等待列表滚动时启动超长帧任务");
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
    
    /**
     * 开始新的滑动周期，重新计算触发时间点
     */
    private void startNewScrollCycle() {
        mScrollStartTime = System.currentTimeMillis();
        mTriggerCount = LoadConfig.getLongFrameTriggerCount();
        mTriggerTimes = LoadConfig.getLongFrameTriggerTimes(mTriggerCount);
        mCurrentTriggerIndex = 0;
        mLastTriggerTime = 0;
        
        Log.d(TAG, "startNewScrollCycle: 新滑动周期开始, 计划触发" + mTriggerCount + "次超长帧");
    }
    
    /**
     * 重置滑动周期
     */
    private void resetScrollCycle() {
        mScrollStartTime = 0;
        mCurrentTriggerIndex = 0;
    }
    
    /**
     * 初始化创建超长帧所需的组件
     */
    private void initLongFrameComponents() {
        mRandom = new Random(LoadConfig.COMPUTATION_SEED);
        mBitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsEnabled && mIsScrolling) {
            checkAndExecuteLongFrame();
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 检查是否应该执行超长帧，并执行
     */
    private void checkAndExecuteLongFrame() {
        if (mCurrentTriggerIndex >= mTriggerCount || mTriggerTimes == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - mScrollStartTime;
        
        // 检查是否到达触发时间点
        if (elapsedTime >= mTriggerTimes[mCurrentTriggerIndex]) {
            // 检查最小触发间隔
            if (currentTime - mLastTriggerTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                Log.d(TAG, "触发超长帧 #" + (mCurrentTriggerIndex + 1) + "/" + mTriggerCount + 
                        " at " + elapsedTime + "ms");
                
                Trace.beginSection("LongFrameLoad_executeLongFrame_" + (mCurrentTriggerIndex + 1));
                executeLongFrameTask();
                Trace.endSection();
                
                mLastTriggerTime = currentTime;
                mCurrentTriggerIndex++;
            }
        }
        
        // 如果超过滑动周期，重新开始
        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startNewScrollCycle();
        }
    }
    
    /**
     * 执行超长帧任务，创造 HEAVY×10 的负载
     */
    private void executeLongFrameTask() {
        int intensity = LoadConfig.LONG_FRAME_INTENSITY;
        
        // 执行大量计算和绘制操作
        for (int i = 0; i < intensity; i++) {
            float x = mRandom.nextFloat() * 800;
            float y = mRandom.nextFloat() * 800;
            
            // 随机颜色
            mPaint.setColor(Color.argb(
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256)
            ));
            
            // 绘制图形
            mCanvas.drawCircle(x, y, 10 + mRandom.nextFloat() * 20, mPaint);
            
            // 执行数学计算
            double sinValue = Math.sin(x) * Math.cos(y);
            double tanValue = Math.tan(x * 0.1);
            double sqrtValue = Math.sqrt(x * x + y * y);
            double powValue = Math.pow(x, 1.5) * Math.pow(y, 1.2);
            double logValue = Math.log(i + 1) + Math.log10(i + 1);
            
            // 更多运算增加CPU负载
            float[] matrix = new float[16];
            for (int j = 0; j < 16; j++) {
                matrix[j] = mRandom.nextFloat() * (float)(sinValue + sqrtValue);
            }
            
            // 防止编译器优化
            if (sqrtValue > 500 && powValue > 10000 && logValue > 5) {
                mPaint.setARGB((int) sqrtValue % 256, (int) powValue % 256, 
                               (int) (logValue * 50) % 256, 255);
            }
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
        } else {
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
        recyclerView.setAdapter(null);
        adapter = null;
        imageLoader = null;
        
        mIsEnabled = false;
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mCanvas = null;
        mHandler.removeCallbacksAndMessages(null);
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

