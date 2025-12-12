package com.example.wechatfriendforcompose.config

/**
 * Compose版负载配置
 */
object LoadConfig {

    // 任务调度配置
    const val MIN_TASK_INTERVAL_MS = 16L   // 1帧间隔
    const val MAX_TASK_INTERVAL_MS = 83L   // 5帧间隔

    // 随机数种子
    const val TASK_INTERVAL_SEED = 12345L
    const val DOFRAME_INTERVAL_SEED = 11111L
    const val COMPUTATION_SEED = 67890L

    // 单一负载配置
    object LightLoad {
        const val TASK_INTENSITY = 150
    }

    object MediumLoad {
        const val TASK_INTENSITY = 300
    }

    object HeavyLoad {
        const val TASK_INTENSITY = 500
    }

    // 混合负载配置
    object LightMixedLoad {
        const val DOFRAME_TASK_INTENSITY = 1000
        const val BETWEEN_FRAME_TASK_INTENSITY = 2400
    }

    object MediumMixedLoad {
        const val DOFRAME_TASK_INTENSITY = 2000
        const val BETWEEN_FRAME_TASK_INTENSITY = 1600
    }

    object HeavyMixedLoad {
        const val DOFRAME_TASK_INTENSITY = 4000
        const val BETWEEN_FRAME_TASK_INTENSITY = 1067
    }

    // 帧间负载配置
    object LightLoadBetweenFrames {
        const val COMPUTATION_LOOP_COUNT = 200
        const val TASK_EXECUTION_PROBABILITY = 0.3f
    }

    object MediumLoadBetweenFrames {
        const val COMPUTATION_LOOP_COUNT = 400
        const val TASK_EXECUTION_PROBABILITY = 0.5f
    }

    object HeavyLoadBetweenFrames {
        const val COMPUTATION_LOOP_COUNT = 800
        const val TASK_EXECUTION_PROBABILITY = 0.7f
    }
}


