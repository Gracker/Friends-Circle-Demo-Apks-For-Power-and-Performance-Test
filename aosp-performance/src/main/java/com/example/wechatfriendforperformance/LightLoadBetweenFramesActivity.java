package com.example.wechatfriendforperformance;

import android.content.Context;
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
import android.view.Choreographer;
import android.view.KeyEvent;
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

import java.util.Random;

/**
 * Light Load Between Frames Activity, light load happens between doFrame calls
 */
public class LightLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LightLoadBetweenFramesActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = com.example.loadconfig.LoadType.LIGHT;
    
    // 用于在帧之间执行负载的成员变量
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mRandom = new Random(LoadConfig.TASK_INTERVAL_SEED); // 使用统一配置的随机种子
    private Random mTaskDecisionRandom = new Random(LoadConfig.COMPUTATION_SEED); // 用于决定是否执行任务的随机数生成器
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false; // 是否正在滚动
    private float mTaskExecutionProbability = LoadConfig.LIGHT_TASK_PROBABILITY; // 使用配置的任务执行概率
    
    // 用于存储计算结果，防止编译器优化
    private volatile double mComputationResult = 0.0;
    private volatile int mImageProcessingResult = 0;
    
    // 滚动监听器
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_load_between_frames);
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.LIGHT);
        }
        
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        // 初始化用于创建帧间负载的组件
        initBetweenFrameLoadComponents();
        
        // 注册Choreographer帧回调
        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());
        
        // 初始化滚动监听器 - 只有在滚动时才执行负载任务
        initScrollListener();
        
        // 注意：不再在onCreate中启动帧回调
        // 帧回调只在列表滚动时启动
        Log.d(TAG, "onCreate: 等待列表滚动时启动负载任务");
    }
    
    /**
     * 初始化滚动监听器
     * 只有在列表滚动时才执行帧间负载
     */
    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 列表停止滚动，停止负载
                    mIsScrolling = false;
                    Log.d(TAG, "列表停止滚动，停止负载任务");
                } else {
                    // 列表开始滚动，启动帧回调
                    if (!mIsScrolling) {
                        mIsScrolling = true;
                        mChoreographer.postFrameCallback(LightLoadBetweenFramesActivity.this);
                        Log.d(TAG, "列表开始滚动，启动负载任务");
                    }
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    /**
     * 初始化创建帧间负载所需的组件
     */
    private void initBetweenFrameLoadComponents() {
        // 创建用于绘制的Bitmap和Canvas
        mBitmap = Bitmap.createBitmap(LoadConfig.LIGHT_BITMAP_SIZE, 
                                      LoadConfig.LIGHT_BITMAP_SIZE, 
                                      Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint.setAntiAlias(true);
    }
    
    /**
     * Choreographer的doFrame回调，根据随机概率决定是否执行帧间负载
     * 只有在列表滚动时才执行
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        // 只有在列表滚动时才执行负载和继续帧回调
        if (mIsBetweenFrameLoadEnabled && mIsScrolling) {
            // 使用固定种子的随机数决定是否执行帧间负载任务
            if (mTaskDecisionRandom.nextFloat() < mTaskExecutionProbability) {
                // 在帧回调完成后执行负载任务（帧与帧之间）
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsScrolling) { // 再次检查滚动状态
                            executeBetweenFrameLoad();
                        }
                    }
                });
            }
            
            // 注册下一帧回调（只在滚动时继续）
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 执行帧间轻负载任务 - 基础数学计算和简单图像处理
     */
    private void executeBetweenFrameLoad() {
        Trace.beginSection("LightLoadBetweenFrames_betweenFrameLoad");
        
        // 任务1: 基础数学计算 - 计算小范围数列的数学函数
        double sum = 0.0;
        for (int i = 1; i <= LoadConfig.BETWEEN_FRAME_LIGHT_INTENSITY; i++) {
            double x = i * 0.1;
            sum += Math.sin(x) * Math.cos(x) + Math.sqrt(i) + Math.log(i);
        }
        
        // 任务2: 简单矩阵运算 - 2x2矩阵乘法
        double[][] matrixA = {{mRandom.nextDouble(), mRandom.nextDouble()}, 
                             {mRandom.nextDouble(), mRandom.nextDouble()}};
        double[][] matrixB = {{mRandom.nextDouble(), mRandom.nextDouble()}, 
                             {mRandom.nextDouble(), mRandom.nextDouble()}};
        double[][] result = new double[2][2];
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // 任务3: 简单图像处理 - 在小画布上绘制基本图形
        mPaint.setColor(Color.rgb(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256)));
        for (int i = 0; i < 40; i++) {
            float x = mRandom.nextFloat() * LoadConfig.LIGHT_BITMAP_SIZE;
            float y = mRandom.nextFloat() * LoadConfig.LIGHT_BITMAP_SIZE;
            float radius = 5 + mRandom.nextFloat() * 10;
            mCanvas.drawCircle(x, y, radius, mPaint);
        }
        
        // 任务4: 字符串处理 - 创建和处理字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            sb.append("Task").append(i).append("_");
        }
        String processedString = sb.toString().toUpperCase().replace("_", "-");
        
        // 存储结果防止编译器优化
        mComputationResult = sum + result[0][0] + result[1][1];
        mImageProcessingResult = processedString.length();
        
        // 验证计算结果，确保不被优化掉
        if (mComputationResult > 0 && mImageProcessingResult > 0) {
            // 更新paint属性，使用计算结果
            mPaint.setAlpha((int)(Math.abs(mComputationResult) % 255) + 1);
        }
        
        Trace.endSection();
        Log.d(TAG, "执行了帧间轻负载任务，计算结果: " + mComputationResult + ", 字符串长度: " + mImageProcessingResult);
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

        String loadTypeStr = getLoadTypeString(mLoadType);
        Log.d(TAG, "onResume: 当前模式: " + loadTypeStr + " (帧间负载)");
        
        // 恢复负载启用状态，但不立即启动帧回调
        // 帧回调只在列表滚动时启动
        mIsBetweenFrameLoadEnabled = true;
        mIsScrolling = false;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停帧间负载
        mIsBetweenFrameLoadEnabled = false;
    }

    private void initRecyclerView() {
        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 创建适配器
        if (adapter == null) {
            adapter = new PerformanceFriendCircleAdapter(this, recyclerView, mLoadType);
            // 添加header view
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            // 设置数据
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        } else {
            // 刷新数据
            adapter.setFriendCircleBeans(PerformanceDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        }
    }

    private String getLoadTypeString(int loadType) {
        // 使用统一的 LoadType.toLabel() 获取负载类型标签
        return com.example.loadconfig.LoadType.toLabel(loadType);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除滚动监听器
        if (recyclerView != null && mScrollListener != null) {
            recyclerView.removeOnScrollListener(mScrollListener);
        }
        
        // 清理资源
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }
        recyclerView.setAdapter(null);
        adapter = null;
        imageLoader = null;
        
        // 停止帧回调并释放资源
        mIsBetweenFrameLoadEnabled = false;
        mIsScrolling = false;
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