package com.example.switch_common.views

import android.content.Context
import android.graphics.*
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * 复杂容器 View
 *
 * 模拟复杂的 ViewGroup，包含多层嵌套和复杂的布局计算
 */
class ComplexContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val SEED = 0xABCDEF12L

        @Volatile
        var blackHole: Double = 0.0
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(SEED)
    private val childPositions = ArrayList<Rect>()

    var containerIndex: Int = 0
        set(value) {
            field = value
            random.setSeed(SEED + value)
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.beginSection("ComplexContainer_onMeasure")

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var totalHeight = paddingTop + paddingBottom
        var maxWidth = 0

        // 测量所有子 View
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            // 复杂的子 View 测量约束计算
            val childWidthSpec = MeasureSpec.makeMeasureSpec(
                widthSize - paddingLeft - paddingRight,
                MeasureSpec.AT_MOST
            )
            val childHeightSpec = MeasureSpec.makeMeasureSpec(
                0,
                MeasureSpec.UNSPECIFIED
            )

            measureChild(child, childWidthSpec, childHeightSpec)

            totalHeight += child.measuredHeight + 8 // 8dp spacing
            maxWidth = maxOf(maxWidth, child.measuredWidth)

            // 一些计算来增加 CPU 负载
            blackHole += sin(i * 0.1 + containerIndex) * cos(i * 0.05)
        }

        val finalWidth = resolveSize(maxWidth + paddingLeft + paddingRight, widthMeasureSpec)
        val finalHeight = resolveSize(totalHeight, heightMeasureSpec)

        setMeasuredDimension(finalWidth, finalHeight)
        Trace.endSection()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Trace.beginSection("ComplexContainer_onLayout")

        childPositions.clear()
        var currentTop = paddingTop

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            // 计算水平位置（居中）
            val left = (width - childWidth) / 2
            val right = left + childWidth
            val top = currentTop
            val bottom = top + childHeight

            child.layout(left, top, right, bottom)
            childPositions.add(Rect(left, top, right, bottom))

            currentTop = bottom + 8

            // 额外计算
            blackHole += child.measuredWidth * 0.001
        }

        Trace.endSection()
    }

    override fun dispatchDraw(canvas: Canvas) {
        Trace.beginSection("ComplexContainer_dispatchDraw")

        // 绘制背景
        val hue = (containerIndex * 15f) % 360f
        paint.color = Color.HSVToColor(30, floatArrayOf(hue, 0.3f, 0.95f))
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 绘制分隔线
        paint.color = Color.argb(50, 0, 0, 0)
        paint.strokeWidth = 1f
        for (rect in childPositions) {
            canvas.drawLine(
                paddingLeft.toFloat(),
                rect.bottom + 4f,
                (width - paddingRight).toFloat(),
                rect.bottom + 4f,
                paint
            )
        }

        super.dispatchDraw(canvas)
        Trace.endSection()
    }
}
