package com.example.switch_flutter

import android.content.Context
import android.graphics.*
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import com.example.switch_common.SelfLoadLevel
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Flutter 风格的自定义 View
 *
 * 模拟 Flutter 的 Canvas 渲染方式：
 * - 使用 Canvas 直接绘制所有 UI
 * - 复杂的图形操作：圆形、矩形、路径、渐变等
 * - Widget 树的模拟：通过嵌套绘制实现
 *
 * Flutter 的特点：
 * 1. 所有 UI 都通过 Canvas 绘制（类似于游戏引擎）
 * 2. Widget rebuild 会触发整个子树的重绘
 * 3. 大量的对象创建和回收
 */
class FlutterStyleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val SEED = 123456789L

        @Volatile
        var blackHole: Double = 0.0
    }

    var loadLevel: SelfLoadLevel = SelfLoadLevel.NONE
        set(value) {
            field = value
            invalidate()
        }

    var loadTypeName: String = ""

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        alpha = 200
    }

    private val random = Random(SEED)
    private var widgetCount = 0
    private var drawDuration = 0L

    override fun onDraw(canvas: Canvas) {
        val startTime = System.nanoTime()
        Trace.beginSection("FlutterStyleView_onDraw_${loadLevel.name}")

        super.onDraw(canvas)

        // 绘制背景渐变
        drawBackground(canvas)

        // 根据负载级别绘制不同数量的 "Widget" - 线性增长
        widgetCount = when (loadLevel) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 80     // LIGHT: ~100-200ms 目标
            SelfLoadLevel.MEDIUM -> 300   // MEDIUM: ~300-500ms 目标
            SelfLoadLevel.HEAVY -> 1000   // HEAVY: ~700-1200ms 目标
        }

        // 模拟 Flutter Widget 树的渲染
        simulateFlutterWidgetTree(canvas, widgetCount)

        // 绘制信息文本
        drawInfoText(canvas)

        drawDuration = (System.nanoTime() - startTime) / 1_000_000 // ms
        Trace.endSection()
    }

    private fun drawBackground(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.parseColor("#02569B"), // Flutter blue
                Color.parseColor("#13B9FD")  // Light blue
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    /**
     * 模拟 Flutter Widget 树的渲染
     *
     * Flutter 中每个 Widget 都需要：
     * 1. 创建 RenderObject
     * 2. 布局计算（constraints）
     * 3. 绘制（paint）
     */
    private fun simulateFlutterWidgetTree(canvas: Canvas, count: Int) {
        if (count == 0) return

        Trace.beginSection("FlutterWidget_Build")
        random.setSeed(SEED)

        // 模拟不同类型的 Widget
        val containerWidgets = count / 5
        val listItemWidgets = count / 2
        val decorativeWidgets = count - containerWidgets - listItemWidgets

        // 1. Container Widgets - 背景容器
        for (i in 0 until containerWidgets) {
            drawContainerWidget(canvas, i)
        }

        // 2. ListTile Widgets - 列表项
        for (i in 0 until listItemWidgets) {
            drawListTileWidget(canvas, i, listItemWidgets)
        }

        // 3. Decorative Widgets - 装饰性元素
        for (i in 0 until decorativeWidgets) {
            drawDecorativeWidget(canvas, i)
        }

        Trace.endSection()
    }

    /**
     * 模拟 Container Widget
     * 包含背景、边框、阴影等
     */
    private fun drawContainerWidget(canvas: Canvas, index: Int) {
        Trace.beginSection("Container_$index")

        val x = random.nextFloat() * width * 0.8f + width * 0.1f
        val y = random.nextFloat() * height * 0.6f + height * 0.15f
        val size = 40f + random.nextFloat() * 60f

        // 阴影
        paint.color = Color.argb(30, 0, 0, 0)
        canvas.drawRoundRect(x + 4, y + 4, x + size + 4, y + size + 4, 12f, 12f, paint)

        // 背景
        val hue = (index * 36) % 360
        paint.color = Color.HSVToColor(180, floatArrayOf(hue.toFloat(), 0.6f, 0.9f))
        canvas.drawRoundRect(x, y, x + size, y + size, 12f, 12f, paint)

        // 边框
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        paint.alpha = 100
        canvas.drawRoundRect(x, y, x + size, y + size, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        // 模拟复杂计算（Flutter 的 layout constraints）- 线性增长
        val iterations = when (loadLevel) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 200
            SelfLoadLevel.MEDIUM -> 2000
            SelfLoadLevel.HEAVY -> 20000
        }
        if (iterations > 0) {
            var result = 0.0
            for (j in 0 until iterations) {
                result += sin(j * 0.1 + index) * cos(j * 0.05)
                result += sqrt((j * index + 1).toDouble()) * 0.01
            }
            blackHole += result
        }

        Trace.endSection()
    }

    /**
     * 模拟 ListTile Widget
     * 包含头像、标题、副标题、trailing icon
     */
    private fun drawListTileWidget(canvas: Canvas, index: Int, total: Int) {
        Trace.beginSection("ListTile_$index")

        val itemHeight = 72f
        val y = height * 0.2f + (index.toFloat() / total) * (height * 0.6f)

        if (y > height * 0.8f) {
            Trace.endSection()
            return
        }

        // 背景
        paint.color = Color.argb(20, 255, 255, 255)
        canvas.drawRect(20f, y, width - 20f, y + itemHeight - 4, paint)

        // 头像圆形
        paint.color = Color.HSVToColor(floatArrayOf(
            (index * 30 % 360).toFloat(), 0.7f, 0.8f
        ))
        val avatarX = 60f
        val avatarY = y + itemHeight / 2
        canvas.drawCircle(avatarX, avatarY, 24f, paint)

        // 头像文字
        textPaint.textSize = 20f
        canvas.drawText(
            ('A' + (index % 26)).toString(),
            avatarX, avatarY + 7f, textPaint
        )
        textPaint.textSize = 48f

        // 标题和副标题
        subtitlePaint.textAlign = Paint.Align.LEFT
        subtitlePaint.textSize = 18f
        canvas.drawText("Item ${index + 1}", 100f, y + 28f, subtitlePaint)
        subtitlePaint.textSize = 14f
        subtitlePaint.alpha = 150
        canvas.drawText("Description for item ${index + 1}", 100f, y + 50f, subtitlePaint)
        subtitlePaint.alpha = 200
        subtitlePaint.textAlign = Paint.Align.CENTER

        // Trailing icon
        paint.color = Color.WHITE
        paint.alpha = 100
        canvas.drawCircle(width - 50f, avatarY, 16f, paint)
        paint.alpha = 255

        // 模拟 Widget rebuild 和数据处理 - 线性增长
        val iterations = when (loadLevel) {
            SelfLoadLevel.NONE -> 0
            SelfLoadLevel.LIGHT -> 100
            SelfLoadLevel.MEDIUM -> 1000
            SelfLoadLevel.HEAVY -> 10000
        }
        if (iterations > 0) {
            var result = 0.0
            for (j in 0 until iterations) {
                result += sqrt((j + index).toDouble()) * sin(j * 0.01)
                result += cos(j * 0.005 + index) * 100
            }
            blackHole += result
        }

        Trace.endSection()
    }

    /**
     * 模拟装饰性 Widget
     * 图标、分隔线、徽章等
     */
    private fun drawDecorativeWidget(canvas: Canvas, index: Int) {
        Trace.beginSection("Decorative_$index")

        val x = random.nextFloat() * width
        val y = random.nextFloat() * height

        when (index % 4) {
            0 -> {
                // 小图标
                paint.color = Color.WHITE
                paint.alpha = (50 + random.nextInt(100))
                canvas.drawCircle(x, y, 8f + random.nextFloat() * 8f, paint)
            }
            1 -> {
                // 小矩形
                paint.color = Color.WHITE
                paint.alpha = (30 + random.nextInt(70))
                val size = 4f + random.nextFloat() * 12f
                canvas.drawRoundRect(x, y, x + size * 2, y + size, 4f, 4f, paint)
            }
            2 -> {
                // 渐变点
                val gradient = RadialGradient(
                    x, y, 20f,
                    Color.argb(100, 255, 255, 255),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawCircle(x, y, 20f, paint)
                paint.shader = null
            }
            3 -> {
                // 小线条
                paint.color = Color.WHITE
                paint.alpha = 40
                paint.strokeWidth = 1f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(x, y, x + 30f, y + 10f, paint)
                paint.style = Paint.Style.FILL
            }
        }
        paint.alpha = 255

        Trace.endSection()
    }

    private fun drawInfoText(canvas: Canvas) {
        val centerX = width / 2f

        textPaint.textSize = 32f
        canvas.drawText(loadTypeName, centerX, 80f, textPaint)

        textPaint.textSize = 20f
        canvas.drawText("Flutter-Style Canvas Rendering", centerX, 120f, textPaint)

        subtitlePaint.textSize = 16f
        canvas.drawText("Widgets: $widgetCount", centerX, height - 80f, subtitlePaint)
        canvas.drawText("Load: ${loadLevel.name}", centerX, height - 55f, subtitlePaint)
    }
}
