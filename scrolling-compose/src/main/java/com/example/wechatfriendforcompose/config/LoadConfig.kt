package com.example.wechatfriendforcompose.config

/**
 * Compose版负载配置
 */
object LoadConfig {

    // 任务调度配置
    const val MIN_TASK_INTERVAL_MS = 8L    // 1帧间隔 @120fps
    const val MAX_TASK_INTERVAL_MS = 42L   // 5帧间隔 @120fps

    // 随机数种子
    const val TASK_INTERVAL_SEED = 12345L
    const val SCHEDULE_INTERVAL_SEED = 11111L
    const val COMPUTATION_SEED = 67890L
    const val LOAD_TYPE_SEED_STRIDE = 9973L

    // 列表 item 渲染负载（onBind/重组路径）
    object ItemLoad {
        const val MINIMAL_TASK_INTENSITY = 0
        const val LIGHT_TASK_INTENSITY = 120
        const val MEDIUM_TASK_INTENSITY = 2600
        const val HEAVY_TASK_INTENSITY = 8800
    }

    // 单一负载配置
    object LightLoad {
        const val TASK_INTENSITY = 300
    }

    object MediumLoad {
        const val TASK_INTENSITY = 1400
    }

    object HeavyLoad {
        const val TASK_INTENSITY = 4600
    }

    // 混合负载配置
    object LightMixedLoad {
        const val DOFRAME_TASK_INTENSITY = 1200
        const val BETWEEN_FRAME_TASK_INTENSITY = 2200
    }

    object MediumMixedLoad {
        const val DOFRAME_TASK_INTENSITY = 3600
        const val BETWEEN_FRAME_TASK_INTENSITY = 7000
    }

    object HeavyMixedLoad {
        const val DOFRAME_TASK_INTENSITY = 8000
        const val BETWEEN_FRAME_TASK_INTENSITY = 16000
    }

    // 帧间负载配置
    object LightLoadBetweenFrames {
        const val COMPUTATION_LOOP_COUNT = 400
        const val TASK_EXECUTION_PROBABILITY = 0.3f
    }

    object MediumLoadBetweenFrames {
        const val COMPUTATION_LOOP_COUNT = 1200
        const val TASK_EXECUTION_PROBABILITY = 0.5f
    }

    object HeavyLoadBetweenFrames {
        const val COMPUTATION_LOOP_COUNT = 3000
        const val TASK_EXECUTION_PROBABILITY = 0.7f
    }
}

