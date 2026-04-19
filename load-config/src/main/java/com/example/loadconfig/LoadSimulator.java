package com.example.loadconfig;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Trace;

import java.util.Random;

/**
 * 统一的负载模拟器
 * 
 * 集中管理所有负载模拟逻辑，避免在各个模块中重复代码。
 * 所有模块共享相同的负载实现，确保测试结果的一致性。
 * 
 * 设计原则：
 * 1. 伪随机性：所有随机数使用固定种子，确保每次测试结果可重现
 * 2. 防编译器优化：使用 volatile 变量、数据依赖链、黑洞消费等技术
 * 3. 基于120fps：所有时间相关参数基于8.33ms/帧设计
 * 
 * 支持的负载类型：
 * - MINIMAL: 不执行任何负载
 * - LIGHT/MEDIUM/HEAVY: 帧内负载（在 onBindViewHolder 等位置执行）
 * - LIGHT_BETWEEN_FRAMES/MEDIUM_BETWEEN_FRAMES/HEAVY_BETWEEN_FRAMES: 纯帧间负载
 * - LIGHT_MIXED/MEDIUM_MIXED/HEAVY_MIXED: 混合负载（doFrame + betweenFrame）
 * - LONG_FRAME: 超长帧负载
 * 
 * 使用方法：
 * 1. 创建 LoadSimulator 实例
 * 2. 根据需要调用相应的执行方法
 * 3. 在 Activity 销毁时调用 release() 释放资源
 */
public class LoadSimulator {

    private static final String TAG = "LoadSimulator";

    // 绘制相关
    private Paint mPaint;
    private Canvas mCanvas;
    private Bitmap mBitmap;

    // 伪随机数生成器（使用固定种子确保可重现）
    private Random mComputationRandom;
    private Random mStringRandom;  // 用于字符串生成的独立随机数生成器
    private Random mTaskProbabilityRandom; // 用于触发概率判定的随机数生成器

    // 用于存储计算结果，防止编译器优化
    // volatile 关键字确保每次读写都直接访问内存，防止寄存器缓存优化
    private volatile double mComputationResult = 0.0;
    private volatile int mImageProcessingResult = 0;
    private volatile long mDataProcessingResult = 0L;
    private volatile double mComplexMathResult = 0.0;

    // 黑洞变量：用于消费计算结果，防止死代码消除
    // 这些变量的存在确保编译器不会优化掉"无用"的计算
    private static volatile long sBlackHole = 0L;

    // Bitmap 大小
    private static final int BITMAP_SIZE = 600;

    // 执行计数器（用于生成确定性的伪随机序列）
    private long mExecutionCounter = 0;

    // doFrame 伪随机帧间隔触发相关
    private Random mDoFrameIntervalRandom;  // 用于生成帧间隔的随机数生成器
    private long mDoFrameCounter = 0;       // 当前帧计数
    private long mNextDoFrameTarget = 0;    // 下一次执行 doFrame 负载的目标帧数
    private int mCurrentDoFrameLoadLevel = 0;  // 当前 doFrame 负载级别（用于检测变化）

    // 帧间负载伪随机帧间隔触发相关
    private Random mBetweenFrameIntervalRandom;  // 用于生成帧间隔的随机数生成器
    private long mBetweenFrameCounter = 0;       // 当前帧计数
    private long mNextBetweenFrameTarget = 0;    // 下一次执行帧间负载的目标帧数
    private int mCurrentBetweenFrameLoadLevel = 0;  // 当前帧间负载级别（用于检测变化）

