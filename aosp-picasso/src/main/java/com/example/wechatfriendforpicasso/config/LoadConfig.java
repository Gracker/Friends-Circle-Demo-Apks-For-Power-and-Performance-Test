package com.example.wechatfriendforpicasso.config;

/**
 * 性能测试负载配置中心
 * 统一管理所有Activity的负载参数，便于调试和优化
 * 
 * 设计原理：
 * 1. 基于Android 60fps渲染管线（每帧16.67ms）
 * 2. doFrame负载：几何级数增长（1000→2000→4000）
 * 3. 帧间负载：反比例衰减，平衡整体负载
 * 4. 任务间隔：1-5帧（16-83ms），确保测试可重现性
 */
public class LoadConfig {
    
    // ==================== 任务调度配置 ====================
    
    /**
     * 任务执行间隔配置
     * 基于60fps标准：每帧约16.67ms
     */
    public static final int MIN_TASK_INTERVAL_MS = 16;   // 1帧间隔
    public static final int MAX_TASK_INTERVAL_MS = 83;   // 5帧间隔
    
    /**
     * 随机数种子配置
     * 使用固定种子确保测试结果可重现
     */
    public static final long TASK_INTERVAL_SEED = 12345L;     // 任务间隔随机种子
    public static final long DOFRAME_INTERVAL_SEED = 11111L;  // doFrame间隔随机种子  
    public static final long COMPUTATION_SEED = 67890L;       // 计算逻辑随机种子
    
    // ==================== 单一负载配置 ====================
    
    /**
     * 轻负载配置
     */
    public static class LightLoad {
        public static final int TASK_INTENSITY = 150;
    }
    
    /**
     * 中负载配置  
     */
    public static class MediumLoad {
        public static final int TASK_INTENSITY = 300;
    }
    
    /**
     * 高负载配置
     */
    public static class HeavyLoad {
        public static final int TASK_INTENSITY = 500;
    }
    
    // ==================== 混合负载配置 ====================
    
    /**
     * 轻负载混合配置
     * doFrame强度较低，帧间强度较高，适合日常使用场景测试
     */
    public static class LightMixedLoad {
        public static final int DOFRAME_TASK_INTENSITY = 1000;        // doFrame任务强度
        public static final int BETWEEN_FRAME_TASK_INTENSITY = 2400;  // 帧间任务强度
    }
    
    /**
     * 中负载混合配置
     * doFrame强度中等，帧间强度平衡，适合中等压力测试
     */
    public static class MediumMixedLoad {
        public static final int DOFRAME_TASK_INTENSITY = 2000;        // doFrame任务强度  
        public static final int BETWEEN_FRAME_TASK_INTENSITY = 1600;  // 帧间任务强度
    }
    
    /**
     * 高负载混合配置
     * doFrame强度最高，帧间强度较低，适合极限性能测试
     */
    public static class HeavyMixedLoad {
        public static final int DOFRAME_TASK_INTENSITY = 4000;        // doFrame任务强度
        public static final int BETWEEN_FRAME_TASK_INTENSITY = 1067;  // 帧间任务强度
    }
    
    // ==================== 帧间负载配置 ====================
    
    /**
     * 轻负载帧间配置
     */
    public static class LightLoadBetweenFrames {
        public static final int COMPUTATION_LOOP_COUNT = 200;          // 计算循环次数
        public static final float TASK_EXECUTION_PROBABILITY = 0.3f;   // 任务执行概率30%
        public static final int BITMAP_SIZE = 200;                     // 位图大小
    }
    
    /**
     * 中负载帧间配置
     */
    public static class MediumLoadBetweenFrames {
        public static final int COMPUTATION_LOOP_COUNT = 400;          // 计算循环次数
        public static final float TASK_EXECUTION_PROBABILITY = 0.5f;   // 任务执行概率50%
        public static final int BITMAP_SIZE = 400;                     // 位图大小
    }
    
