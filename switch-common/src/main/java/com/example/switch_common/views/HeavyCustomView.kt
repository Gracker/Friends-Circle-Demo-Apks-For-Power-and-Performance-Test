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
 * 重负载自定义 View
 *
 * 在 onMeasure、onLayout、onDraw 中执行真实的复杂操作
 * 使用固定种子确保每次测试结果一致
 */
class HeavyCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val SEED = 0xDEADBEEFL

        @Volatile
        var blackHole: Double = 0.0
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val matrix = Matrix()
    private val random = Random(SEED)
    private val rectF = RectF()

    // 预计算的数据
    private val computedData = DoubleArray(100)
    private val colorArray = IntArray(50)
    private val pointsArray = FloatArray(200)

    var viewIndex: Int = 0
        set(value) {
            field = value
            random.setSeed(SEED + value)
            invalidate()
        }

    init {
        // 初始化时进行一些计算
        initComputedData()
    }

    private fun initComputedData() {
        for (i in computedData.indices) {
            computedData[i] = sin(i * 0.1) * cos(i * 0.05) + sqrt(i + 1.0)
        }
        for (i in colorArray.indices) {
            val hue = (i * 7.2f) % 360f
            colorArray[i] = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.beginSection("HeavyView_onMeasure")

        // 真实的测量计算
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        // 复杂的尺寸计算
        var computedWidth = 0
        var computedHeight = 0

        // 模拟复杂的子元素测量
        for (i in 0 until 20) {
            val childWidth = (100 + computedData[i % computedData.size] * 10).toInt()
            val childHeight = (80 + computedData[(i + 10) % computedData.size] * 8).toInt()
            computedWidth = maxOf(computedWidth, childWidth)
            computedHeight += childHeight / 20
        }

        val finalWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(computedWidth + paddingLeft + paddingRight, widthSize)
            else -> computedWidth + paddingLeft + paddingRight
        }

        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(computedHeight + paddingTop + paddingBottom, heightSize)
            else -> computedHeight + paddingTop + paddingBottom
        }

        setMeasuredDimension(finalWidth, finalHeight)
        Trace.endSection()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Trace.beginSection("HeavyView_onLayout")
        super.onLayout(changed, left, top, right, bottom)

        // 真实的布局计算
        val w = right - left
        val h = bottom - top

        // 计算点位置
        for (i in pointsArray.indices step 2) {
            val angle = (i / 2) * (Math.PI * 2 / 100) + viewIndex * 0.1
            pointsArray[i] = (w / 2 + cos(angle) * w * 0.4).toFloat()
            pointsArray[i + 1] = (h / 2 + sin(angle) * h * 0.4).toFloat()
        }

        Trace.endSection()
    }

    override fun onDraw(canvas: Canvas) {
        Trace.beginSection("HeavyView_onDraw")
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. 绘制渐变背景
        val gradient = LinearGradient(
            0f, 0f, w, h,
            colorArray[viewIndex % colorArray.size],
            colorArray[(viewIndex + 25) % colorArray.size],
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // 2. 绘制多个圆形
        for (i in 0 until 10) {
            val cx = (i + 1) * w / 11
            val cy = h / 2 + sin(i + viewIndex * 0.1).toFloat() * h * 0.2f
            val radius = 10f + (computedData[i] * 5).toFloat()

            paint.color = colorArray[(viewIndex + i) % colorArray.size]
            paint.alpha = 180
            canvas.drawCircle(cx, cy, radius, paint)
        }

        // 3. 绘制复杂路径
        path.reset()
        path.moveTo(pointsArray[0], pointsArray[1])
        for (i in 2 until pointsArray.size step 2) {
            path.lineTo(pointsArray[i], pointsArray[i + 1])
        }
        path.close()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        paint.alpha = 100
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255

        // 4. 绘制文本
        paint.color = Color.WHITE
        paint.textSize = 14f * resources.displayMetrics.density
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("View #$viewIndex", w / 2, h / 2, paint)

        // 5. 矩阵变换绘制
        canvas.save()
        matrix.reset()
        matrix.postRotate(viewIndex * 5f, w / 2, h / 2)
        canvas.concat(matrix)

        rectF.set(w * 0.3f, h * 0.3f, w * 0.7f, h * 0.7f)
        paint.color = Color.WHITE
        paint.alpha = 30
        canvas.drawRect(rectF, paint)
        paint.alpha = 255

        canvas.restore()

        Trace.endSection()
    }
}
