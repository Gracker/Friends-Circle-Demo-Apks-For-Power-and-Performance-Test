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

import java.util.Random;

/**
 * Medium Load Between Frames Activity, medium load happens between doFrame calls
 */
public class MediumLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "MediumLoadBetweenFramesActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM;
    
    // 用于在帧之间执行负载的成员变量
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mRandom = new Random(12345); // 使用固定种子确保可重复性
    private Random mTaskDecisionRandom = new Random(67890); // 用于决定是否执行任务的随机数生成器
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false; // 是否正在滚动
    private float mTaskExecutionProbability = 0.5f; // 50%的概率执行帧间任务
    
    // 用于存储计算结果，防止编译器优化
    private volatile double mComputationResult = 0.0;
    private volatile int mImageProcessingResult = 0;
    private volatile long mDataProcessingResult = 0L;
    
    // 滚动监听器
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium_load_between_frames);
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM);
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
                        mChoreographer.postFrameCallback(MediumLoadBetweenFramesActivity.this);
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
        mBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
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
     * 执行帧间中等负载任务 - 复杂数学计算、图像处理和数据排序
     */
    private void executeBetweenFrameLoad() {
        Trace.beginSection("MediumLoadBetweenFrames_betweenFrameLoad");
        
        // 任务1: 复杂数学计算 - 计算更大范围的数学函数和级数
        double sum = 0.0;
        for (int i = 1; i <= 400; i++) {
            double x = i * 0.05;
            sum += Math.sin(x) * Math.cos(x * 2) + Math.pow(x, 1.5) + Math.log(i + 1) * Math.exp(-x * 0.1);
            // 添加更复杂的计算
            sum += Math.atan2(Math.sin(x), Math.cos(x)) + Math.sinh(x * 0.01) + Math.cosh(x * 0.01);
        }
        
        // 任务2: 矩阵运算 - 3x3矩阵乘法和求逆
        double[][] matrixA = new double[3][3];
        double[][] matrixB = new double[3][3];
        double[][] result = new double[3][3];
        
        // 初始化矩阵
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                matrixA[i][j] = mRandom.nextDouble() * 10;
                matrixB[i][j] = mRandom.nextDouble() * 10;
            }
        }
        
        // 矩阵乘法
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // 任务3: 图像处理 - 复杂图形绘制和滤镜效果
        mPaint.setColor(Color.rgb(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256)));
        mPaint.setAntiAlias(true);
        
        // 绘制更多复杂图形
        for (int i = 0; i < 100; i++) {
            float x = mRandom.nextFloat() * 400;
            float y = mRandom.nextFloat() * 400;
            float radius = 8 + mRandom.nextFloat() * 15;
            
            // 绘制渐变圆形
            mCanvas.drawCircle(x, y, radius, mPaint);
            
            // 绘制矩形和线条
            if (i % 3 == 0) {
                mCanvas.drawRect(x - radius, y - radius, x + radius, y + radius, mPaint);
            }
            if (i % 5 == 0) {
                mCanvas.drawLine(x, y, x + radius * 2, y + radius * 2, mPaint);
            }
        }
        
        // 任务4: 数据处理 - 排序和搜索算法
        int[] dataArray = new int[400];
        for (int i = 0; i < 400; i++) {
            dataArray[i] = mRandom.nextInt(1000);
        }
        
        // 快速排序
        quickSort(dataArray, 0, dataArray.length - 1);
        
        // 二分查找
        int searchTarget = dataArray[dataArray.length / 2];
        int foundIndex = binarySearch(dataArray, searchTarget);
        
        // 任务5: 字符串处理 - 复杂字符串操作
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("MediumTask_").append(i).append("_").append(mRandom.nextInt(100)).append("_");
        }
        String processedString = sb.toString().toUpperCase().replace("_", "-");
        String[] parts = processedString.split("-");
        
        // 存储结果防止编译器优化
        mComputationResult = sum + result[0][0] + result[1][1] + result[2][2];
        mImageProcessingResult = processedString.length() + parts.length;
        mDataProcessingResult = foundIndex + dataArray[0] + dataArray[dataArray.length - 1];
        
        // 验证计算结果，确保不被优化掉
        if (mComputationResult > 0 && mImageProcessingResult > 0 && mDataProcessingResult >= 0) {
            // 更新paint属性，使用计算结果
            mPaint.setAlpha((int)(Math.abs(mComputationResult) % 255) + 1);
            mPaint.setStrokeWidth((mImageProcessingResult % 10) + 1);
        }
        
        Trace.endSection();
        Log.d(TAG, "执行了帧间中等负载任务，计算结果: " + mComputationResult + 
                   ", 图像处理: " + mImageProcessingResult + ", 数据处理: " + mDataProcessingResult);
    }
    
    // 快速排序算法
    private void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
    
    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }
    
    // 二分查找算法
    private int binarySearch(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) return mid;
            if (arr[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
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
        switch (loadType) {
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MINIMAL:
                return "最轻负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_LIGHT:
                return "轻负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                return "中负载";
            case PerformanceFriendCircleAdapter.LOAD_TYPE_HEAVY:
                return "高负载";
            default:
                return "未知负载";
        }
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