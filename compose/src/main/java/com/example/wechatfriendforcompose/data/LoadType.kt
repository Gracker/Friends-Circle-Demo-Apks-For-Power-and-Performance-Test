package com.example.wechatfriendforcompose.data

/**
 * 负载类型枚举
 */
enum class LoadType(val displayName: String) {
    MINIMAL("最轻负载"),
    LIGHT("轻负载"),
    MEDIUM("中负载"),
    HEAVY("高负载"),
    LIGHT_BETWEEN_FRAMES("轻负载 (帧间)"),
    MEDIUM_BETWEEN_FRAMES("中负载 (帧间)"),
    HEAVY_BETWEEN_FRAMES("高负载 (帧间)"),
    LIGHT_MIXED("轻负载 (混合)"),
    MEDIUM_MIXED("中负载 (混合)"),
    HEAVY_MIXED("高负载 (混合)");
    
    /**
     * 获取基础负载类型（用于数据生成）
     */
    fun getBaseLoadType(): BaseLoadType {
        return when (this) {
            MINIMAL -> BaseLoadType.MINIMAL
            LIGHT, LIGHT_BETWEEN_FRAMES, LIGHT_MIXED -> BaseLoadType.LIGHT
            MEDIUM, MEDIUM_BETWEEN_FRAMES, MEDIUM_MIXED -> BaseLoadType.MEDIUM
            HEAVY, HEAVY_BETWEEN_FRAMES, HEAVY_MIXED -> BaseLoadType.HEAVY
        }
    }
}

/**
 * 基础负载类型（用于确定评论和点赞数量）
 */
enum class BaseLoadType {
    MINIMAL,
    LIGHT,
    MEDIUM,
    HEAVY
}


