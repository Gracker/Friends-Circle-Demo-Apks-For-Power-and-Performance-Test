package com.example.switch_flutter.target

import com.example.switch_common.SwitchLoadType

/**
 * Flutter 风格版本的 10 种负载类型目标 Activity
 */

class PureActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.PURE
}

class SelfLightActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_LIGHT
}

class SelfMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_MEDIUM
}

class SelfHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_HEAVY
}

class BgMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.BG_MEDIUM
}

class BgHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.BG_HEAVY
}

class SelfLightBgMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_LIGHT_BG_MEDIUM
}

class SelfMediumBgMediumActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_MEDIUM_BG_MEDIUM
}

class SelfMediumBgHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_MEDIUM_BG_HEAVY
}

class SelfHeavyBgHeavyActivity : BaseTargetActivity() {
    override val loadType = SwitchLoadType.SELF_HEAVY_BG_HEAVY
}
