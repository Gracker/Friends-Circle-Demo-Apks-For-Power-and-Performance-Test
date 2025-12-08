package com.example.wechatfriendforvideo;

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

import android.view.View;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.example.wechatfriendforvideo.adapters.VideoFriendCircleAdapter;
import com.example.wechatfriendforvideo.config.LoadConfig;

import java.util.Random;

/**
 * Light Mixed Load Activity, combines light doFrame load + light between-frame load
 * 使用间隔随机数安排Task执行，而不是每帧检查
 */
public class LightMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "LightMixedLoadActivity";
    private RecyclerView recyclerView;
    private VideoFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = VideoFriendCircleAdapter.LOAD_TYPE_LIGHT;
    
    // 轻负载混合Task配置 - 使用统一配置中心
    private static final int MIN_TASK_INTERVAL_MS = LoadConfig.MIN_TASK_INTERVAL_MS;
    private static final int MAX_TASK_INTERVAL_MS = LoadConfig.MAX_TASK_INTERVAL_MS;
    private static final int DOFRAME_TASK_INTENSITY = LoadConfig.LightMixedLoad.DOFRAME_TASK_INTENSITY;
    private static final int BETWEEN_FRAME_TASK_INTENSITY = LoadConfig.LightMixedLoad.BETWEEN_FRAME_TASK_INTENSITY;
    
    // 随机数生成器和调度器
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mTaskIntervalRandom = new Random(LoadConfig.TASK_INTERVAL_SEED);
    private Random mDoFrameIntervalRandom = new Random(LoadConfig.DOFRAME_INTERVAL_SEED);
    private Random mComputationRandom = new Random(LoadConfig.COMPUTATION_SEED);
    
    // 绘制相关
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    private Bitmap mBitmap;
    
    // 控制变量
    private boolean mIsTaskSchedulingEnabled = true;
    private long mTaskExecutionCount = 0;
    private long mDoFrameTaskExecutionCount = 0;
    
    // 用于存储计算结果，防止编译器优化
    private volatile double mComputationResult = 0.0;
    private volatile int mImageProcessingResult = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_mixed_load);
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(VideoMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(VideoMainActivity.EXTRA_LOAD_TYPE, VideoFriendCircleAdapter.LOAD_TYPE_LIGHT);
        }
        
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        // 初始化用于创建Task的组件
        initTaskComponents();
        
        // 注册Choreographer帧回调(仅用于基础渲染，不执行额外负载)
        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());
        mChoreographer.postFrameCallback(this);
        
        // 启动两个独立的Task调度器
        scheduleNextBetweenFrameTask(); // 帧间任务调度器
        scheduleNextDoFrameTask();      // doFrame任务调度器
    }
    
    /**
     * 初始化创建Task所需的组件
     */
    private void initTaskComponents() {
        // 创建用于绘制的Bitmap和Canvas (轻负载使用小尺寸)
        mBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint.setAntiAlias(true);
    }
    
    /**
     * Choreographer的doFrame回调，仅用于基础渲染，不执行额外负载
     * 负载任务由独立的调度器管理
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        // 这里仅注册下一帧回调，保持基础渲染循环
        // 不在doFrame中执行额外负载，负载由scheduleNextTask管理
        if (mIsTaskSchedulingEnabled) {
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 调度下一个帧间Task执行，使用随机间隔
     */
    private void scheduleNextBetweenFrameTask() {
        if (!mIsTaskSchedulingEnabled) {
            return;
        }
        
        // 生成随机间隔时间 (16ms - 83ms)
        int intervalMs = MIN_TASK_INTERVAL_MS + 
                        mTaskIntervalRandom.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        Log.d(TAG, "调度下一个帧间Task，间隔: " + intervalMs + "ms");
        
        // 使用Handler.postDelayed安排下一个帧间Task
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mTaskExecutionCount++;
                executeBetweenFrameLightLoad();
                // 执行完当前Task后，继续调度下一个
                scheduleNextBetweenFrameTask();
            }
        }, intervalMs);
    }
    
    /**
     * 调度下一个doFrame Task执行，使用随机间隔
     */
    private void scheduleNextDoFrameTask() {
        if (!mIsTaskSchedulingEnabled) {
            return;
        }
        
        // 生成随机间隔时间 (16ms - 83ms)
        int intervalMs = MIN_TASK_INTERVAL_MS + 
                        mDoFrameIntervalRandom.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        Log.d(TAG, "调度下一个doFrame Task，间隔: " + intervalMs + "ms");
        
        // 使用Handler.postDelayed安排下一个doFrame Task
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDoFrameTaskExecutionCount++;
                executeDoFrameLightLoad();
                // 执行完当前Task后，继续调度下一个
                scheduleNextDoFrameTask();
            }
        }, intervalMs);
    }
    
    /**
     * 执行doFrame期间的轻负载任务 (来自原始轻负载逻辑)
     */
    private void executeDoFrameLightLoad() {
        Trace.beginSection("LightMixedLoad_doFrameLoad");
        
        // 轻负载计算: 使用配置的强度进行计算
        double sum = 0;
        for (int i = 0; i < DOFRAME_TASK_INTENSITY; i++) {
            sum += Math.sin(i * 0.1) + Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        
        // 存储结果防止编译器优化
        mComputationResult = sum;
        
        Trace.endSection();
        Log.d(TAG, "执行了第 " + mDoFrameTaskExecutionCount + " 个doFrame轻负载任务，强度: " + DOFRAME_TASK_INTENSITY + ", 计算结果: " + sum);
    }
    
    /**
     * 执行帧间轻负载任务 (使用配置的强度)
     */
    private void executeBetweenFrameLightLoad() {
        Trace.beginSection("LightMixedLoad_betweenFrameLoad");
        
        // 任务1: 基础数学计算 - 使用配置强度
        double sum = 0.0;
        for (int i = 1; i <= BETWEEN_FRAME_TASK_INTENSITY; i++) {
            double x = i * 0.1;
            sum += Math.sin(x) * Math.cos(x) + Math.sqrt(i) + Math.log(i);
        }
        
        // 任务2: 简单矩阵运算 - 2x2矩阵乘法
        double[][] matrixA = {{mComputationRandom.nextDouble(), mComputationRandom.nextDouble()}, 
                             {mComputationRandom.nextDouble(), mComputationRandom.nextDouble()}};
        double[][] matrixB = {{mComputationRandom.nextDouble(), mComputationRandom.nextDouble()}, 
                             {mComputationRandom.nextDouble(), mComputationRandom.nextDouble()}};
        double[][] result = new double[2][2];
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // 任务3: 简单图像处理 - 在小画布上绘制基本图形 (降低强度)
        mPaint.setColor(Color.rgb(mComputationRandom.nextInt(256), mComputationRandom.nextInt(256), mComputationRandom.nextInt(256)));
        int graphicsCount = BETWEEN_FRAME_TASK_INTENSITY / 3; // 约40个图形
        for (int i = 0; i < graphicsCount; i++) {
            float x = mComputationRandom.nextFloat() * 200;
            float y = mComputationRandom.nextFloat() * 200;
            float radius = 5 + mComputationRandom.nextFloat() * 10;
            mCanvas.drawCircle(x, y, radius, mPaint);
        }
        
        // 任务4: 字符串处理 - 创建和处理字符串
        StringBuilder sb = new StringBuilder();
        int stringCount = BETWEEN_FRAME_TASK_INTENSITY / 2; // 约60个字符串
        for (int i = 0; i < stringCount; i++) {
            sb.append("LightTask").append(i).append("_");
        }
        String processedString = sb.toString().toUpperCase().replace("_", "-");
        
        // 存储结果防止编译器优化
        mComputationResult += sum + result[0][0] + result[1][1];
        mImageProcessingResult = processedString.length();
        
        // 验证计算结果，确保不被优化掉
        if (mComputationResult > 0 && mImageProcessingResult > 0) {
            // 更新paint属性，使用计算结果
            mPaint.setAlpha((int)(Math.abs(mComputationResult) % 255) + 1);
        }
        
        Trace.endSection();
        Log.d(TAG, "执行了第 " + mTaskExecutionCount + " 个帧间轻负载任务，强度: " + BETWEEN_FRAME_TASK_INTENSITY + 
                   ", 计算结果: " + mComputationResult + ", 字符串长度: " + mImageProcessingResult);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 清空缓存，确保使用正确的负载类型
        VideoDataCenter.getInstance().clearCachedData();
        
        // 确保数据已根据正确的负载类型生成
        if (adapter != null) {
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }

        Log.d(TAG, "onResume: " + LoadConfig.getLoadConfigDescription("LightMixedLoad"));
        Log.d(TAG, "Task间隔: " + MIN_TASK_INTERVAL_MS + "-" + MAX_TASK_INTERVAL_MS + "ms");
        
        // 恢复Task调度和帧回调
        if (!mIsTaskSchedulingEnabled) {
            mIsTaskSchedulingEnabled = true;
            mChoreographer.postFrameCallback(this);
            scheduleNextBetweenFrameTask();
            scheduleNextDoFrameTask();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停Task调度
        mIsTaskSchedulingEnabled = false;
        // 清除所有待执行的回调
        mHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "onPause: 已暂停Task调度，共执行了 " + mTaskExecutionCount + " 个帧间Task, " + 
                   mDoFrameTaskExecutionCount + " 个doFrame Task");
    }

    private void initRecyclerView() {
        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 创建适配器
        if (adapter == null) {
            adapter = new VideoFriendCircleAdapter(this, recyclerView, mLoadType);
            // 添加header view
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            // 设置数据
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        } else {
            // 刷新数据
            adapter.setFriendCircleBeans(VideoDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
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
        
        // 停止Task调度和帧回调，释放资源
        mIsTaskSchedulingEnabled = false;
        mHandler.removeCallbacksAndMessages(null);
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mCanvas = null;
        
        Log.d(TAG, "onDestroy: 资源已清理，总共执行了 " + mTaskExecutionCount + " 个帧间Task, " + 
                   mDoFrameTaskExecutionCount + " 个doFrame Task");
    }


}