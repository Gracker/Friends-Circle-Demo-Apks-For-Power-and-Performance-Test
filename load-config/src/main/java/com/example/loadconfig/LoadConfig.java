package com.example.loadconfig;

/**
 * 统一的负载配置中心
 * 
 * 集中管理所有模块的负载参数，便于调试和优化。
 * 
 * 设计原理：
 * 1. 基于Android 60fps渲染管线（每帧16.67ms）
 * 2. doFrame负载：几何级数增长（1000→2000→4000）
 * 3. 帧间负载：反比例衰减，平衡整体负载
 * 4. 任务间隔：1-5帧（16-83ms），确保测试可重现性
 * 
 * 负载分级策略：
 * 1. doFrame负载 - 几何级数增长
 *    - 轻负载：1000
 *    - 中负载：2000 (2x)
 *    - 高负载：4000 (2x)
 * 
 * 2. 帧间负载 - 反比例平衡设计
 *    - 轻负载：2400
 *    - 中负载：1600 (↓33%)
 *    - 高负载：1067 (↓33%)
 * 
 * 3. 总负载趋势 - 线性递增
 *    - 轻负载总和：3400
 *    - 中负载总和：3600 (↑6%)
 *    - 高负载总和：5067 (↑41%)
 * 
 * 科学依据：
 * - Weber-Fechner定律：人眼感知呈对数关系，doFrame采用几何级数
 * - 负载平衡理论：帧间负载反比例衰减，平衡整体性能
 * - Android渲染管线：基于60fps (16.67ms/帧) 设计任务间隔
 */
public final class LoadConfig {

    // ==================== 任务调度配置 ====================
    
    /** 最小任务执行间隔（1帧 = 16ms） */
    public static final int MIN_TASK_INTERVAL_MS = 16;
    
    /** 最大任务执行间隔（5帧 = 83ms） */
    public static final int MAX_TASK_INTERVAL_MS = 83;
    
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
    public static final int IN_FRAME_LIGHT_INTENSITY = 150;
    
    /** 帧内中负载强度 */
    public static final int IN_FRAME_MEDIUM_INTENSITY = 300;
    
    /** 帧内高负载强度 */
    public static final int IN_FRAME_HEAVY_INTENSITY = 500;
    
    // ==================== 超长帧负载配置 ====================
    
    /** 超长帧负载强度（HEAVY的20倍） */
    public static final int LONG_FRAME_INTENSITY = 10000;
    
    /** 超长帧在滑动周期内的最小触发次数 */
    public static final int LONG_FRAME_MIN_TRIGGERS = 2;
    
    /** 超长帧在滑动周期内的最大触发次数 */
    public static final int LONG_FRAME_MAX_TRIGGERS = 3;
    
    /** 超长帧滑动周期时长（毫秒） */
    public static final int LONG_FRAME_SCROLL_PERIOD_MS = 2000;
    
    /** 超长帧最小触发间隔（毫秒），避免连续触发 */
    public static final int LONG_FRAME_MIN_INTERVAL_MS = 300;
    
    // ==================== 混合负载 - doFrame配置 ====================
    
    /** 混合轻负载 - doFrame任务强度 */
    public static final int MIXED_DOFRAME_LIGHT_INTENSITY = 1000;
    
    /** 混合中负载 - doFrame任务强度 */
    public static final int MIXED_DOFRAME_MEDIUM_INTENSITY = 2000;
    
    /** 混合高负载 - doFrame任务强度 */
    public static final int MIXED_DOFRAME_HEAVY_INTENSITY = 4000;
    
    // ==================== 帧间负载配置 ====================
    
    /** 帧间轻负载强度 */
    public static final int BETWEEN_FRAME_LIGHT_INTENSITY = 200;
    
    /** 帧间中负载强度 */
    public static final int BETWEEN_FRAME_MEDIUM_INTENSITY = 400;
    
    /** 帧间高负载强度 */
    public static final int BETWEEN_FRAME_HEAVY_INTENSITY = 800;
    
    // ==================== 混合负载 - 帧间配置 ====================
    
