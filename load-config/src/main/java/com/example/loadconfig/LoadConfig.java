package com.example.loadconfig;

/**
 * 统一的负载配置中心
 * 
 * 集中管理所有模块的负载参数，便于调试和优化。
 * 
 * 设计原理：
 * 1. 基于Android 120fps渲染管线（每帧8.33ms）
 * 2. 负载递增策略：1.5倍递增（轻 → 中 → 重）
 *    - 科学依据：Weber-Fechner定律表明人眼感知呈对数关系
 *    - 1.5倍递增在感知上有明显差异，同时不会跨度太大
 * 3. 所有随机均使用固定种子，确保测试可重现性
 * 
 * 负载分级策略：
 * 1. 帧内负载（In-Frame）- 保持原有设计
 *    - 轻负载：150
 *    - 中负载：600
 *    - 高负载：1000
 * 
 * 2. 混合负载 - doFrame部分
 *    - 轻负载：1000
 *    - 中负载：1500 (1.5x)
 *    - 高负载：2250 (1.5x)
 * 
 * 3. 帧间负载（Between-Frame）
 *    - 轻负载：1000
 *    - 中负载：1500 (1.5x)
 *    - 高负载：2250 (1.5x)
 * 
 * 4. 混合负载 - 帧间部分
 *    - 轻负载：1000
 *    - 中负载：1500 (1.5x)
 *    - 高负载：2250 (1.5x)
 * 
 * 5. 混合负载总和趋势
 *    - 轻负载总和：2000 (1000 + 1000)
 *    - 中负载总和：3000 (1500 + 1500)
 *    - 高负载总和：4500 (2250 + 2250)
 */
public final class LoadConfig {

    // ==================== 帧率基准配置 ====================
    
    /** 目标帧率 (fps) */
    public static final int TARGET_FPS = 120;
    
    /** 单帧时长 (ms) - 120fps = 8.33ms/frame */
    public static final double FRAME_DURATION_MS = 1000.0 / TARGET_FPS;  // 8.33ms
    
    /** 负载递增倍数 */
    public static final double LOAD_INCREMENT_MULTIPLIER = 1.5;

    // ==================== 任务调度配置 ====================
    
    /** 最小任务执行间隔（1帧 @ 120fps = 8ms） */
    public static final int MIN_TASK_INTERVAL_MS = 8;
    
    /** 最大任务执行间隔（5帧 @ 120fps = 42ms） */
    public static final int MAX_TASK_INTERVAL_MS = 42;
    
    /** 任务间隔随机种子（确保测试可重现） */
    public static final long TASK_INTERVAL_SEED = 12345L;
    
    /** doFrame间隔随机种子 */
    public static final long DOFRAME_INTERVAL_SEED = 11111L;
    
    /** 计算逻辑随机种子 */
    public static final long COMPUTATION_SEED = 67890L;
    
    /** 超长帧触发时间随机种子（固定种子确保每次触发位置一致） */
    public static final long LONG_FRAME_SEED = 54321L;
    
    /** 数据生成随机种子（用于生成测试数据，确保数据可重现） */
    public static final long DATA_GENERATION_SEED = 42L;
    
    /** 帧间负载随机种子（确保伪随机可重现） */
    public static final long BETWEEN_FRAME_INTERVAL_SEED = 88888L;
    
    /** 字符串生成随机种子（替代System.nanoTime()确保可重现） */
    public static final long STRING_GENERATION_SEED = 99999L;
    
    // ==================== 帧间负载触发间隔配置（帧数）====================
    
    /** 轻负载帧间隔 - 最小帧数 */
    public static final int LIGHT_BETWEEN_FRAME_MIN_INTERVAL = 4;
    
    /** 轻负载帧间隔 - 最大帧数 */
    public static final int LIGHT_BETWEEN_FRAME_MAX_INTERVAL = 6;
    
    /** 中负载帧间隔 - 最小帧数 */
    public static final int MEDIUM_BETWEEN_FRAME_MIN_INTERVAL = 3;
    
