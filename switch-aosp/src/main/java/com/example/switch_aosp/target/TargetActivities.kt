package com.example.switch_aosp.target

import com.example.switch_common.SwitchLoadType

/**
 * 10 种负载类型对应的目标 Activity
 *
 * 每个 Activity 只需指定其对应的 SwitchLoadType
 * 具体的负载执行由 BaseTargetActivity 统一处理
 */

/** 1. Pure - 无任何负载 */
class PureActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.PURE
}

/** 2. Self-Light - 仅自身轻负载 */
class SelfLightActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_LIGHT
}

/** 3. Self-Medium - 仅自身中负载 */
class SelfMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_MEDIUM
}

/** 4. Self-Heavy - 仅自身重负载 */
class SelfHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_HEAVY
}

/** 5. Bg-Medium - 仅背景中负载 */
class BgMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.BG_MEDIUM
}

/** 6. Bg-Heavy - 仅背景重负载 */
class BgHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.BG_HEAVY
}

/** 7. Self-Light + Bg-Medium - 自身轻 + 背景中 */
class SelfLightBgMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_LIGHT_BG_MEDIUM
}

/** 8. Self-Medium + Bg-Medium - 自身中 + 背景中 */
class SelfMediumBgMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_MEDIUM_BG_MEDIUM
}

/** 9. Self-Medium + Bg-Heavy - 自身中 + 背景重 */
class SelfMediumBgHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_MEDIUM_BG_HEAVY
}

/** 10. Self-Heavy + Bg-Heavy - 自身重 + 背景重 */
class SelfHeavyBgHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_HEAVY_BG_HEAVY
}
