package com.example.wechatfriendforcustomscroller;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 定义10种负载模式，与wechatfriendforperformance模块保持一致：
 * - 最轻负载 (Minimal)
 * - 帧内轻/中/重负载 (In-Frame Light/Medium/Heavy)
 * - 帧间轻/中/重负载 (Between-Frame Light/Medium/Heavy)
 * - 混合轻/中/重负载 (Mixed Light/Medium/Heavy)
 */
public final class LoadProfile {

    // 最轻负载
    public static final int LOAD_TYPE_MINIMAL = 0;
    
    // 帧内负载
    public static final int LOAD_TYPE_LIGHT = 1;
    public static final int LOAD_TYPE_MEDIUM = 2;
    public static final int LOAD_TYPE_HEAVY = 3;
    
    // 帧间负载
    public static final int LOAD_TYPE_LIGHT_BETWEEN_FRAMES = 4;
    public static final int LOAD_TYPE_MEDIUM_BETWEEN_FRAMES = 5;
    public static final int LOAD_TYPE_HEAVY_BETWEEN_FRAMES = 6;
    
    // 混合负载
    public static final int LOAD_TYPE_LIGHT_MIXED = 7;
    public static final int LOAD_TYPE_MEDIUM_MIXED = 8;
    public static final int LOAD_TYPE_HEAVY_MIXED = 9;

    @IntDef({
            LOAD_TYPE_MINIMAL,
            LOAD_TYPE_LIGHT, LOAD_TYPE_MEDIUM, LOAD_TYPE_HEAVY,
            LOAD_TYPE_LIGHT_BETWEEN_FRAMES, LOAD_TYPE_MEDIUM_BETWEEN_FRAMES, LOAD_TYPE_HEAVY_BETWEEN_FRAMES,
            LOAD_TYPE_LIGHT_MIXED, LOAD_TYPE_MEDIUM_MIXED, LOAD_TYPE_HEAVY_MIXED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LoadType {}

    private LoadProfile() {
    }

    public static String toLabel(@LoadType int loadType) {
        switch (loadType) {
            case LOAD_TYPE_MINIMAL:
                return "最轻负载";
            case LOAD_TYPE_LIGHT:
                return "帧内轻负载";
            case LOAD_TYPE_MEDIUM:
                return "帧内中负载";
            case LOAD_TYPE_HEAVY:
                return "帧内高负载";
            case LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
                return "帧间轻负载";
            case LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
                return "帧间中负载";
            case LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
                return "帧间高负载";
            case LOAD_TYPE_LIGHT_MIXED:
                return "混合轻负载";
            case LOAD_TYPE_MEDIUM_MIXED:
                return "混合中负载";
            case LOAD_TYPE_HEAVY_MIXED:
                return "混合高负载";
            default:
                return "未知负载";
        }
    }
    
    /**
     * 判断是否为帧间负载类型
     */
    public static boolean isBetweenFramesLoad(@LoadType int loadType) {
        return loadType == LOAD_TYPE_LIGHT_BETWEEN_FRAMES 
                || loadType == LOAD_TYPE_MEDIUM_BETWEEN_FRAMES 
                || loadType == LOAD_TYPE_HEAVY_BETWEEN_FRAMES;
    }
    
    /**
     * 判断是否为混合负载类型
     */
    public static boolean isMixedLoad(@LoadType int loadType) {
        return loadType == LOAD_TYPE_LIGHT_MIXED 
                || loadType == LOAD_TYPE_MEDIUM_MIXED 
                || loadType == LOAD_TYPE_HEAVY_MIXED;
    }
    
    /**
     * 判断是否为帧内负载类型（包括最轻负载）
     */
    public static boolean isInFrameLoad(@LoadType int loadType) {
        return loadType == LOAD_TYPE_MINIMAL
                || loadType == LOAD_TYPE_LIGHT 
                || loadType == LOAD_TYPE_MEDIUM 
                || loadType == LOAD_TYPE_HEAVY;
    }
}
