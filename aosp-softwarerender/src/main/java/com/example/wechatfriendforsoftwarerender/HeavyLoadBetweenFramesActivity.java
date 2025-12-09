package com.example.wechatfriendforsoftwarerender;

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
import com.example.wechatfriendforsoftwarerender.adapters.SoftwareRenderFriendCircleAdapter;

import java.util.Random;

/**
 * Heavy Load Between Frames Activity, heavy load happens between doFrame calls
 */
public class HeavyLoadBetweenFramesActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    private static final String TAG = "HeavyLoadBetweenFramesActivity";
    private RecyclerView recyclerView;
    private SoftwareRenderFriendCircleAdapter adapter;
    private RequestBuilder<Drawable> imageLoader;
    private int mLoadType = SoftwareRenderFriendCircleAdapter.LOAD_TYPE_HEAVY;
    
    // 用于在帧之间执行负载的成员变量
    private Choreographer mChoreographer;
    private Handler mHandler;
    private Random mRandom = new Random(12345);
    private Random mTaskDecisionRandom = new Random(67890);
    private Paint mPaint = new Paint();
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private boolean mIsBetweenFrameLoadEnabled = true;
    private boolean mIsScrolling = false;
    private float mTaskExecutionProbability = 0.7f;
    
    private volatile double mComputationResult = 0.0;
    private volatile int mImageProcessingResult = 0;
    private volatile long mDataProcessingResult = 0L;
    private volatile double mComplexMathResult = 0.0;
    
    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heavy_load_between_frames);
        
        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 从Intent中获取负载类型
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SoftwareRenderMainActivity.EXTRA_LOAD_TYPE)) {
            mLoadType = intent.getIntExtra(SoftwareRenderMainActivity.EXTRA_LOAD_TYPE, SoftwareRenderFriendCircleAdapter.LOAD_TYPE_HEAVY);
        }
        
        imageLoader = Glide.with(this).asDrawable().apply(
                new RequestOptions().centerCrop()
        );

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();
        
        initBetweenFrameLoadComponents();
        
        mChoreographer = Choreographer.getInstance();
        mHandler = new Handler(Looper.getMainLooper());
        
        initScrollListener();
        Log.d(TAG, "onCreate: 等待列表滚动时启动负载任务");
    }
    
    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                } else if (!mIsScrolling) {
                    mIsScrolling = true;
                    mChoreographer.postFrameCallback(HeavyLoadBetweenFramesActivity.this);
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
        mBitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint.setAntiAlias(true);
    }
    
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsBetweenFrameLoadEnabled && mIsScrolling) {
            if (mTaskDecisionRandom.nextFloat() < mTaskExecutionProbability) {
                mHandler.post(() -> {
                    if (mIsScrolling) executeBetweenFrameLoad();
                });
            }
            mChoreographer.postFrameCallback(this);
        }
    }
    
    /**
     * 执行帧间高负载任务 - 复杂数学计算、高密度图像处理、数据排序和算法运算
     */
    private void executeBetweenFrameLoad() {
        Trace.beginSection("HeavyLoadBetweenFrames_betweenFrameLoad");
        
        // 任务1: 高复杂度数学计算 - 数值积分、级数计算
        double sum = 0.0;
        for (int i = 1; i <= 800; i++) {
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
                matrixA[i][j] = mRandom.nextDouble() * 20 - 10; // -10到10的随机数
                matrixB[i][j] = mRandom.nextDouble() * 20 - 10;
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
        
        // 任务3: 高密度图像处理 - 复杂图形和滤镜效果
        mPaint.setColor(Color.rgb(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256)));
        mPaint.setAntiAlias(true);
        
        // 绘制大量复杂图形
        for (int i = 0; i < 200; i++) {
            float x = mRandom.nextFloat() * 600;
            float y = mRandom.nextFloat() * 600;
            float radius = 10 + mRandom.nextFloat() * 20;
            
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
        
        // 任务4: 复杂数据处理 - 多种排序算法和数据结构操作
        int[] dataArray = new int[800];
        for (int i = 0; i < 800; i++) {
            dataArray[i] = mRandom.nextInt(2000);
        }
        
        // 归并排序（更复杂的算法）
        int[] sortedArray = mergeSort(dataArray.clone());
        
        // 堆排序
        heapSort(dataArray);
        
        // 多次二分查找
        int searchCount = 0;
        for (int i = 0; i < 80; i++) {
            int target = mRandom.nextInt(2000);
            if (binarySearch(sortedArray, target) >= 0) {
                searchCount++;
            }
        }
        
        // 任务5: 算法运算 - 动态规划和递归计算
        int fibResult = fibonacci(30); // 计算第30个斐波那契数
        long factorialResult = factorial(15); // 计算15的阶乘
        
        // 任务6: 高级字符串处理 - 正则表达式和编码转换
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            sb.append("HeavyTask_").append(i).append("_").append(mRandom.nextInt(1000))
              .append("_Complex_").append(System.nanoTime() % 1000).append("_");
        }
        String processedString = sb.toString().toUpperCase().replace("_", "-");
        String[] parts = processedString.split("-");
        
        // 字符串编码处理
        byte[] encodedBytes = processedString.getBytes();
        String reConstructed = new String(encodedBytes);
        
        // 存储结果防止编译器优化
        mComputationResult = sum + result[0][0] + result[2][2] + result[4][4];
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
        Log.d(TAG, "执行了帧间高负载任务，数学计算: " + mComputationResult + 
                   ", 图像处理: " + mImageProcessingResult + ", 数据处理: " + mDataProcessingResult + 
                   ", 复杂算法: " + mComplexMathResult);
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
        SoftwareRenderDataCenter.getInstance().clearCachedData();
        
        // 确保数据已根据正确的负载类型生成
        if (adapter != null) {
            adapter.setFriendCircleBeans(SoftwareRenderDataCenter.getInstance().getFriendCircleBeans(mLoadType));
        }

        String loadTypeStr = getLoadTypeString(mLoadType);
        Log.d(TAG, "onResume: 当前模式: " + loadTypeStr + " (帧间负载)");
        
        // 如果已经停止了帧回调，重新启动
        if (!mIsBetweenFrameLoadEnabled) {
            mIsBetweenFrameLoadEnabled = true;
            mChoreographer.postFrameCallback(this);
        }
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
            adapter = new SoftwareRenderFriendCircleAdapter(this, recyclerView, mLoadType);
            // 添加header view
            View headerView = getLayoutInflater().inflate(R.layout.include_title_bar_view, recyclerView, false);
            adapter.setHeaderView(headerView);
            recyclerView.setAdapter(adapter);
            // 设置数据
            adapter.setFriendCircleBeans(SoftwareRenderDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        } else {
            // 刷新数据
            adapter.setFriendCircleBeans(SoftwareRenderDataCenter.getInstance().generateDataForLoadType(this, mLoadType));
        }
    }

    private String getLoadTypeString(int loadType) {
        switch (loadType) {
            case SoftwareRenderFriendCircleAdapter.LOAD_TYPE_MINIMAL:
                return "最轻负载";
            case SoftwareRenderFriendCircleAdapter.LOAD_TYPE_LIGHT:
                return "轻负载";
            case SoftwareRenderFriendCircleAdapter.LOAD_TYPE_MEDIUM:
                return "中负载";
            case SoftwareRenderFriendCircleAdapter.LOAD_TYPE_HEAVY:
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
        mIsBetweenFrameLoadEnabled = false;
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