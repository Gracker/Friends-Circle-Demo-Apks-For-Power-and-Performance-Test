package com.example.wechatfriendforvideo;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Trace;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.example.wechatfriendforvideo.adapters.VideoFriendCircleAdapter;
import com.example.wechatfriendforvideo.beans.FriendCircleBean;

import com.example.loadconfig.LoadConfig;

import java.util.List;
import java.util.Random;
import android.util.Log;

/**
 * Medium Load Activity, each frame calculation is medium, medium load during sliding
 * 同样添加了基于Choreographer的帧回调，在动画阶段周期性执行中等负载任务
 */
public class MediumLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "MediumLoadActivity";
    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = com.example.loadconfig.LoadType.MEDIUM;
    
    // 用于创建周期性"肥"帧的成员变量
    private Choreographer mChoreographer;
    private int mFrameCount = 0;
    private static final int HEAVY_FRAME_INTERVAL = 8;
    private Random mRandom = new Random(LoadConfig.COMPUTATION_SEED);
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private boolean mIsHeavyFrameEnabled = true;
    private boolean mIsScrolling = false;
    private int mHeavyFrameLoadFactor = 800;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium_load);
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(VideoMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(VideoMainActivity.EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.MEDIUM);
        }
        
        // Process image name, remove possible file extension
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        initHeavyFrameComponents();
        
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
                    mChoreographer.postFrameCallback(MediumLoadActivity.this);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    /**
     * 初始化创建"肥"帧所需的组件
     */
    private void initHeavyFrameComponents() {
        // 创建用于绘制的Bitmap和Canvas
        mBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint.setAntiAlias(true);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsHeavyFrameEnabled && mIsScrolling) {
            mFrameCount++;
            if (mFrameCount % HEAVY_FRAME_INTERVAL == 0) {
                Trace.beginSection("MediumLoadActivity_doFrame_heavyTask");
                executeHeavyTask();
                Trace.endSection();
            }
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 执行中等负载任务，使当前帧变"肥"
     * 但负载强度是HeavyLoadActivity的一半
     */
    private void executeHeavyTask() {
        // 执行大量计算和绘制操作，创造中等CPU和GPU负载
        for (int i = 0; i < mHeavyFrameLoadFactor; i++) {
            float x = mRandom.nextFloat() * 500;
            float y = mRandom.nextFloat() * 500;
            
            // 随机颜色
            mPaint.setColor(Color.argb(
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256),
                    mRandom.nextInt(256)
            ));
            
            // 绘制图形
            mCanvas.drawCircle(x, y, 10 + mRandom.nextFloat() * 10, mPaint);
            
            // 执行一些数学计算，防止编译器优化
            double sinValue = Math.sin(x) * Math.cos(y);
            double tanValue = Math.tan(x * 0.1);
            if (sinValue > 0.999 && tanValue > 100) {
                mPaint.setStrokeWidth((float) (sinValue + tanValue));
            }
            
            // 计算矩阵运算，增加CPU负载
            float[] matrix = new float[9];
            for (int j = 0; j < 9; j++) {
                matrix[j] = mRandom.nextFloat() * 10;
            }
            
            // 更多运算增加CPU负载
            double sqrtValue = Math.sqrt(x * x + y * y);
            double powValue = Math.pow(x, 1.5) * Math.pow(y, 1.2);
            
            // 防止编译器优化
            if (sqrtValue > 400 && powValue > 5000) {
                mPaint.setARGB((int) sqrtValue % 256, (int) powValue % 256, 
                               (int) (sqrtValue * powValue) % 256, 255);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 清空缓存，确保使用正确的负载类型
        VideoDataCenter.getInstance().clearCachedData();
        
        // 确保数据已根据正确的负载类型生成
        if (adapter != null) {
            // 刷新数据，确保显示正确的点赞和评论数量
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }

        String loadTypeStr = getLoadTypeString(mLoadType);
        Log.d(TAG, "onResume: 当前模式: " + loadTypeStr);
        
        // 如果已经停止了帧回调，重新启动
        if (!mIsHeavyFrameEnabled) {
            mIsHeavyFrameEnabled = true;
            mChoreographer.postFrameCallback(this);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停"肥"帧生成
        mIsHeavyFrameEnabled = false;
    }

    private void initRecyclerView() {
        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 创建适配器
        if (adapter == null) {
            adapter = new VideoFriendCircleAdapter(this, recyclerView, mLoadType);
            // 添加header view - 恢复原有的加载方式
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            // 设置数据 - 传入Context
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        } else {
            // 刷新数据 - 传入Context
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        }
    }

    private String getLoadTypeString(int loadType) {
        switch (loadType) {
            case com.example.loadconfig.LoadType.LIGHT:
                return "轻负载";
            case com.example.loadconfig.LoadType.MEDIUM:
                return "中负载";
            case com.example.loadconfig.LoadType.HEAVY:
                return "高负载";
            default:
                return "未知负载";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
        recyclerView.setAdapter(null);
        adapter = null;
        imageLoader = null;
        
        // 停止帧回调并释放资源
        mIsHeavyFrameEnabled = false;
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mCanvas = null;
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