    /**
     * 创建负载模拟器
     * 使用固定种子初始化所有随机数生成器，确保测试可重现
     */
    public LoadSimulator() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        // 使用固定种子初始化随机数生成器
        mComputationRandom = new Random(LoadConfig.COMPUTATION_SEED);
        mStringRandom = new Random(LoadConfig.STRING_GENERATION_SEED);
        mTaskProbabilityRandom = new Random(LoadConfig.TASK_INTERVAL_SEED);
        mDoFrameIntervalRandom = new Random(LoadConfig.DOFRAME_INTERVAL_SEED);
        mBetweenFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);

        // 创建用于绘制的 Bitmap 和 Canvas
        mBitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    /**
     * 重置随机数生成器到初始状态
     * 在需要确保测试从相同状态开始时调用
     */
    public void resetRandomState() {
        mComputationRandom = new Random(LoadConfig.COMPUTATION_SEED);
        mStringRandom = new Random(LoadConfig.STRING_GENERATION_SEED);
        mTaskProbabilityRandom = new Random(LoadConfig.TASK_INTERVAL_SEED);
        mDoFrameIntervalRandom = new Random(LoadConfig.DOFRAME_INTERVAL_SEED);
        mBetweenFrameIntervalRandom = new Random(LoadConfig.BETWEEN_FRAME_INTERVAL_SEED);
        mExecutionCounter = 0;
        mDoFrameCounter = 0;
        mNextDoFrameTarget = 0;
        mCurrentDoFrameLoadLevel = 0;
        mBetweenFrameCounter = 0;
        mNextBetweenFrameTarget = 0;
        mCurrentBetweenFrameLoadLevel = 0;
    }

    /**
     * 释放资源
     * 在 Activity 销毁时调用
     */
    public void release() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mCanvas = null;
    }

    // ==================== 防编译器优化的工具方法 ====================

    /**
     * 消费计算结果到黑洞变量
     * 防止编译器因"结果未使用"而优化掉计算过程
     */
    private void consumeToBlackHole(double value) {
        sBlackHole ^= Double.doubleToLongBits(value);
    }

    private void consumeToBlackHole(long value) {
        sBlackHole ^= value;
    }

    private void consumeToBlackHole(int value) {
        sBlackHole ^= value;
    }

    /**
     * 创建数据依赖链
     * 确保循环的每次迭代都依赖于前一次的结果，防止循环被并行化或展开优化
     */
    private double createDependencyChain(double previousValue, int iteration) {
        // 使用复杂的数学运算创建不可预测的依赖
        return Math.sin(previousValue + iteration) * Math.cos(previousValue * 0.1) + 
               Math.sqrt(Math.abs(previousValue) + 1);
    }

    // ==================== 统一入口方法 ====================

    /**
     * 执行负载（根据负载类型自动选择合适的执行方式）
     * 使用伪随机帧间隔触发，确保测试可重现
     * 
     * @param loadType 负载类型
     * @param traceTag Trace 标签名称
     */
    public void executeInFrameLoad(@LoadType.Type int loadType, String traceTag) {
        int intensity = LoadConfig.getInFrameIntensity(loadType);
        if (intensity == 0) return;

        // 获取负载级别
        int level = LoadType.getLoadLevel(loadType);

        // 检测负载级别是否变化，如果变化则重新计算目标帧数
        if (level != mCurrentDoFrameLoadLevel) {
            mCurrentDoFrameLoadLevel = level;
            mNextDoFrameTarget = mDoFrameCounter + calculateNextDoFrameInterval(level);
        }

        // 增加帧计数
        mDoFrameCounter++;

        // 检查是否到达执行时机
        if (mDoFrameCounter < mNextDoFrameTarget) {
            return; // 还没到执行时机，跳过本帧
        }

        // 计算下一次执行的目标帧数
        mNextDoFrameTarget = mDoFrameCounter + calculateNextDoFrameInterval(level);

        // 命中时间窗口后，再按伪随机概率决定是否执行
        if (!shouldExecuteByProbability(loadType)) {
            return;
        }

        // 执行负载
        Trace.beginSection(traceTag);
        mExecutionCounter++;

        // 根据负载级别选择执行方式
        switch (level) {
            case 1: // Light
                executeLightLoad(intensity);
                break;
            case 2: // Medium
                executeMediumLoad(intensity);
                break;
            case 3: // Heavy
                executeHeavyLoad(intensity);
                break;
            default:
                executeSimpleLoad(intensity);
                break;
        }

        Trace.endSection();
    }

    /**
     * 计算下一次 doFrame 负载执行的帧间隔
     * 使用伪随机数生成器，在最小和最大间隔之间生成随机值
     * 
     * @param loadLevel 负载级别 (1=Light, 2=Medium, 3=Heavy)
     * @return 帧间隔数
     */
    private int calculateNextDoFrameInterval(int loadLevel) {
        int minInterval = LoadConfig.getBetweenFrameMinIntervalByLevel(loadLevel);
        int maxInterval = LoadConfig.getBetweenFrameMaxIntervalByLevel(loadLevel);

        // 生成 [minInterval, maxInterval] 范围内的伪随机帧间隔
        return minInterval + mDoFrameIntervalRandom.nextInt(maxInterval - minInterval + 1);
    }

    /**
     * 伪随机判定当前时机是否真正执行负载
     * 使用固定种子随机数，确保每次测试触发序列可重现
     */
    private boolean shouldExecuteByProbability(@LoadType.Type int loadType) {
        float probability = LoadConfig.getTaskProbability(loadType);
        if (probability <= 0f) {
            return false;
        }
        if (probability >= 1f) {
            return true;
        }
        return mTaskProbabilityRandom.nextFloat() < probability;
    }

    /**
     * 执行 doFrame 负载（混合负载的 doFrame 部分）
     * 使用伪随机帧间隔触发，确保测试可重现
     * 
     * @param loadType 负载类型
     * @param traceTag Trace 标签名称
     */
    public void executeDoFrameLoad(@LoadType.Type int loadType, String traceTag) {
        int intensity = LoadConfig.getDoFrameIntensity(loadType);
        if (intensity == 0) return;

        // 获取负载级别
        int level = LoadType.getLoadLevel(loadType);

        // 检测负载级别是否变化，如果变化则重新计算目标帧数
        if (level != mCurrentDoFrameLoadLevel) {
            mCurrentDoFrameLoadLevel = level;
            mNextDoFrameTarget = mDoFrameCounter + calculateNextDoFrameInterval(level);
        }

        // 增加帧计数
        mDoFrameCounter++;

        // 检查是否到达执行时机
        if (mDoFrameCounter < mNextDoFrameTarget) {
            return; // 还没到执行时机，跳过本帧
        }

        // 计算下一次执行的目标帧数
        mNextDoFrameTarget = mDoFrameCounter + calculateNextDoFrameInterval(level);

        // 命中时间窗口后，再按伪随机概率决定是否执行
        if (!shouldExecuteByProbability(loadType)) {
            return;
        }

        // 执行负载
        Trace.beginSection(traceTag);
        mExecutionCounter++;

        // 根据负载级别选择执行方式
        switch (level) {
            case 1: // Light
                executeLightLoad(intensity);
                break;
            case 2: // Medium
                executeMediumLoad(intensity);
                break;
            case 3: // Heavy
                executeHeavyLoad(intensity);
                break;
            default:
                executeSimpleLoad(intensity);
                break;
        }

        Trace.endSection();
    }

    /**
     * 执行帧间负载（混合负载的帧间部分）
     * 使用伪随机帧间隔触发，确保测试可重现
     * 
     * @param loadType 负载类型
     * @param traceTag Trace 标签名称
     */
    public void executeBetweenFrameLoad(@LoadType.Type int loadType, String traceTag) {
        int intensity = LoadConfig.getMixedBetweenFrameIntensity(loadType);
        if (intensity == 0) return;

        // 获取负载级别
        int level = LoadType.getLoadLevel(loadType);

        // 检测负载级别是否变化，如果变化则重新计算目标帧数
        if (level != mCurrentBetweenFrameLoadLevel) {
            mCurrentBetweenFrameLoadLevel = level;
            mNextBetweenFrameTarget = mBetweenFrameCounter + calculateNextBetweenFrameInterval(level);
        }

        // 增加帧计数
        mBetweenFrameCounter++;

        // 检查是否到达执行时机
        if (mBetweenFrameCounter < mNextBetweenFrameTarget) {
            return; // 还没到执行时机，跳过本帧
        }

        // 计算下一次执行的目标帧数
        mNextBetweenFrameTarget = mBetweenFrameCounter + calculateNextBetweenFrameInterval(level);

        // 命中时间窗口后，再按伪随机概率决定是否执行
        if (!shouldExecuteByProbability(loadType)) {
            return;
        }

        // 执行负载
        Trace.beginSection(traceTag);
        mExecutionCounter++;

        // 根据负载级别选择执行方式
        switch (level) {
            case 1: // Light
                executeLightLoad(intensity);
                break;
            case 2: // Medium
                executeMediumLoad(intensity);
                break;
            case 3: // Heavy
                executeHeavyLoad(intensity);
                break;
            default:
                executeSimpleLoad(intensity);
                break;
        }

        Trace.endSection();
    }

    /**
     * 计算下一次帧间负载执行的帧间隔
     * 使用伪随机数生成器，在最小和最大间隔之间生成随机值
     * 
     * @param loadLevel 负载级别 (1=Light, 2=Medium, 3=Heavy)
     * @return 帧间隔数
     */
    private int calculateNextBetweenFrameInterval(int loadLevel) {
        int minInterval = LoadConfig.getBetweenFrameMinIntervalByLevel(loadLevel);
        int maxInterval = LoadConfig.getBetweenFrameMaxIntervalByLevel(loadLevel);

        // 生成 [minInterval, maxInterval] 范围内的伪随机帧间隔
        return minInterval + mBetweenFrameIntervalRandom.nextInt(maxInterval - minInterval + 1);
    }

    /**
     * 执行纯帧间负载（非混合模式）
     * 使用伪随机帧间隔触发，确保测试可重现
     * 
     * @param loadType 负载类型
     * @param traceTag Trace 标签名称
     */
    public void executePureBetweenFrameLoad(@LoadType.Type int loadType, String traceTag) {
        int intensity = LoadConfig.getBetweenFrameIntensity(loadType);
        if (intensity == 0) return;

        // 获取负载级别
        int level = LoadType.getLoadLevel(loadType);

        // 检测负载级别是否变化，如果变化则重新计算目标帧数
        if (level != mCurrentBetweenFrameLoadLevel) {
            mCurrentBetweenFrameLoadLevel = level;
            mNextBetweenFrameTarget = mBetweenFrameCounter + calculateNextBetweenFrameInterval(level);
        }

        // 增加帧计数
        mBetweenFrameCounter++;

        // 检查是否到达执行时机
        if (mBetweenFrameCounter < mNextBetweenFrameTarget) {
            return; // 还没到执行时机，跳过本帧
        }

        // 计算下一次执行的目标帧数
        mNextBetweenFrameTarget = mBetweenFrameCounter + calculateNextBetweenFrameInterval(level);

        // 命中时间窗口后，再按伪随机概率决定是否执行
        if (!shouldExecuteByProbability(loadType)) {
            return;
        }

        // 执行负载
        Trace.beginSection(traceTag);
        mExecutionCounter++;

        // 根据负载级别选择执行方式
        switch (level) {
            case 1: // Light
                executeLightLoad(intensity);
                break;
            case 2: // Medium
                executeMediumLoad(intensity);
                break;
            case 3: // Heavy
                executeHeavyLoad(intensity);
                break;
            default:
                executeSimpleLoad(intensity);
                break;
        }

        Trace.endSection();
    }

    /**
     * 执行超长帧负载
     * @param traceTag Trace 标签名称
     */
    public void executeLongFrameLoad(String traceTag) {
        int intensity = LoadConfig.getLongFrameIntensity();

        Trace.beginSection(traceTag);
        mExecutionCounter++;
        executeHeavyLoad(intensity);
        Trace.endSection();
    }

    // ==================== 不同级别的负载实现 ====================

    /**
     * 执行简单负载 - 最基础的计算
     * 使用数据依赖链防止循环优化
     * @param intensity 负载强度
     */
    private void executeSimpleLoad(int intensity) {
        double sum = mExecutionCounter; // 使用执行计数器作为初始值，增加不可预测性
        for (int i = 0; i < intensity; i++) {
            // 创建数据依赖链：每次迭代依赖前一次结果
            sum = createDependencyChain(sum, i);
            sum += Math.sin(i * 0.1 + sum * 0.001) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        mComputationResult = sum;
        consumeToBlackHole(sum);
    }

    /**
     * 执行轻负载 - 基础数学计算和简单图像处理
     * @param intensity 负载强度
     */
    private void executeLightLoad(int intensity) {
        // 任务1: 基础数学计算（带依赖链）
        double sum = mExecutionCounter;
        for (int i = 1; i <= intensity; i++) {
            double x = i * 0.05;
            // 依赖链：使用前一次结果
            sum = createDependencyChain(sum, i);
            sum += Math.sin(x + sum * 0.0001) * Math.cos(x) + Math.sqrt(i + 1);
            sum += Math.log(i + 1) + Math.exp(-x * 0.1);
        }

        // 任务2: 简单图像处理
        int pixelSum = 0;
        if (mCanvas != null) {
            mPaint.setColor(Color.rgb(
                mComputationRandom.nextInt(256), 
                mComputationRandom.nextInt(256), 
                mComputationRandom.nextInt(256)));

            int graphicsCount = intensity / 10;
            for (int i = 0; i < graphicsCount; i++) {
                float x = mComputationRandom.nextFloat() * BITMAP_SIZE;
                float y = mComputationRandom.nextFloat() * BITMAP_SIZE;
                float radius = 5 + mComputationRandom.nextFloat() * 10;
                mCanvas.drawCircle(x, y, radius, mPaint);
                pixelSum += (int)(x + y + radius); // 收集结果防止优化
            }
        }

        // 任务3: 简单数据处理
        int dataArraySize = intensity / 2;
        int[] dataArray = new int[Math.max(1, dataArraySize)];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = mComputationRandom.nextInt(1000);
        }
        // 简单排序
        java.util.Arrays.sort(dataArray);

        // 存储结果防止编译器优化
        mComputationResult = sum;
        mDataProcessingResult = dataArray.length > 0 ? dataArray[0] : 0;
        mImageProcessingResult = pixelSum;

        // 消费所有结果到黑洞
        consumeToBlackHole(sum);
        consumeToBlackHole(pixelSum);
        consumeToBlackHole(mDataProcessingResult);
    }

    /**
     * 执行中等负载 - 复杂数学计算、图像处理和数据排序
     * @param intensity 负载强度
     */
    private void executeMediumLoad(int intensity) {
        // 任务1: 复杂数学计算（带强依赖链）
        double sum = mExecutionCounter;
        for (int i = 1; i <= intensity; i++) {
            double x = i * 0.03;
            // 强依赖链
            sum = createDependencyChain(sum, i);
            sum += Math.sin(x * Math.PI + sum * 0.00001) * Math.cos(x * Math.PI / 2) + 
                   Math.pow(x, 2.0) + Math.log(i + 1) * Math.exp(-x * 0.08);
            sum += Math.atan2(Math.sin(x + sum * 0.00001), Math.cos(x)) + Math.tanh(x * 0.03);
        }

        // 任务2: 3x3矩阵运算
        double[][] matrixA = new double[3][3];
        double[][] matrixB = new double[3][3];
        double[][] result = new double[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                matrixA[i][j] = mComputationRandom.nextDouble() * 10 - 5;
                matrixB[i][j] = mComputationRandom.nextDouble() * 10 - 5;
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }

        // 任务3: 图像处理
        int pixelSum = 0;
        if (mCanvas != null) {
            mPaint.setColor(Color.rgb(
                mComputationRandom.nextInt(256), 
                mComputationRandom.nextInt(256), 
                mComputationRandom.nextInt(256)));
            mPaint.setAntiAlias(true);

            int graphicsCount = intensity / 4;
            for (int i = 0; i < graphicsCount; i++) {
                float x = mComputationRandom.nextFloat() * BITMAP_SIZE;
                float y = mComputationRandom.nextFloat() * BITMAP_SIZE;
                float radius = 8 + mComputationRandom.nextFloat() * 15;

                mCanvas.drawCircle(x, y, radius, mPaint);
                mCanvas.drawCircle(x, y, radius * 0.6f, mPaint);

                if (i % 2 == 0) {
                    mCanvas.drawRect(x - radius, y - radius, x + radius, y + radius, mPaint);
                }
                pixelSum += (int)(x + y + radius);
            }
        }

        // 任务4: 数据处理
        int dataArraySize = intensity;
        int[] dataArray = new int[dataArraySize];
        for (int i = 0; i < dataArraySize; i++) {
            dataArray[i] = mComputationRandom.nextInt(1500);
        }

        // 归并排序
        int[] sortedArray = mergeSort(dataArray.clone());

        // 二分查找
        int searchCount = 0;
        int searchTimes = intensity / 10;
        for (int i = 0; i < searchTimes; i++) {
            int target = mComputationRandom.nextInt(1500);
            if (binarySearch(sortedArray, target) >= 0) {
                searchCount++;
            }
        }

        // 任务5: 字符串处理（使用伪随机替代System.nanoTime）
        StringBuilder sb = new StringBuilder();
        int stringCount = intensity / 4;
        for (int i = 0; i < stringCount; i++) {
            // 使用固定种子的随机数替代System.nanoTime()
            sb.append("MediumTask_").append(i).append("_")
              .append(mStringRandom.nextInt(500)).append("_");
        }
        String processedString = sb.toString().toUpperCase();

        // 存储结果防止编译器优化
        mComputationResult = sum + result[0][0] + result[1][1] + result[2][2];
        mImageProcessingResult = processedString.length() + pixelSum;
        mDataProcessingResult = searchCount + sortedArray[0];

        // 消费所有结果到黑洞
        consumeToBlackHole(mComputationResult);
        consumeToBlackHole(mImageProcessingResult);
        consumeToBlackHole(mDataProcessingResult);
    }

    /**
     * 执行重负载 - 包含复杂的数学计算、矩阵运算、图像处理、排序算法和字符串处理
     * @param intensity 负载强度
     */
    private void executeHeavyLoad(int intensity) {
        // 任务1: 高复杂度数学计算（带强依赖链防止并行优化）
        double sum = mExecutionCounter;
        for (int i = 1; i <= intensity * 2; i++) {
            double x = i * 0.02;
            // 强依赖链：每次迭代必须等待前一次完成
            sum = createDependencyChain(sum, i);
            // 复杂的三角函数和指数函数计算
            sum += Math.sin(x * Math.PI + sum * 0.000001) * Math.cos(x * Math.PI / 2) + 
                   Math.pow(x, 2.5) + Math.log(i + 1) * Math.exp(-x * 0.05);
            // 双曲函数和反三角函数
            sum += Math.atan2(Math.sin(x + sum * 0.000001), Math.cos(x)) + 
                   Math.sinh(x * 0.01) + Math.cosh(x * 0.01) + 
                   Math.tanh(x * 0.02);
            // 贝塞尔函数近似和伽马函数近似
            sum += approximateBesselJ0(x + sum * 0.000001) + logGammaApproximation(x + 1);
        }

        // 任务2: 大型矩阵运算 - 5x5矩阵乘法
        double[][] matrixA = new double[5][5];
        double[][] matrixB = new double[5][5];
        double[][] result = new double[5][5];

        // 初始化矩阵
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                matrixA[i][j] = mComputationRandom.nextDouble() * 20 - 10;
                matrixB[i][j] = mComputationRandom.nextDouble() * 20 - 10;
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

        // 任务3: 高密度图像处理
        int pixelSum = 0;
        if (mCanvas != null) {
            mPaint.setColor(Color.rgb(
                mComputationRandom.nextInt(256), 
                mComputationRandom.nextInt(256), 
                mComputationRandom.nextInt(256)));
            mPaint.setAntiAlias(true);

            // 绘制大量复杂图形
            int graphicsCount = intensity / 2;
            for (int i = 0; i < graphicsCount; i++) {
                float x = mComputationRandom.nextFloat() * BITMAP_SIZE;
                float y = mComputationRandom.nextFloat() * BITMAP_SIZE;
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

                pixelSum += (int)(x + y + radius);
            }
        }

        // 任务4: 复杂数据处理
        int dataArraySize = intensity * 2;
        int[] dataArray = new int[dataArraySize];
        for (int i = 0; i < dataArraySize; i++) {
            dataArray[i] = mComputationRandom.nextInt(2000);
        }

        // 归并排序
        int[] sortedArray = mergeSort(dataArray.clone());

        // 堆排序
        heapSort(dataArray);

        // 多次二分查找
        int searchCount = 0;
        int searchTimes = intensity / 5;
        for (int i = 0; i < searchTimes; i++) {
            int target = mComputationRandom.nextInt(2000);
            if (binarySearch(sortedArray, target) >= 0) {
                searchCount++;
            }
        }

        // 任务5: 算法运算 - 动态规划和递归计算
        int fibResult = fibonacci(30);
        long factorialResult = factorial(15);

        // 任务6: 高级字符串处理（使用伪随机替代System.nanoTime）
        StringBuilder sb = new StringBuilder();
        int stringCount = intensity;
        for (int i = 0; i < stringCount; i++) {
            // 使用固定种子的随机数生成器替代 System.nanoTime()
            // 确保每次执行产生相同的字符串序列
            sb.append("LoadTask_").append(i).append("_")
              .append(mStringRandom.nextInt(1000))
              .append("_Complex_")
              .append(mStringRandom.nextInt(1000)).append("_");
        }
        String processedString = sb.toString().toUpperCase().replace("_", "-");
        String[] parts = processedString.split("-");

        // 字符串编码处理
        byte[] encodedBytes = processedString.getBytes();
        String reConstructed = new String(encodedBytes);

        // 存储结果防止编译器优化
        mComputationResult = sum + result[0][0] + result[2][2] + result[4][4];
        mImageProcessingResult = processedString.length() + parts.length + reConstructed.length() + pixelSum;
        mDataProcessingResult = searchCount + sortedArray[0] + sortedArray[sortedArray.length - 1];
        mComplexMathResult = determinant + fibResult + factorialResult;

        // 消费所有结果到黑洞，确保编译器不会优化掉任何计算
        consumeToBlackHole(mComputationResult);
        consumeToBlackHole(mImageProcessingResult);
        consumeToBlackHole(mDataProcessingResult);
        consumeToBlackHole(mComplexMathResult);

        // 验证计算结果，使用结果影响后续状态（增强防优化）
        if (Math.abs(mComputationResult) > 0 && mImageProcessingResult > 0 && 
            mDataProcessingResult >= 0 && Math.abs(mComplexMathResult) > 0) {
            mPaint.setAlpha((int)(Math.abs(mComputationResult + mComplexMathResult) % 255) + 1);
            mPaint.setStrokeWidth((mImageProcessingResult % 15) + 1);
        }
    }

    // ==================== 数学工具方法 ====================

    /**
     * 贝塞尔函数J0近似
     * 使用Chebyshev多项式近似，确保计算复杂度
     */
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

    /**
     * 伽马函数对数近似（Stirling近似）
     */
    private double logGammaApproximation(double x) {
        return (x - 0.5) * Math.log(x) - x + 0.5 * Math.log(2 * Math.PI) +
               1.0 / (12.0 * x) - 1.0 / (360.0 * x * x * x);
    }

    /**
     * 计算5x5矩阵行列式（使用余子式展开，确保计算复杂度）
     */
    private double calculateDeterminant5x5(double[][] matrix) {
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
        return minor[0][0] * (minor[1][1] * (minor[2][2] * minor[3][3] - minor[2][3] * minor[3][2]) -
                              minor[1][2] * (minor[2][1] * minor[3][3] - minor[2][3] * minor[3][1]) +
                              minor[1][3] * (minor[2][1] * minor[3][2] - minor[2][2] * minor[3][1]));
    }

    // ==================== 排序和搜索算法 ====================

    /**
     * 归并排序
     * 使用递归实现，确保算法复杂度不被优化
     */
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

    /**
     * 堆排序
     * 原地排序，确保内存访问模式不可预测
     */
    private void heapSort(int[] arr) {
        int n = arr.length;

        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }

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

    /**
     * 二分查找
     */
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

    // ==================== 算法工具方法 ====================

    /**
     * 斐波那契数计算（使用动态规划，避免递归优化）
     */
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

    /**
     * 阶乘计算
     */
    private long factorial(int n) {
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    // ==================== Getter 方法（用于日志输出和验证）====================

    public double getComputationResult() {
        return mComputationResult;
    }

    public int getImageProcessingResult() {
        return mImageProcessingResult;
    }

    public long getDataProcessingResult() {
        return mDataProcessingResult;
    }

    public double getComplexMathResult() {
        return mComplexMathResult;
    }

    /**
     * 获取黑洞值（用于验证计算确实执行）
     * @return 黑洞累积值
     */
    public static long getBlackHoleValue() {
        return sBlackHole;
    }

    /**
     * 获取执行计数器（用于验证执行次数）
     * @return 执行计数
     */
    public long getExecutionCounter() {
        return mExecutionCounter;
    }
}