    /** 混合轻负载 - 帧间任务强度 */
    public static final int MIXED_BETWEEN_FRAME_LIGHT_INTENSITY = 2400;
    
    /** 混合中负载 - 帧间任务强度 */
    public static final int MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY = 1600;
    
    /** 混合高负载 - 帧间任务强度 */
    public static final int MIXED_BETWEEN_FRAME_HEAVY_INTENSITY = 1067;
    
    // ==================== 帧间任务概率配置 ====================
    
    /** 轻负载任务执行概率 */
    public static final float LIGHT_TASK_PROBABILITY = 0.3f;
    
    /** 中负载任务执行概率 */
    public static final float MEDIUM_TASK_PROBABILITY = 0.5f;
    
    /** 高负载任务执行概率 */
    public static final float HEAVY_TASK_PROBABILITY = 0.7f;
    
    // ==================== Bitmap大小配置 ====================
    
    /** 轻负载位图大小 */
    public static final int LIGHT_BITMAP_SIZE = 200;
    
    /** 中负载位图大小 */
    public static final int MEDIUM_BITMAP_SIZE = 400;
    
    /** 高负载位图大小 */
    public static final int HEAVY_BITMAP_SIZE = 600;
    
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
     * @return 超长帧负载强度（HEAVY的10倍）
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
                return String.format("帧间中负载 - 强度:%d, 概率:%.0f%%", 
                        BETWEEN_FRAME_MEDIUM_INTENSITY, MEDIUM_TASK_PROBABILITY * 100);
            case LoadType.HEAVY_BETWEEN_FRAMES:
                return String.format("帧间高负载 - 强度:%d, 概率:%.0f%%", 
                        BETWEEN_FRAME_HEAVY_INTENSITY, HEAVY_TASK_PROBABILITY * 100);
            case LoadType.LIGHT_MIXED:
                return String.format("混合轻负载 - doFrame:%d, 帧间:%d", 
                        MIXED_DOFRAME_LIGHT_INTENSITY, MIXED_BETWEEN_FRAME_LIGHT_INTENSITY);
            case LoadType.MEDIUM_MIXED:
                return String.format("混合中负载 - doFrame:%d, 帧间:%d", 
                        MIXED_DOFRAME_MEDIUM_INTENSITY, MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY);
            case LoadType.HEAVY_MIXED:
                return String.format("混合高负载 - doFrame:%d, 帧间:%d", 
                        MIXED_DOFRAME_HEAVY_INTENSITY, MIXED_BETWEEN_FRAME_HEAVY_INTENSITY);
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
     * @return 验证结果
     */
    public static boolean validateConfig() {
        // 验证doFrame负载呈几何级数增长
        boolean doFrameValid = 
                (MIXED_DOFRAME_MEDIUM_INTENSITY == MIXED_DOFRAME_LIGHT_INTENSITY * 2) &&
                (MIXED_DOFRAME_HEAVY_INTENSITY == MIXED_DOFRAME_MEDIUM_INTENSITY * 2);
        
        // 验证帧间负载递增
        boolean betweenFrameValid = 
                (BETWEEN_FRAME_MEDIUM_INTENSITY > BETWEEN_FRAME_LIGHT_INTENSITY) &&
                (BETWEEN_FRAME_HEAVY_INTENSITY > BETWEEN_FRAME_MEDIUM_INTENSITY);
        
        // 验证混合负载总体趋势递增
        int totalLight = MIXED_DOFRAME_LIGHT_INTENSITY + MIXED_BETWEEN_FRAME_LIGHT_INTENSITY;
        int totalMedium = MIXED_DOFRAME_MEDIUM_INTENSITY + MIXED_BETWEEN_FRAME_MEDIUM_INTENSITY;
        int totalHeavy = MIXED_DOFRAME_HEAVY_INTENSITY + MIXED_BETWEEN_FRAME_HEAVY_INTENSITY;
        boolean totalValid = (totalMedium > totalLight) && (totalHeavy > totalMedium);
        
        return doFrameValid && betweenFrameValid && totalValid;
    }
}

