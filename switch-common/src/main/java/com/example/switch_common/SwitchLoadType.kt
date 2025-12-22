package com.example.switch_common

/**
 * Switch Activity 负载类型定义
 *
 * 负载组合矩阵：
 * - 自身负载（目标 Activity 的复杂性）：none / light / medium / heavy
 * - 背景负载（系统负载模拟）：none / medium / heavy
 *
 * 10 种负载类型：
 * 1. PURE - 无任何负载，纯净跳转基准
 * 2. SELF_LIGHT - 仅自身轻负载
 * 3. SELF_MEDIUM - 仅自身中负载
 * 4. SELF_HEAVY - 仅自身重负载
 * 5. BG_MEDIUM - 仅背景中负载
 * 6. BG_HEAVY - 仅背景重负载
 * 7. SELF_LIGHT_BG_MEDIUM - 自身轻 + 背景中
 * 8. SELF_MEDIUM_BG_MEDIUM - 自身中 + 背景中
 * 9. SELF_MEDIUM_BG_HEAVY - 自身中 + 背景重
 * 10. SELF_HEAVY_BG_HEAVY - 自身重 + 背景重
 */
enum class SwitchLoadType(
    val displayName: String,
    val selfLoad: SelfLoadLevel,
    val bgLoad: BackgroundLoadLevel
) {
    PURE("Pure", SelfLoadLevel.NONE, BackgroundLoadLevel.NONE),
    SELF_LIGHT("Self-Light", SelfLoadLevel.LIGHT, BackgroundLoadLevel.NONE),
    SELF_MEDIUM("Self-Medium", SelfLoadLevel.MEDIUM, BackgroundLoadLevel.NONE),
    SELF_HEAVY("Self-Heavy", SelfLoadLevel.HEAVY, BackgroundLoadLevel.NONE),
    BG_MEDIUM("Bg-Medium", SelfLoadLevel.NONE, BackgroundLoadLevel.MEDIUM),
    BG_HEAVY("Bg-Heavy", SelfLoadLevel.NONE, BackgroundLoadLevel.HEAVY),
    SELF_LIGHT_BG_MEDIUM("Light+BgMed", SelfLoadLevel.LIGHT, BackgroundLoadLevel.MEDIUM),
    SELF_MEDIUM_BG_MEDIUM("Med+BgMed", SelfLoadLevel.MEDIUM, BackgroundLoadLevel.MEDIUM),
    SELF_MEDIUM_BG_HEAVY("Med+BgHeavy", SelfLoadLevel.MEDIUM, BackgroundLoadLevel.HEAVY),
    SELF_HEAVY_BG_HEAVY("Heavy+BgHeavy", SelfLoadLevel.HEAVY, BackgroundLoadLevel.HEAVY);

    companion object {
        fun fromOrdinal(ordinal: Int): SwitchLoadType {
            return entries.getOrElse(ordinal) { PURE }
        }
    }
}

/**
 * 自身负载级别
 * 模拟目标 Activity 的界面复杂度
 */
enum class SelfLoadLevel {
    /** 无负载 - 空白 Activity */
    NONE,
    /** 轻负载 - 简单布局，约 20 个 View，少量操作 */
    LIGHT,
    /** 中负载 - 中等复杂度，约 100 个 View，自定义 View，Binder 调用 */
    MEDIUM,
    /** 重负载 - 复杂布局，约 300+ View，大量自定义 View，Binder、IO、数据处理 */
    HEAVY
}

/**
 * 背景负载级别
 * 模拟系统繁忙程度
 */
enum class BackgroundLoadLevel {
    /** 无背景负载 */
    NONE,
    /** 中等背景负载 - 4 个后台线程 + 子进程 Service 4 个任务 */
    MEDIUM,
    /** 重背景负载 - 8 个后台线程 + 子进程 Service 6 个任务 */
    HEAVY
}