    /** 中负载帧间隔 - 最大帧数 */
    public static final int MEDIUM_BETWEEN_FRAME_MAX_INTERVAL = 5;
    
    /** 重负载帧间隔 - 最小帧数 */
    public static final int HEAVY_BETWEEN_FRAME_MIN_INTERVAL = 2;
    
    /** 重负载帧间隔 - 最大帧数 */
    public static final int HEAVY_BETWEEN_FRAME_MAX_INTERVAL = 4;
    
    // ==================== 按负载类型区分的数据生成配置 ====================
    
    /** 轻负载数据生成种子 */
    public static final long LIGHT_DATA_SEED = 42L;
    
    /** 中负载数据生成种子 */
    public static final long MEDIUM_DATA_SEED = 142L;
    
    /** 高负载数据生成种子 */
    public static final long HEAVY_DATA_SEED = 242L;
    
    /** 轻负载位置偏移量（用于生成评论和点赞数据） */
    public static final int LIGHT_POSITION_OFFSET = 0;
    
    /** 中负载位置偏移量（用于生成评论和点赞数据） */
    public static final int MEDIUM_POSITION_OFFSET = 100;
    
    /** 高负载位置偏移量（用于生成评论和点赞数据） */
    public static final int HEAVY_POSITION_OFFSET = 200;
    
    // ==================== 数据生成随机种子乘数 ====================
    
    /** 评论数据随机种子 - 位置乘数 */
    public static final int COMMENT_SEED_POSITION_MULTIPLIER = 100;
    
    /** 评论数据随机种子 - 负载类型乘数 */
    public static final int COMMENT_SEED_LOADTYPE_MULTIPLIER = 10;
    
    /** 点赞数据随机种子 - 位置乘数 */
    public static final int PRAISE_SEED_POSITION_MULTIPLIER = 50;
    
    /** 点赞数据随机种子 - 负载类型乘数 */
    public static final int PRAISE_SEED_LOADTYPE_MULTIPLIER = 5;
    
    /** 评论用户ID起始值 */
    public static final int COMMENT_USER_ID_BASE = 20000;
    
    /** 点赞用户ID起始值 */
    public static final int PRAISE_USER_ID_BASE = 30000;

    // ==================== 帧内负载配置 ====================
    
    /** 帧内轻负载强度 */
    public static final int IN_FRAME_LIGHT_INTENSITY = 200;
    
    /** 帧内中负载强度 */
    public static final int IN_FRAME_MEDIUM_INTENSITY = 600;
    
    /** 帧内高负载强度 */
    public static final int IN_FRAME_HEAVY_INTENSITY = 1000;
    
    // ==================== 超长帧负载配置 ====================
    
    /** 超长帧负载强度（HEAVY的20倍，用于模拟极端卡顿） */
    public static final int LONG_FRAME_INTENSITY = 4500;
    
    /** 超长帧在滑动周期内的最小触发次数 */
    public static final int LONG_FRAME_MIN_TRIGGERS = 2;
    
    /** 超长帧在滑动周期内的最大触发次数 */
    public static final int LONG_FRAME_MAX_TRIGGERS = 3;
    
    /** 超长帧滑动周期时长（毫秒） */
    public static final int LONG_FRAME_SCROLL_PERIOD_MS = 2000;
    
    /** 超长帧最小触发间隔（毫秒），避免连续触发 */
    public static final int LONG_FRAME_MIN_INTERVAL_MS = 300;
    
    // ==================== 混合负载 - doFrame配置（1.5倍递增）====================
    
    /** 混合轻负载 - doFrame任务强度基准值 */
    public static final int MIXED_DOFRAME_LIGHT_INTENSITY = 1000;
    
    /** 混合中负载 - doFrame任务强度（1.5倍） */
    public static final int MIXED_DOFRAME_MEDIUM_INTENSITY = 1500;
    
    /** 混合高负载 - doFrame任务强度（2.25倍） */
    public static final int MIXED_DOFRAME_HEAVY_INTENSITY = 2250;
    
