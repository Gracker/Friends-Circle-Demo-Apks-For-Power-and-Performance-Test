package com.example.switch_common.views

import android.content.Context
import android.graphics.*
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 数据绑定模拟 View
 *
 * 模拟复杂的数据绑定场景：
 * - 创建大量数据对象
 * - 数据转换和格式化
 * - 条件渲染逻辑
 */
class DataBindingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val SEED = 0x12345678L

        @Volatile
        var blackHole: Any? = null
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(SEED)

    // 模拟数据模型
    data class ItemData(
        val id: Int,
        val title: String,
        val subtitle: String,
        val value: Double,
        val enabled: Boolean,
        val children: List<String>
    )

    private var dataList: List<ItemData> = emptyList()
    private var processedData: Map<Int, String> = emptyMap()

    var viewIndex: Int = 0
        set(value) {
            field = value
            random.setSeed(SEED + value)
            bindData()
            invalidate()
        }

    var dataComplexity: Int = 10  // 数据复杂度

    private fun bindData() {
        Trace.beginSection("DataBindingView_bindData")

        // 1. 创建数据对象
        val items = ArrayList<ItemData>(dataComplexity)
        for (i in 0 until dataComplexity) {
            val children = (0 until (i % 5 + 1)).map { "Child_${i}_$it" }
            items.add(
                ItemData(
                    id = viewIndex * 1000 + i,
                    title = "Item ${viewIndex}_$i",
                    subtitle = "Description for item $i with some extra text",
                    value = sin(i * 0.1 + viewIndex) * 100,
                    enabled = i % 3 != 0,
                    children = children
                )
            )
        }
        dataList = items

        // 2. 数据转换
        val processed = HashMap<Int, String>()
        for (item in dataList) {
            val formatted = StringBuilder()
            formatted.append(item.title)
            formatted.append(" - ")
            formatted.append(String.format("%.2f", item.value))
            if (!item.enabled) {
                formatted.append(" [DISABLED]")
            }
            for (child in item.children) {
                formatted.append("\n  - ")
                formatted.append(child)
            }
            processed[item.id] = formatted.toString()
        }
        processedData = processed

        // 3. 数据排序和过滤
        val sorted = dataList.sortedByDescending { it.value }
        val filtered = sorted.filter { it.enabled }
        val grouped = filtered.groupBy { it.id % 3 }

        // 保持引用防止优化
        blackHole = Triple(sorted, filtered, grouped)

        Trace.endSection()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.beginSection("DataBindingView_onMeasure")

        // 基于数据计算尺寸
        val baseHeight = 60 + dataList.size * 20

        setMeasuredDimension(
            resolveSize(300, widthMeasureSpec),
            resolveSize(baseHeight, heightMeasureSpec)
        )
        Trace.endSection()
    }

    override fun onDraw(canvas: Canvas) {
        Trace.beginSection("DataBindingView_onDraw")
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 背景
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(0f, 0f, w, h, paint)

        // 绘制数据项
        var y = 20f
        paint.textSize = 12f * resources.displayMetrics.density

        for ((index, item) in dataList.take(10).withIndex()) {
            // 条件渲染
            if (item.enabled) {
                paint.color = Color.parseColor("#333333")
            } else {
                paint.color = Color.parseColor("#999999")
            }

            // 标题
            canvas.drawText(item.title, 16f, y, paint)

            // 值（右对齐）
            paint.textAlign = Paint.Align.RIGHT
            val valueText = String.format("%.1f", item.value)
            canvas.drawText(valueText, w - 16f, y, paint)
            paint.textAlign = Paint.Align.LEFT

            // 分隔线
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(16f, y + 8f, w - 16f, y + 8f, paint)

            y += 24f
        }

        // 统计信息
        paint.color = Color.parseColor("#666666")
        paint.textSize = 10f * resources.displayMetrics.density
        canvas.drawText("Total: ${dataList.size} | Enabled: ${dataList.count { it.enabled }}", 16f, h - 10f, paint)

        Trace.endSection()
    }
}
