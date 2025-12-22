package com.example.switch_flutter

import android.content.Intent
import android.os.Bundle
import android.os.Trace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.switch_flutter.target.*
import com.example.switch_common.BackgroundLoadLevel
import com.example.switch_common.SelfLoadLevel
import com.example.switch_common.SwitchLoadManager
import com.example.switch_common.SwitchLoadType

/**
 * 主界面 - 负载类型选择器 (Flutter 风格版本)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = LoadTypeAdapter(SwitchLoadType.entries) { loadType ->
            launchTargetActivity(loadType)
        }
    }

    override fun onResume() {
        super.onResume()
        SwitchLoadManager.stopAllLoads(this)
    }

    private fun launchTargetActivity(loadType: SwitchLoadType) {
        Trace.beginSection("Switch_Flutter_Launch_${loadType.name}")

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

class LoadTypeAdapter(
    private val loadTypes: List<SwitchLoadType>,
    private val onItemClick: (SwitchLoadType) -> Unit
) : RecyclerView.Adapter<LoadTypeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val loadIndicator: View = view.findViewById(R.id.loadIndicator)
        val tvLoadName: TextView = view.findViewById(R.id.tvLoadName)
        val tvSelfLoad: TextView = view.findViewById(R.id.tvSelfLoad)
        val tvBgLoad: TextView = view.findViewById(R.id.tvBgLoad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_load_type, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val loadType = loadTypes[position]
        val context = holder.itemView.context

        holder.tvLoadName.text = loadType.displayName
        holder.tvSelfLoad.text = "Self: ${loadType.selfLoad.name}"
        holder.tvBgLoad.text = "Background: ${loadType.bgLoad.name}"

        val indicatorColor = getIndicatorColor(loadType)
        holder.loadIndicator.setBackgroundColor(ContextCompat.getColor(context, indicatorColor))

        holder.itemView.setOnClickListener {
            onItemClick(loadType)
        }
    }

    override fun getItemCount() = loadTypes.size

    private fun getIndicatorColor(loadType: SwitchLoadType): Int {
        val selfLevel = when (loadType.selfLoad) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 1
            SelfLoadLevel.MEDIUM -> 2
            SelfLoadLevel.HEAVY -> 3
        }
        val bgLevel = when (loadType.bgLoad) {
            BackgroundLoadLevel.NONE -> 0
            BackgroundLoadLevel.MEDIUM -> 2
            BackgroundLoadLevel.HEAVY -> 3
        }
        val maxLevel = maxOf(selfLevel, bgLevel)

        return when (maxLevel) {
            0 -> R.color.load_none
            1 -> R.color.load_light
            2 -> R.color.load_medium
            else -> R.color.load_heavy
        }
    }
}