    // ==================== 帧间负载配置（1.5倍递增）====================
    
    /** 帧间轻负载强度基准值 */
    public static final int BETWEEN_FRAME_LIGHT_INTENSITY = 1000;
    
    /** 帧间中负载强度（1.5倍） */
    public static final int BETWEEN_FRAME_MEDIUM_INTENSITY = 1500;
    
    /** 帧间高负载强度（2.25倍） */
    public static final int BETWEEN_FRAME_HEAVY_INTENSITY = 2250;
    
    // ==================== 混合负载 - 帧间配置（1.5倍递增）====================
    
    /** 混合轻负载 - 帧间任务强度基准值 */
    public static final int MIXED_BETWEEN_FRAME_LIGHT_INTENSITY = 1000;
    
    /** 混合中负载 - 帧间任务强度（1.5倍） */
    public static final int MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY = 1500;
    
    /** 混合高负载 - 帧间任务强度（2.25倍） */
    public static final int MIXED_BETWEEN_FRAME_HEAVY_INTENSITY = 2250;
    
    // ==================== 帧间任务概率配置 ====================
    
    /** 轻负载任务执行概率 */
    public static final float LIGHT_TASK_PROBABILITY = 0.7f;
    
    /** 中负载任务执行概率 */
    public static final float MEDIUM_TASK_PROBABILITY = 0.8f;
    
    /** 高负载任务执行概率 */
    public static final float HEAVY_TASK_PROBABILITY = 1.0f;
    
    // ==================== Bitmap大小配置 ====================
    
    /** 轻负载位图大小 */
    public static final int LIGHT_BITMAP_SIZE = 200;
    
    /** 中负载位图大小 */
    public static final int MEDIUM_BITMAP_SIZE = 300;
    
    /** 高负载位图大小 */
    public static final int HEAVY_BITMAP_SIZE = 450;
    
    // ==================== 私有构造函数 ====================
    
    private LoadConfig() {
        throw new UnsupportedOperationException("LoadConfig is a utility class and cannot be instantiated");
    }

    // ==================== 获取负载强度的工具方法 ====================
    
    /**
     * 获取帧内负载强度
     * @param loadType 负载类型
     * @return 负载强度（循环迭代次数）
     */
    public static int getInFrameIntensity(@LoadType.Type int loadType) {
        switch (loadType) {
            case LoadType.LIGHT:
            case LoadType.LIGHT_MIXED:
                return IN_FRAME_LIGHT_INTENSITY;
            case LoadType.MEDIUM:
            case LoadType.MEDIUM_MIXED:
                return IN_FRAME_MEDIUM_INTENSITY;
            case LoadType.HEAVY:
            case LoadType.HEAVY_MIXED:
                return IN_FRAME_HEAVY_INTENSITY;
            case LoadType.LONG_FRAME:
                return LONG_FRAME_INTENSITY;
            default:
                return 0;
        }
    }
    
    /**
     * 获取超长帧负载强度
     * @return 超长帧负载强度（HEAVY的20倍）
     */
    public static int getLongFrameIntensity() {
        return LONG_FRAME_INTENSITY;
    }
    
    /**
     * 计算超长帧在当前滑动周期内应该触发的次数
     * 使用固定种子 LONG_FRAME_SEED，确保每次返回相同结果
     * @return 触发次数（2-3次）
     */
    public static int getLongFrameTriggerCount() {
        java.util.Random random = new java.util.Random(LONG_FRAME_SEED);
        return LONG_FRAME_MIN_TRIGGERS + random.nextInt(LONG_FRAME_MAX_TRIGGERS - LONG_FRAME_MIN_TRIGGERS + 1);
    }
    
