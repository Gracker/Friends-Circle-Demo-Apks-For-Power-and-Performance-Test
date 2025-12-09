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
 * Heavy Mixed Load Activity, combines heavy doFrame load + heavy between-frame load
 * 使用间隔随机数安排Task执行，最高负载和最高频率
 */
public class HeavyMixedLoadActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyMixedLoadActivity";
    private RecyclerView recyclerView;
    private PerformanceFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = com.example.loadconfig.LoadType.HEAVY;
    
    // 高负载混合Task配置 - 使用统一配置中心
    private static final int MIN_TASK_INTERVAL_MS = LoadConfig.MIN_TASK_INTERVAL_MS;
    private static final int MAX_TASK_INTERVAL_MS = LoadConfig.MAX_TASK_INTERVAL_MS;
    private static final int DOFRAME_TASK_INTENSITY = LoadConfig.MIXED_DOFRAME_HEAVY_INTENSITY;
    private static final int BETWEEN_FRAME_TASK_INTENSITY = LoadConfig.MIXED_BETWEEN_FRAME_HEAVY_INTENSITY;
    
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
    private boolean mIsScrolling = false; // 是否正在滚动
    private long mTaskExecutionCount = 0;
    private long mDoFrameTaskExecutionCount = 0;
    
    // 用于存储计算结果，防止编译器优化
    private volatile double mComputationResult = 0.0;
    private volatile int mImageProcessingResult = 0;
    private volatile long mDataProcessingResult = 0L;
    private volatile double mComplexMathResult = 0.0;
    
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
            mLoadType = intent.getIntExtra(PerformanceMainActivity.EXTRA_LOAD_TYPE, com.example.loadconfig.LoadType.HEAVY);
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
        
        // 初始化滚动监听器 - 只有在滚动时才执行负载任务
        initScrollListener();
        
        // 注意：不再在onCreate中启动Task调度器
        // 任务调度器只在列表滚动时启动
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
                    // 列表停止滚动，停止任务调度
                    mIsScrolling = false;
                    stopTaskScheduling();
                    Log.d(TAG, "列表停止滚动，停止负载任务");
                } else {
                    // 列表开始滚动，启动任务调度
                    if (!mIsScrolling) {
                        mIsScrolling = true;
                        startTaskScheduling();
                        Log.d(TAG, "列表开始滚动，启动负载任务");
                    }
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }
    
    /**
     * 启动任务调度
     */
    private void startTaskScheduling() {
        if (!mIsTaskSchedulingEnabled) {
            mIsTaskSchedulingEnabled = true;
        }
        mChoreographer.postFrameCallback(this);
        scheduleNextBetweenFrameTask();
        scheduleNextDoFrameTask();
    }
    
    /**
     * 停止任务调度
     */
    private void stopTaskScheduling() {
        mHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * 初始化创建Task所需的组件
     */
    private void initTaskComponents() {
        // 创建用于绘制的Bitmap和Canvas (高负载使用大尺寸)
        mBitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint.setAntiAlias(true);
    }
    
    /**
     * Choreographer的doFrame回调，仅用于基础渲染，不执行额外负载
     * 负载任务由独立的调度器管理
     * 只有在列表滚动时才继续帧回调
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        // 只有在列表滚动时才继续帧回调
        if (mIsTaskSchedulingEnabled && mIsScrolling) {
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 调度下一个帧间Task执行，使用随机间隔
     * 只有在列表滚动时才调度
     */
    private void scheduleNextBetweenFrameTask() {
        if (!mIsTaskSchedulingEnabled || !mIsScrolling) {
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
                if (!mIsScrolling) return; // 停止滚动则不执行
                mTaskExecutionCount++;
                executeBetweenFrameHeavyLoad();
                // 执行完当前Task后，继续调度下一个
                scheduleNextBetweenFrameTask();
            }
        }, intervalMs);
    }
    
    /**
     * 调度下一个doFrame Task执行，使用随机间隔
     * 只有在列表滚动时才调度
     */
    private void scheduleNextDoFrameTask() {
        if (!mIsTaskSchedulingEnabled || !mIsScrolling) {
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
                if (!mIsScrolling) return; // 停止滚动则不执行
                mDoFrameTaskExecutionCount++;
                executeDoFrameHeavyLoad();
                // 执行完当前Task后，继续调度下一个
                scheduleNextDoFrameTask();
            }
        }, intervalMs);
    }
    
    /**
     * 执行doFrame期间的高负载任务 (使用配置的强度)
     */
    private void executeDoFrameHeavyLoad() {
        Trace.beginSection("HeavyMixedLoad_doFrameLoad");
        
        // 高负载计算: 使用配置的强度进行计算
        double sum = 0;
        for (int i = 0; i < DOFRAME_TASK_INTENSITY; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.02) + 
                   Math.sqrt(i + 1) * Math.log(i + 1) + 
                   Math.pow(i * 0.01, 2.0) +
                   Math.exp(-i * 0.001) * Math.atan(i * 0.1);
        }
        
        // 存储结果防止编译器优化
        mComputationResult = sum;
        
        Trace.endSection();
        Log.d(TAG, "执行了第 " + mDoFrameTaskExecutionCount + " 个doFrame高负载任务，强度: " + DOFRAME_TASK_INTENSITY + ", 计算结果: " + sum);
    }
    
    /**
     * 执行帧间高负载任务 (来自帧间高负载逻辑)
     */
    private void executeBetweenFrameHeavyLoad() {
        Trace.beginSection("HeavyMixedLoad_betweenFrameLoad");
        
        // 任务1: 高复杂度数学计算 - 使用配置强度
        double sum = 0.0;
        for (int i = 1; i <= BETWEEN_FRAME_TASK_INTENSITY * 2; i++) { // 约1600次计算
            double x = i * 0.02;
            // 复杂的三角函数和指数函数计算
            sum += Math.sin(x * Math.PI) * Math.cos(x * Math.PI / 2) + 
                   Math.pow(x, 2.5) + Math.log(i + 1) * Math.exp(-x * 0.05);
            // 双曲函数和反三角函数
            sum += Math.atan2(Math.sin(x), Math.cos(x)) + 
                   Math.sinh(x * 0.01) + Math.cosh(x * 0.01) + 
                   Math.tanh(x * 0.02);
            // 贝塞尔函数近似和伽马函数近似
            sum += approximateBesselJ0(x) + logGammaApproximation(x + 1);
        }
        
        // 任务2: 大型矩阵运算 - 5x5矩阵乘法、求逆和特征值
        double[][] matrixA = new double[5][5];
        double[][] matrixB = new double[5][5];
        double[][] result = new double[5][5];
        double[][] identity = new double[5][5];
        
        // 初始化矩阵
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                matrixA[i][j] = mComputationRandom.nextDouble() * 20 - 10; // -10到10的随机数
                matrixB[i][j] = mComputationRandom.nextDouble() * 20 - 10;
                identity[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
        
        // 矩阵乘法
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        
        // LU分解近似计算
        double determinant = calculateDeterminant5x5(matrixA);
        
        // 任务3: 高密度图像处理 - 使用配置强度
        mPaint.setColor(Color.rgb(mComputationRandom.nextInt(256), mComputationRandom.nextInt(256), mComputationRandom.nextInt(256)));
        mPaint.setAntiAlias(true);
        
        // 绘制大量复杂图形，数量基于配置强度
        int graphicsCount = BETWEEN_FRAME_TASK_INTENSITY / 2; // 约400个图形
        for (int i = 0; i < graphicsCount; i++) {
            float x = mComputationRandom.nextFloat() * 600;
            float y = mComputationRandom.nextFloat() * 600;
            float radius = 10 + mComputationRandom.nextFloat() * 20;
            
            // 绘制多层次图形
            mCanvas.drawCircle(x, y, radius, mPaint);
            mCanvas.drawCircle(x, y, radius * 0.7f, mPaint);
            mCanvas.drawCircle(x, y, radius * 0.4f, mPaint);
            
            // 绘制复杂几何图形
            if (i % 2 == 0) {
                mCanvas.drawRect(x - radius, y - radius, x + radius, y + radius, mPaint);
                mCanvas.drawLine(x - radius, y - radius, x + radius, y + radius, mPaint);
                mCanvas.drawLine(x - radius, y + radius, x + radius, y - radius, mPaint);
            }
            
            // 绘制椭圆和弧形
            if (i % 3 == 0) {
                mCanvas.drawOval(x - radius, y - radius * 1.5f, x + radius, y + radius * 1.5f, mPaint);
                mCanvas.drawArc(x - radius, y - radius, x + radius, y + radius, 0, 270, false, mPaint);
            }
        }
        
        // 任务4: 复杂数据处理 - 使用配置强度
        int dataArraySize = BETWEEN_FRAME_TASK_INTENSITY * 2; // 约1600个元素
        int[] dataArray = new int[dataArraySize];
        for (int i = 0; i < dataArraySize; i++) {
            dataArray[i] = mComputationRandom.nextInt(2000);
        }
        
        // 归并排序（更复杂的算法）
        int[] sortedArray = mergeSort(dataArray.clone());
        
        // 堆排序
        heapSort(dataArray);
        
        // 多次二分查找，使用配置强度
        int searchCount = 0;
        int searchTimes = BETWEEN_FRAME_TASK_INTENSITY / 5; // 约160次搜索
        for (int i = 0; i < searchTimes; i++) {
            int target = mComputationRandom.nextInt(2000);
            if (binarySearch(sortedArray, target) >= 0) {
                searchCount++;
            }
        }
        
        // 任务5: 算法运算 - 动态规划和递归计算
        int fibResult = fibonacci(30); // 计算第30个斐波那契数
        long factorialResult = factorial(15); // 计算15的阶乘
        
        // 任务6: 高级字符串处理 - 使用配置强度
        StringBuilder sb = new StringBuilder();
        int stringCount = BETWEEN_FRAME_TASK_INTENSITY; // 约800个字符串
        for (int i = 0; i < stringCount; i++) {
            sb.append("HeavyTask_").append(i).append("_").append(mComputationRandom.nextInt(1000))
              .append("_Complex_").append(System.nanoTime() % 1000).append("_");
        }
        String processedString = sb.toString().toUpperCase().replace("_", "-");
        String[] parts = processedString.split("-");
        
        // 字符串编码处理
        byte[] encodedBytes = processedString.getBytes();
        String reConstructed = new String(encodedBytes);
        
        // 存储结果防止编译器优化
        mComputationResult += sum + result[0][0] + result[2][2] + result[4][4];
        mImageProcessingResult = processedString.length() + parts.length + reConstructed.length();
        mDataProcessingResult = searchCount + sortedArray[0] + sortedArray[sortedArray.length - 1];
        mComplexMathResult = determinant + fibResult + factorialResult;
        
        // 验证计算结果，确保不被优化掉
        if (Math.abs(mComputationResult) > 0 && mImageProcessingResult > 0 && 
            mDataProcessingResult >= 0 && Math.abs(mComplexMathResult) > 0) {
            // 更新paint属性，使用所有计算结果
            mPaint.setAlpha((int)(Math.abs(mComputationResult + mComplexMathResult) % 255) + 1);
            mPaint.setStrokeWidth((mImageProcessingResult % 15) + 1);
        }
        
        Trace.endSection();
        Log.d(TAG, "执行了第 " + mTaskExecutionCount + " 个帧间高负载任务，强度: " + BETWEEN_FRAME_TASK_INTENSITY + 
                   ", 数学计算: " + mComputationResult + ", 图像处理: " + mImageProcessingResult + 
                   ", 数据处理: " + mDataProcessingResult + ", 复杂算法: " + mComplexMathResult);
    }
    
    // 贝塞尔函数J0近似
    private double approximateBesselJ0(double x) {
        if (Math.abs(x) < 8.0) {
            double y = x * x;
            return (79.78 - y * (0.000077 * y - 0.55274)) /
                   (79.78 + y * (2.18 + y * (0.92 + y * 0.267)));
        } else {
            double z = 8.0 / x;
            double y = z * z;
            return Math.sqrt(0.636 / Math.abs(x)) * 
                   Math.cos(Math.abs(x) - 0.785 + y * (-0.055 + y * 0.0043));
        }
    }
    
    // 伽马函数对数近似
    private double logGammaApproximation(double x) {
        return (x - 0.5) * Math.log(x) - x + 0.5 * Math.log(2 * Math.PI) +
               1.0 / (12.0 * x) - 1.0 / (360.0 * x * x * x);
    }
    
    // 计算5x5矩阵行列式
    private double calculateDeterminant5x5(double[][] matrix) {
        // 简化版本，使用第一行展开
        double det = 0;
        for (int i = 0; i < 5; i++) {
            double minor = calculateMinor4x4(matrix, 0, i);
            det += (i % 2 == 0 ? 1 : -1) * matrix[0][i] * minor;
        }
        return det;
    }
    
    private double calculateMinor4x4(double[][] matrix, int row, int col) {
        double[][] minor = new double[4][4];
        int r = 0;
        for (int i = 0; i < 5; i++) {
            if (i == row) continue;
            int c = 0;
            for (int j = 0; j < 5; j++) {
                if (j == col) continue;
                minor[r][c] = matrix[i][j];
                c++;
            }
            r++;
        }
        // 简化的4x4行列式计算
        return minor[0][0] * (minor[1][1] * (minor[2][2] * minor[3][3] - minor[2][3] * minor[3][2]) -
                              minor[1][2] * (minor[2][1] * minor[3][3] - minor[2][3] * minor[3][1]) +
                              minor[1][3] * (minor[2][1] * minor[3][2] - minor[2][2] * minor[3][1]));
    }
    
    // 归并排序
    private int[] mergeSort(int[] arr) {
        if (arr.length <= 1) return arr;
        
        int mid = arr.length / 2;
        int[] left = new int[mid];
        int[] right = new int[arr.length - mid];
        
        System.arraycopy(arr, 0, left, 0, mid);
        System.arraycopy(arr, mid, right, 0, arr.length - mid);
        
        left = mergeSort(left);
        right = mergeSort(right);
        
        return merge(left, right);
    }
    
    private int[] merge(int[] left, int[] right) {
        int[] result = new int[left.length + right.length];
        int i = 0, j = 0, k = 0;
        
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                result[k++] = left[i++];
            } else {
                result[k++] = right[j++];
            }
        }
        
        while (i < left.length) result[k++] = left[i++];
        while (j < right.length) result[k++] = right[j++];
        
        return result;
    }
    
    // 堆排序
    private void heapSort(int[] arr) {
        int n = arr.length;
        
        // 构建堆
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }
        
        // 提取元素
        for (int i = n - 1; i > 0; i--) {
            int temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;
            heapify(arr, i, 0);
        }
    }
    
    private void heapify(int[] arr, int n, int i) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        
        if (left < n && arr[left] > arr[largest]) largest = left;
        if (right < n && arr[right] > arr[largest]) largest = right;
        
        if (largest != i) {
            int temp = arr[i];
            arr[i] = arr[largest];
            arr[largest] = temp;
            heapify(arr, n, largest);
        }
    }
    
    // 二分查找
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
    
    // 斐波那契数计算
    private int fibonacci(int n) {
        if (n <= 1) return n;
        int[] fib = new int[n + 1];
        fib[0] = 0;
        fib[1] = 1;
        for (int i = 2; i <= n; i++) {
            fib[i] = fib[i - 1] + fib[i - 2];
        }
        return fib[n];
    }
    
    // 阶乘计算
    private long factorial(int n) {
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
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

        Log.d(TAG, "onResume: " + LoadConfig.getDescription(com.example.loadconfig.LoadType.HEAVY_MIXED));
        Log.d(TAG, "Task间隔: " + MIN_TASK_INTERVAL_MS + "-" + MAX_TASK_INTERVAL_MS + "ms");
        
        // 恢复任务调度启用状态，但不立即启动任务
        // 任务只在列表滚动时启动
        mIsTaskSchedulingEnabled = true;
        mIsScrolling = false;
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
        
        // 停止Task调度和帧回调，释放资源
        mIsTaskSchedulingEnabled = false;
        mIsScrolling = false;
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