package com.example.loadconfig;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 统一的负载类型定义
 * 
 * 定义11种负载模式：
 * - 最轻负载 (Minimal) - 不添加任何额外负载
 * - 帧内轻/中/重负载 (In-Frame Light/Medium/Heavy) - 每帧内执行计算
 * - 帧间轻/中/重负载 (Between-Frame Light/Medium/Heavy) - 帧与帧之间执行任务
 * - 混合轻/中/重负载 (Mixed Light/Medium/Heavy) - 同时执行帧内和帧间负载
 * - 超长帧负载 (Long Frame) - 滑动过程中随机出现2-3次超长帧，强度是HEAVY的10倍
 */
public final class LoadType {

    // ==================== 负载类型常量 ====================

    /** 最轻负载 - 不添加任何额外负载 */
    public static final int MINIMAL = 0;

    /** 帧内轻负载 - 每帧内执行轻量计算 */
    public static final int LIGHT = 1;

    /** 帧内中负载 - 每帧内执行中等计算 */
    public static final int MEDIUM = 2;

    /** 帧内高负载 - 每帧内执行密集计算 */
    public static final int HEAVY = 3;

    /** 帧间轻负载 - 帧与帧之间执行轻量任务 */
    public static final int LIGHT_BETWEEN_FRAMES = 4;

    /** 帧间中负载 - 帧与帧之间执行中等任务 */
    public static final int MEDIUM_BETWEEN_FRAMES = 5;

    /** 帧间高负载 - 帧与帧之间执行密集任务 */
    public static final int HEAVY_BETWEEN_FRAMES = 6;

    /** 混合轻负载 - 同时执行帧内和帧间轻量负载 */
    public static final int LIGHT_MIXED = 7;

    /** 混合中负载 - 同时执行帧内和帧间中等负载 */
    public static final int MEDIUM_MIXED = 8;

    /** 混合高负载 - 同时执行帧内和帧间密集负载 */
    public static final int HEAVY_MIXED = 9;

    /** 超长帧负载 - 滑动过程中随机出现2-3次超长帧，强度是HEAVY的10倍 */
    public static final int LONG_FRAME = 10;

    // ==================== 类型注解 ====================

    @IntDef({
            MINIMAL,
            LIGHT, MEDIUM, HEAVY,
            LIGHT_BETWEEN_FRAMES, MEDIUM_BETWEEN_FRAMES, HEAVY_BETWEEN_FRAMES,
            LIGHT_MIXED, MEDIUM_MIXED, HEAVY_MIXED,
            LONG_FRAME
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    // ==================== 私有构造函数 ====================

    private LoadType() {
        throw new UnsupportedOperationException("LoadType is a utility class and cannot be instantiated");
    }

    // ==================== 工具方法 ====================

    /**
     * 获取负载类型的显示标签（中文）
     * @param loadType 负载类型
     * @return 中文标签
     */
    public static String toLabel(@Type int loadType) {
        switch (loadType) {
            case MINIMAL:
                return "最轻负载";
            case LIGHT:
                return "帧内轻负载";
            case MEDIUM:
                return "帧内中负载";
            case HEAVY:
                return "帧内高负载";
            case LIGHT_BETWEEN_FRAMES:
                return "帧间轻负载";
            case MEDIUM_BETWEEN_FRAMES:
                return "帧间中负载";
            case HEAVY_BETWEEN_FRAMES:
                return "帧间高负载";
            case LIGHT_MIXED:
                return "混合轻负载";
            case MEDIUM_MIXED:
                return "混合中负载";
            case HEAVY_MIXED:
                return "混合高负载";
            case LONG_FRAME:
                return "超长帧负载";
            default:
                return "未知负载";
        }
    }

    /**
     * 获取负载类型的显示标签（英文）
     * @param loadType 负载类型
     * @return 英文标签
     */
    public static String toLabelEn(@Type int loadType) {
        switch (loadType) {
            case MINIMAL:
                return "Minimal Load";
            case LIGHT:
                return "In-Frame Light Load";
            case MEDIUM:
                return "In-Frame Medium Load";
            case HEAVY:
                return "In-Frame Heavy Load";
            case LIGHT_BETWEEN_FRAMES:
                return "Between-Frame Light Load";
            case MEDIUM_BETWEEN_FRAMES:
                return "Between-Frame Medium Load";
            case HEAVY_BETWEEN_FRAMES:
                return "Between-Frame Heavy Load";
            case LIGHT_MIXED:
                return "Mixed Light Load";
            case MEDIUM_MIXED:
                return "Mixed Medium Load";
            case HEAVY_MIXED:
                return "Mixed Heavy Load";
            case LONG_FRAME:
                return "Long Frame Load";
            default:
                return "Unknown Load";
        }
    }

    /**
     * 判断是否为超长帧负载类型
     * @param loadType 负载类型
     * @return 是否为超长帧负载
     */
    public static boolean isLongFrameLoad(@Type int loadType) {
        return loadType == LONG_FRAME;
    }

    /**
     * 判断是否为帧间负载类型
     * @param loadType 负载类型
     * @return 是否为帧间负载
     */
    public static boolean isBetweenFramesLoad(@Type int loadType) {
        return loadType == LIGHT_BETWEEN_FRAMES 
                || loadType == MEDIUM_BETWEEN_FRAMES 
                || loadType == HEAVY_BETWEEN_FRAMES;
    }

    /**
     * 判断是否为混合负载类型
     * @param loadType 负载类型
     * @return 是否为混合负载
     */
    public static boolean isMixedLoad(@Type int loadType) {
        return loadType == LIGHT_MIXED 
                || loadType == MEDIUM_MIXED 
                || loadType == HEAVY_MIXED;
    }

    /**
     * 判断是否为帧内负载类型（包括最轻负载）
     * @param loadType 负载类型
     * @return 是否为帧内负载
     */
    public static boolean isInFrameLoad(@Type int loadType) {
        return loadType == MINIMAL
                || loadType == LIGHT 
                || loadType == MEDIUM 
                || loadType == HEAVY;
    }

    /**
     * 判断是否需要执行帧间任务（帧间负载或混合负载）
     * @param loadType 负载类型
     * @return 是否需要执行帧间任务
     */
    public static boolean needsBetweenFrameTask(@Type int loadType) {
        return isBetweenFramesLoad(loadType) || isMixedLoad(loadType);
    }

    /**
     * 判断是否需要执行doFrame任务（混合负载）
     * @param loadType 负载类型
     * @return 是否需要执行doFrame任务
     */
    public static boolean needsDoFrameTask(@Type int loadType) {
        return isMixedLoad(loadType);
    }

    /**
     * 获取负载级别（轻/中/重/超长帧）
     * @param loadType 负载类型
     * @return 0=无/最轻, 1=轻, 2=中, 3=重, 4=超长帧
     */
    public static int getLoadLevel(@Type int loadType) {
        switch (loadType) {
            case MINIMAL:
                return 0;
            case LIGHT:
            case LIGHT_BETWEEN_FRAMES:
            case LIGHT_MIXED:
                return 1;
            case MEDIUM:
            case MEDIUM_BETWEEN_FRAMES:
            case MEDIUM_MIXED:
                return 2;
            case HEAVY:
            case HEAVY_BETWEEN_FRAMES:
            case HEAVY_MIXED:
                return 3;
            case LONG_FRAME:
                return 4;
            default:
                return 0;
        }
    }
}