    /**
     * 计算超长帧的触发时间点（在滑动周期内伪随机分布）
     * 使用固定种子 LONG_FRAME_SEED，确保每次进入应用触发位置一致
     * @param triggerCount 触发次数
     * @return 触发时间点数组（毫秒）
     */
    public static long[] getLongFrameTriggerTimes(int triggerCount) {
        // 使用固定种子，需要跳过 getLongFrameTriggerCount 消耗的随机数
        java.util.Random random = new java.util.Random(LONG_FRAME_SEED);
        random.nextInt(); // 跳过第一个随机数（getLongFrameTriggerCount 使用）
        
        long[] times = new long[triggerCount];
        
        // 将滑动周期分成多个区间，每个区间内随机一个触发点
        int segmentSize = LONG_FRAME_SCROLL_PERIOD_MS / triggerCount;
        
        for (int i = 0; i < triggerCount; i++) {
            int segmentStart = i * segmentSize;
            int segmentEnd = segmentStart + segmentSize - LONG_FRAME_MIN_INTERVAL_MS;
            if (segmentEnd <= segmentStart) {
                segmentEnd = segmentStart + 100;
            }
            times[i] = segmentStart + random.nextInt(segmentEnd - segmentStart);
        }
        
        return times;
    }
    
    /**
     * 获取混合负载的doFrame任务强度
     * @param loadType 负载类型
     * @return doFrame任务强度
     */
    public static int getDoFrameIntensity(@LoadType.Type int loadType) {
        switch (loadType) {
            case LoadType.LIGHT_MIXED:
                return MIXED_DOFRAME_LIGHT_INTENSITY;
            case LoadType.MEDIUM_MIXED:
                return MIXED_DOFRAME_MEDIUM_INTENSITY;
            case LoadType.HEAVY_MIXED:
                return MIXED_DOFRAME_HEAVY_INTENSITY;
            default:
                return 0;
        }
    }
    
    /**
     * 获取帧间负载强度
     * @param loadType 负载类型
     * @return 帧间负载强度
     */
    public static int getBetweenFrameIntensity(@LoadType.Type int loadType) {
        switch (loadType) {
            case LoadType.LIGHT_BETWEEN_FRAMES:
                return BETWEEN_FRAME_LIGHT_INTENSITY;
            case LoadType.MEDIUM_BETWEEN_FRAMES:
                return BETWEEN_FRAME_MEDIUM_INTENSITY;
            case LoadType.HEAVY_BETWEEN_FRAMES:
                return BETWEEN_FRAME_HEAVY_INTENSITY;
            default:
                return 0;
        }
    }
    
    /**
     * 获取混合负载的帧间任务强度
     * @param loadType 负载类型
     * @return 混合负载的帧间任务强度
     */
    public static int getMixedBetweenFrameIntensity(@LoadType.Type int loadType) {
        switch (loadType) {
            case LoadType.LIGHT_MIXED:
                return MIXED_BETWEEN_FRAME_LIGHT_INTENSITY;
            case LoadType.MEDIUM_MIXED:
                return MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY;
            case LoadType.HEAVY_MIXED:
                return MIXED_BETWEEN_FRAME_HEAVY_INTENSITY;
            default:
                return 0;
        }
    }
    
    /**
     * 获取综合帧间负载强度（帧间负载或混合负载的帧间部分）
     * @param loadType 负载类型
     * @return 帧间负载强度
     */
    public static int getCombinedBetweenFrameIntensity(@LoadType.Type int loadType) {
        if (LoadType.isBetweenFramesLoad(loadType)) {
            return getBetweenFrameIntensity(loadType);
        } else if (LoadType.isMixedLoad(loadType)) {
            return getMixedBetweenFrameIntensity(loadType);
        }
        return 0;
    }
    