    /**
     * 高负载帧间配置
     */
    public static class HeavyLoadBetweenFrames {
        public static final int COMPUTATION_LOOP_COUNT = 800;          // 计算循环次数
        public static final float TASK_EXECUTION_PROBABILITY = 0.7f;   // 任务执行概率70%
        public static final int BITMAP_SIZE = 600;                     // 位图大小
    }
    
    // ==================== 负载说明和验证 ====================
    
    /**
     * 获取负载配置说明
     * @param activityType Activity类型
     * @return 配置说明
     */
    public static String getLoadConfigDescription(String activityType) {
        switch (activityType) {
            case "LightMixedLoad":
                return String.format("轻负载混合 - doFrame:%d, 帧间:%d", 
                    LightMixedLoad.DOFRAME_TASK_INTENSITY, 
                    LightMixedLoad.BETWEEN_FRAME_TASK_INTENSITY);
            case "MediumMixedLoad":
                return String.format("中负载混合 - doFrame:%d, 帧间:%d", 
                    MediumMixedLoad.DOFRAME_TASK_INTENSITY, 
                    MediumMixedLoad.BETWEEN_FRAME_TASK_INTENSITY);
            case "HeavyMixedLoad":
                return String.format("高负载混合 - doFrame:%d, 帧间:%d", 
                    HeavyMixedLoad.DOFRAME_TASK_INTENSITY, 
                    HeavyMixedLoad.BETWEEN_FRAME_TASK_INTENSITY);
            case "LightLoadBetweenFrames":
                return String.format("轻负载帧间 - 循环:%d, 概率:%.0f%%", 
                    LightLoadBetweenFrames.COMPUTATION_LOOP_COUNT, 
                    LightLoadBetweenFrames.TASK_EXECUTION_PROBABILITY * 100);
            case "MediumLoadBetweenFrames":
                return String.format("中负载帧间 - 循环:%d, 概率:%.0f%%", 
                    MediumLoadBetweenFrames.COMPUTATION_LOOP_COUNT, 
                    MediumLoadBetweenFrames.TASK_EXECUTION_PROBABILITY * 100);
            case "HeavyLoadBetweenFrames":
                return String.format("高负载帧间 - 循环:%d, 概率:%.0f%%", 
                    HeavyLoadBetweenFrames.COMPUTATION_LOOP_COUNT, 
                    HeavyLoadBetweenFrames.TASK_EXECUTION_PROBABILITY * 100);
            case "LightLoad":
                return String.format("轻负载 - 强度:%d", LightLoad.TASK_INTENSITY);
            case "MediumLoad":
                return String.format("中负载 - 强度:%d", MediumLoad.TASK_INTENSITY);
            case "HeavyLoad":
                return String.format("高负载 - 强度:%d", HeavyLoad.TASK_INTENSITY);
            default:
                return "未知负载配置";
        }
    }
    
    /**
     * 验证负载配置的科学性
     * @return 验证结果
     */
    public static boolean validateLoadConfig() {
        // 验证doFrame负载呈几何级数增长
        boolean doFrameValid = (MediumMixedLoad.DOFRAME_TASK_INTENSITY == LightMixedLoad.DOFRAME_TASK_INTENSITY * 2) &&
                              (HeavyMixedLoad.DOFRAME_TASK_INTENSITY == MediumMixedLoad.DOFRAME_TASK_INTENSITY * 2);
        
        // 验证帧间负载总体平衡
        int totalLight = LightMixedLoad.DOFRAME_TASK_INTENSITY + LightMixedLoad.BETWEEN_FRAME_TASK_INTENSITY;
        int totalMedium = MediumMixedLoad.DOFRAME_TASK_INTENSITY + MediumMixedLoad.BETWEEN_FRAME_TASK_INTENSITY;
        int totalHeavy = HeavyMixedLoad.DOFRAME_TASK_INTENSITY + HeavyMixedLoad.BETWEEN_FRAME_TASK_INTENSITY;
        
        boolean totalValid = (totalMedium > totalLight) && (totalHeavy > totalMedium);
        
        return doFrameValid && totalValid;
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private LoadConfig() {
        throw new UnsupportedOperationException("LoadConfig is a utility class and cannot be instantiated");
    }
}
