package com.example.wechatfriendforrenderstress;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 定义轻/中/重三种负载模式，便于在各层统一引用。
 */
public final class LoadProfile {

    public static final int LOAD_TYPE_LIGHT = 0;
    public static final int LOAD_TYPE_MEDIUM = 1;
    public static final int LOAD_TYPE_HEAVY = 2;

    @IntDef({LOAD_TYPE_LIGHT, LOAD_TYPE_MEDIUM, LOAD_TYPE_HEAVY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LoadType {}

    private LoadProfile() {
    }

    public static String toLabel(@LoadType int loadType) {
        switch (loadType) {
            case LOAD_TYPE_LIGHT:
                return "轻负载";
            case LOAD_TYPE_MEDIUM:
                return "中负载";
            case LOAD_TYPE_HEAVY:
                return "重负载";
            default:
                return "未知负载";
        }
    }
}

