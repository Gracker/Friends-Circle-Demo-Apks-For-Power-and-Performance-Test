package com.example.switch_common.views

import android.content.Context
import android.graphics.*
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * 中等负载自定义 View
 */
class MediumCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val SEED = 0xCAFEBABEL

        @Volatile
        var blackHole: Double = 0.0
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(SEED)
    private val rectF = RectF()

    var viewIndex: Int = 0
        set(value) {
            field = value
            random.setSeed(SEED + value)
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.beginSection("MediumView_onMeasure")

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        // 中等复杂度的测量
        var result = 0.0
        for (i in 0 until 10) {
            result += sin(i * 0.1 + viewIndex) * cos(i * 0.05)
        }
        blackHole += result

        setMeasuredDimension(
            resolveSize(200, widthMeasureSpec),
            resolveSize(60, heightMeasureSpec)
        )
        Trace.endSection()
    }

    override fun onDraw(canvas: Canvas) {
        Trace.beginSection("MediumView_onDraw")
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 背景
        val hue = (viewIndex * 12f) % 360f
        paint.color = Color.HSVToColor(180, floatArrayOf(hue, 0.5f, 0.9f))
        rectF.set(0f, 0f, w, h)
        canvas.drawRoundRect(rectF, 8f, 8f, paint)

        // 绘制几个圆形
        for (i in 0 until 5) {
            val cx = (i + 1) * w / 6
            val cy = h / 2
            paint.color = Color.HSVToColor(floatArrayOf((hue + i * 30) % 360, 0.7f, 0.8f))
            canvas.drawCircle(cx, cy, 12f, paint)
        }

        // 文本
        paint.color = Color.WHITE
        paint.textSize = 12f * resources.displayMetrics.density
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("#$viewIndex", w / 2, h / 2 + 4f, paint)

        Trace.endSection()
    }
}
