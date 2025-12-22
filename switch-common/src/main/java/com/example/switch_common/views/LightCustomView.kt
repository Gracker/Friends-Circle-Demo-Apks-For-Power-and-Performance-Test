package com.example.switch_common.views

import android.content.Context
import android.graphics.*
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import java.util.Random

/**
 * 轻负载自定义 View
 */
class LightCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val SEED = 0xFEEDFACEL
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(SEED)

    var viewIndex: Int = 0
        set(value) {
            field = value
            random.setSeed(SEED + value)
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.beginSection("LightView_onMeasure")
        setMeasuredDimension(
            resolveSize(100, widthMeasureSpec),
            resolveSize(40, heightMeasureSpec)
        )
        Trace.endSection()
    }

    override fun onDraw(canvas: Canvas) {
        Trace.beginSection("LightView_onDraw")
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 简单背景
        val hue = (viewIndex * 20f) % 360f
        paint.color = Color.HSVToColor(floatArrayOf(hue, 0.4f, 0.95f))
        canvas.drawRoundRect(0f, 0f, w, h, 4f, 4f, paint)

        // 简单圆点
        paint.color = Color.WHITE
        canvas.drawCircle(w / 2, h / 2, 8f, paint)

        Trace.endSection()
    }
}