    /**
     * 获取任务执行概率
     * @param loadType 负载类型
     * @return 执行概率 (0.0-1.0)
     */
    public static float getTaskProbability(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return LIGHT_TASK_PROBABILITY;
            case 2:
                return MEDIUM_TASK_PROBABILITY;
            case 3:
                return HEAVY_TASK_PROBABILITY;
            default:
                return 0f;
        }
    }
    
    /**
     * 获取位图大小
     * @param loadType 负载类型
     * @return 位图边长（像素）
     */
    public static int getBitmapSize(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return LIGHT_BITMAP_SIZE;
            case 2:
                return MEDIUM_BITMAP_SIZE;
            case 3:
                return HEAVY_BITMAP_SIZE;
            default:
                return LIGHT_BITMAP_SIZE;
        }
    }
    
    /**
     * 获取循环计数（用于帧间负载的计算循环）
     * @param loadType 负载类型
     * @return 循环次数
     */
    public static int getComputationLoopCount(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return BETWEEN_FRAME_LIGHT_INTENSITY;
            case 2:
                return BETWEEN_FRAME_MEDIUM_INTENSITY;
            case 3:
                return BETWEEN_FRAME_HEAVY_INTENSITY;
            default:
                return 0;
        }
    }
    
    /**
     * 获取帧间负载的最小触发间隔（帧数）
     * @param loadType 负载类型
     * @return 最小帧间隔
     */
    public static int getBetweenFrameMinInterval(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return LIGHT_BETWEEN_FRAME_MIN_INTERVAL;
            case 2:
                return MEDIUM_BETWEEN_FRAME_MIN_INTERVAL;
            case 3:
                return HEAVY_BETWEEN_FRAME_MIN_INTERVAL;
            default:
                return LIGHT_BETWEEN_FRAME_MIN_INTERVAL;
        }
    }
    
    /**
     * 获取帧间负载的最大触发间隔（帧数）
     * @param loadType 负载类型
     * @return 最大帧间隔
     */
    public static int getBetweenFrameMaxInterval(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return LIGHT_BETWEEN_FRAME_MAX_INTERVAL;
            case 2:
                return MEDIUM_BETWEEN_FRAME_MAX_INTERVAL;
            case 3:
                return HEAVY_BETWEEN_FRAME_MAX_INTERVAL;
            default:
                return LIGHT_BETWEEN_FRAME_MAX_INTERVAL;
        }
    }
    
    /**
     * 获取数据生成种子（按负载类型区分）
     * @param loadType 负载类型
     * @return 数据生成种子
     */
    public static long getDataGenerationSeed(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return LIGHT_DATA_SEED;
            case 2:
                return MEDIUM_DATA_SEED;
            case 3:
                return HEAVY_DATA_SEED;
            default:
                return DATA_GENERATION_SEED;
        }
    }
    
    /**
     * 获取位置偏移量（按负载类型区分，用于生成评论和点赞数据）
     * @param loadType 负载类型
     * @return 位置偏移量
     */
    public static int getPositionOffset(@LoadType.Type int loadType) {
        int level = LoadType.getLoadLevel(loadType);
        switch (level) {
            case 1:
                return LIGHT_POSITION_OFFSET;
            case 2:
                return MEDIUM_POSITION_OFFSET;
            case 3:
                return HEAVY_POSITION_OFFSET;
            default:
                return 0;
        }
    }
    
    // ==================== 配置描述和验证 ====================
    
    /**
     * 获取负载配置的描述信息
     * @param loadType 负载类型
     * @return 描述字符串
     */
    public static String getDescription(@LoadType.Type int loadType) {
        switch (loadType) {
            case LoadType.MINIMAL:
                return "最轻负载 - 不添加任何额外负载";
            case LoadType.LIGHT:
                return String.format("帧内轻负载 - 强度:%d", IN_FRAME_LIGHT_INTENSITY);
            case LoadType.MEDIUM:
                return String.format("帧内中负载 - 强度:%d", IN_FRAME_MEDIUM_INTENSITY);
            case LoadType.HEAVY:
                return String.format("帧内高负载 - 强度:%d", IN_FRAME_HEAVY_INTENSITY);
            case LoadType.LIGHT_BETWEEN_FRAMES:
                return String.format("帧间轻负载 - 强度:%d, 概率:%.0f%%", 
                        BETWEEN_FRAME_LIGHT_INTENSITY, LIGHT_TASK_PROBABILITY * 100);
            case LoadType.MEDIUM_BETWEEN_FRAMES:
                return String.format("帧间中负载 - 强度:%d (1.5x), 概率:%.0f%%", 
                        BETWEEN_FRAME_MEDIUM_INTENSITY, MEDIUM_TASK_PROBABILITY * 100);
            case LoadType.HEAVY_BETWEEN_FRAMES:
                return String.format("帧间高负载 - 强度:%d (2.25x), 概率:%.0f%%", 
                        BETWEEN_FRAME_HEAVY_INTENSITY, HEAVY_TASK_PROBABILITY * 100);
            case LoadType.LIGHT_MIXED:
                return String.format("混合轻负载 - doFrame:%d, 帧间:%d (总和:%d)", 
                        MIXED_DOFRAME_LIGHT_INTENSITY, MIXED_BETWEEN_FRAME_LIGHT_INTENSITY,
                        MIXED_DOFRAME_LIGHT_INTENSITY + MIXED_BETWEEN_FRAME_LIGHT_INTENSITY);
            case LoadType.MEDIUM_MIXED:
                return String.format("混合中负载 - doFrame:%d, 帧间:%d (总和:%d, 1.5x)", 
                        MIXED_DOFRAME_MEDIUM_INTENSITY, MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY,
                        MIXED_DOFRAME_MEDIUM_INTENSITY + MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY);
            case LoadType.HEAVY_MIXED:
                return String.format("混合高负载 - doFrame:%d, 帧间:%d (总和:%d, 2.25x)", 
                        MIXED_DOFRAME_HEAVY_INTENSITY, MIXED_BETWEEN_FRAME_HEAVY_INTENSITY,
                        MIXED_DOFRAME_HEAVY_INTENSITY + MIXED_BETWEEN_FRAME_HEAVY_INTENSITY);
            case LoadType.LONG_FRAME:
                return String.format("超长帧负载 - 强度:%d (HEAVY×20), 每%dms内触发%d-%d次", 
                        LONG_FRAME_INTENSITY, LONG_FRAME_SCROLL_PERIOD_MS,
                        LONG_FRAME_MIN_TRIGGERS, LONG_FRAME_MAX_TRIGGERS);
            default:
                return "未知负载配置";
        }
    }
    
    /**
     * 验证负载配置的科学性
     * - 帧间负载、混合负载采用1.5倍递增
     * - 帧内负载保持原有设计（递增但不强制1.5倍）
     * @return 验证结果
     */
    public static boolean validateConfig() {
        // 允许的误差范围（由于取整）
        double tolerance = 0.1;
        
        // 验证帧内负载递增（不强制1.5倍）
        boolean inFrameValid = 
                (IN_FRAME_MEDIUM_INTENSITY > IN_FRAME_LIGHT_INTENSITY) &&
                (IN_FRAME_HEAVY_INTENSITY > IN_FRAME_MEDIUM_INTENSITY);
        
        // 验证doFrame负载呈1.5倍递增
        boolean doFrameValid = 
                isApproximatelyEqual(MIXED_DOFRAME_MEDIUM_INTENSITY, MIXED_DOFRAME_LIGHT_INTENSITY * 1.5, tolerance) &&
                isApproximatelyEqual(MIXED_DOFRAME_HEAVY_INTENSITY, MIXED_DOFRAME_MEDIUM_INTENSITY * 1.5, tolerance);
        
        // 验证帧间负载呈1.5倍递增
        boolean betweenFrameValid = 
                isApproximatelyEqual(BETWEEN_FRAME_MEDIUM_INTENSITY, BETWEEN_FRAME_LIGHT_INTENSITY * 1.5, tolerance) &&
                isApproximatelyEqual(BETWEEN_FRAME_HEAVY_INTENSITY, BETWEEN_FRAME_MEDIUM_INTENSITY * 1.5, tolerance);
        
        // 验证混合负载的帧间部分也呈1.5倍递增
        boolean mixedBetweenFrameValid = 
                isApproximatelyEqual(MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY, MIXED_BETWEEN_FRAME_LIGHT_INTENSITY * 1.5, tolerance) &&
                isApproximatelyEqual(MIXED_BETWEEN_FRAME_HEAVY_INTENSITY, MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY * 1.5, tolerance);
        
        // 验证混合负载总体趋势递增
        int totalLight = MIXED_DOFRAME_LIGHT_INTENSITY + MIXED_BETWEEN_FRAME_LIGHT_INTENSITY;
        int totalMedium = MIXED_DOFRAME_MEDIUM_INTENSITY + MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY;
        int totalHeavy = MIXED_DOFRAME_HEAVY_INTENSITY + MIXED_BETWEEN_FRAME_HEAVY_INTENSITY;
        boolean totalValid = (totalMedium > totalLight) && (totalHeavy > totalMedium);
        
        // 验证Bitmap大小也呈1.5倍递增
        boolean bitmapValid = 
                isApproximatelyEqual(MEDIUM_BITMAP_SIZE, LIGHT_BITMAP_SIZE * 1.5, tolerance) &&
                isApproximatelyEqual(HEAVY_BITMAP_SIZE, MEDIUM_BITMAP_SIZE * 1.5, tolerance);
        
        return inFrameValid && doFrameValid && betweenFrameValid && 
               mixedBetweenFrameValid && totalValid && bitmapValid;
    }
    
    /**
     * 检查两个值是否在容差范围内近似相等
     */
    private static boolean isApproximatelyEqual(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) / expected <= tolerance;
    }
    
    /**
     * 获取配置摘要信息（用于调试）
     * @return 配置摘要字符串
     */
    public static String getConfigSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LoadConfig Summary (120fps) ===\n");
        sb.append(String.format("Target FPS: %d (%.2fms/frame)\n", TARGET_FPS, FRAME_DURATION_MS));
        sb.append(String.format("Task Interval: %d-%dms\n", MIN_TASK_INTERVAL_MS, MAX_TASK_INTERVAL_MS));
        sb.append("\n--- In-Frame Load ---\n");
        sb.append(String.format("  Light:  %d\n", IN_FRAME_LIGHT_INTENSITY));
        sb.append(String.format("  Medium: %d\n", IN_FRAME_MEDIUM_INTENSITY));
        sb.append(String.format("  Heavy:  %d\n", IN_FRAME_HEAVY_INTENSITY));
        sb.append("\n--- Mixed doFrame Load ---\n");
        sb.append(String.format("  Light:  %d (base)\n", MIXED_DOFRAME_LIGHT_INTENSITY));
        sb.append(String.format("  Medium: %d (1.5x)\n", MIXED_DOFRAME_MEDIUM_INTENSITY));
        sb.append(String.format("  Heavy:  %d (2.25x)\n", MIXED_DOFRAME_HEAVY_INTENSITY));
        sb.append("\n--- Between-Frame Load ---\n");
        sb.append(String.format("  Light:  %d (base)\n", BETWEEN_FRAME_LIGHT_INTENSITY));
        sb.append(String.format("  Medium: %d (1.5x)\n", BETWEEN_FRAME_MEDIUM_INTENSITY));
        sb.append(String.format("  Heavy:  %d (2.25x)\n", BETWEEN_FRAME_HEAVY_INTENSITY));
        sb.append("\n--- Mixed Total Load ---\n");
        sb.append(String.format("  Light:  %d\n", MIXED_DOFRAME_LIGHT_INTENSITY + MIXED_BETWEEN_FRAME_LIGHT_INTENSITY));
        sb.append(String.format("  Medium: %d\n", MIXED_DOFRAME_MEDIUM_INTENSITY + MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY));
        sb.append(String.format("  Heavy:  %d\n", MIXED_DOFRAME_HEAVY_INTENSITY + MIXED_BETWEEN_FRAME_HEAVY_INTENSITY));
        sb.append("\nConfig Valid: ").append(validateConfig());
        return sb.toString();
    }
}
