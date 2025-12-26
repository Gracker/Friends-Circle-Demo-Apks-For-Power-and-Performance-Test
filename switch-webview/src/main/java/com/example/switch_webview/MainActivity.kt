package com.example.switch_webview

import android.content.Intent
import android.os.Bundle
import android.os.Trace
import androidx.appcompat.app.AppCompatActivity
import com.example.switch_webview.target.*
import com.example.switch_common.SwitchLoadManager
import com.example.switch_common.SwitchLoadType
import com.google.android.material.button.MaterialButton

/**
 * 主界面 - 负载类型选择器 (WebView 版本)
 *
 * 使用 MaterialButton 显示 10 种负载类型，点击后启动对应的目标 Activity
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置按钮点击监听
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        SwitchLoadManager.stopAllLoads(this)
    }

    private fun setupButtons() {
        // Pure
        findViewById<MaterialButton>(R.id.btn_pure).setOnClickListener {
            launchTargetActivity(SwitchLoadType.PURE)
        }

        // Self loads
        findViewById<MaterialButton>(R.id.btn_self_light).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_LIGHT)
        }
        findViewById<MaterialButton>(R.id.btn_self_medium).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_MEDIUM)
        }
        findViewById<MaterialButton>(R.id.btn_self_heavy).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_HEAVY)
        }

        // Background loads
        findViewById<MaterialButton>(R.id.btn_bg_medium).setOnClickListener {
            launchTargetActivity(SwitchLoadType.BG_MEDIUM)
        }
        findViewById<MaterialButton>(R.id.btn_bg_heavy).setOnClickListener {
            launchTargetActivity(SwitchLoadType.BG_HEAVY)
        }

        // Combined loads
        findViewById<MaterialButton>(R.id.btn_light_bg_medium).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_LIGHT_BG_MEDIUM)
        }
        findViewById<MaterialButton>(R.id.btn_medium_bg_medium).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_MEDIUM_BG_MEDIUM)
        }
        findViewById<MaterialButton>(R.id.btn_medium_bg_heavy).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_MEDIUM_BG_HEAVY)
        }
        findViewById<MaterialButton>(R.id.btn_heavy_bg_heavy).setOnClickListener {
            launchTargetActivity(SwitchLoadType.SELF_HEAVY_BG_HEAVY)
        }
    }

    private fun launchTargetActivity(loadType: SwitchLoadType) {
        Trace.beginSection("Switch_WebView_Launch_${loadType.name}")

        // 先启动背景负载
        SwitchLoadManager.startBackgroundLoad(this, loadType)

        val targetClass = when (loadType) {
            SwitchLoadType.PURE -> PureActivity::class.java
            SwitchLoadType.SELF_LIGHT -> SelfLightActivity::class.java
            SwitchLoadType.SELF_MEDIUM -> SelfMediumActivity::class.java
            SwitchLoadType.SELF_HEAVY -> SelfHeavyActivity::class.java
            SwitchLoadType.BG_MEDIUM -> BgMediumActivity::class.java
            SwitchLoadType.BG_HEAVY -> BgHeavyActivity::class.java
            SwitchLoadType.SELF_LIGHT_BG_MEDIUM -> SelfLightBgMediumActivity::class.java
            SwitchLoadType.SELF_MEDIUM_BG_MEDIUM -> SelfMediumBgMediumActivity::class.java
            SwitchLoadType.SELF_MEDIUM_BG_HEAVY -> SelfMediumBgHeavyActivity::class.java
            SwitchLoadType.SELF_HEAVY_BG_HEAVY -> SelfHeavyBgHeavyActivity::class.java
        }

        val intent = Intent(this, targetClass).apply {
            putExtra(EXTRA_LOAD_TYPE, loadType.ordinal)
        }
        startActivity(intent)

        Trace.endSection()
    }

    companion object {
        const val EXTRA_LOAD_TYPE = "extra_load_type"
    }
}
