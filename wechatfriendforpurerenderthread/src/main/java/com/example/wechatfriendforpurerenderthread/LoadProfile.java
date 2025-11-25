package com.example.wechatfriendforpurerenderthread;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines 10 load profiles for pure RenderThread list testing.
 * This demo renders list items entirely on a separate rendering thread,
 * keeping UI Thread free from any rendering work.
 */
public final class LoadProfile {

    public static final int LOAD_TYPE_MINIMAL = 0;
    public static final int LOAD_TYPE_LIGHT = 1;
    public static final int LOAD_TYPE_MEDIUM = 2;
    public static final int LOAD_TYPE_HEAVY = 3;
    public static final int LOAD_TYPE_LIGHT_BETWEEN_FRAMES = 4;
    public static final int LOAD_TYPE_MEDIUM_BETWEEN_FRAMES = 5;
    public static final int LOAD_TYPE_HEAVY_BETWEEN_FRAMES = 6;
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

    private LoadProfile() {}

    public static String toLabel(@LoadType int loadType) {
        switch (loadType) {
            case LOAD_TYPE_MINIMAL: return "Minimal Load";
            case LOAD_TYPE_LIGHT: return "In-Frame Light Load";
            case LOAD_TYPE_MEDIUM: return "In-Frame Medium Load";
            case LOAD_TYPE_HEAVY: return "In-Frame Heavy Load";
            case LOAD_TYPE_LIGHT_BETWEEN_FRAMES: return "Between-Frame Light Load";
            case LOAD_TYPE_MEDIUM_BETWEEN_FRAMES: return "Between-Frame Medium Load";
            case LOAD_TYPE_HEAVY_BETWEEN_FRAMES: return "Between-Frame Heavy Load";
            case LOAD_TYPE_LIGHT_MIXED: return "Mixed Light Load";
            case LOAD_TYPE_MEDIUM_MIXED: return "Mixed Medium Load";
            case LOAD_TYPE_HEAVY_MIXED: return "Mixed Heavy Load";
            default: return "Unknown Load";
        }
    }
    
    public static boolean isBetweenFramesLoad(@LoadType int loadType) {
        return loadType == LOAD_TYPE_LIGHT_BETWEEN_FRAMES 
                || loadType == LOAD_TYPE_MEDIUM_BETWEEN_FRAMES 
                || loadType == LOAD_TYPE_HEAVY_BETWEEN_FRAMES;
    }
    
    public static boolean isMixedLoad(@LoadType int loadType) {
        return loadType == LOAD_TYPE_LIGHT_MIXED 
                || loadType == LOAD_TYPE_MEDIUM_MIXED 
                || loadType == LOAD_TYPE_HEAVY_MIXED;
    }
}


