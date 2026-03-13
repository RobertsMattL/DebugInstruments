package com.debuginstruments.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class BinaryRainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF41")
        textSize = 28f
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#005518")
        textSize = 28f
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private val columns = mutableListOf<Column>()
    private var columnCount = 0
    private val charWidth get() = paint.measureText("0")
    private val charHeight get() = paint.textSize * 1.2f

    private var animator: ValueAnimator? = null

    private data class Column(
        val x: Float,
        var headY: Float,
        var speed: Float,
        val chars: MutableList<Char> = mutableListOf(),
        val startDelay: Int
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupColumns()
        startAnimation()
    }

    private fun setupColumns() {
        columns.clear()
        columnCount = (width / charWidth).toInt()
        for (i in 0 until columnCount) {
            columns.add(
                Column(
                    x = i * charWidth,
                    headY = -Random.nextFloat() * height,
                    speed = 2f + Random.nextFloat() * 6f,
                    startDelay = Random.nextInt(60)
                )
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateColumns()
                invalidate()
            }
            start()
        }
    }

    private var tick = 0

    private fun updateColumns() {
        tick++
        for (col in columns) {
            if (tick < col.startDelay) continue
            col.headY += col.speed
            if (col.headY > 0 && Random.nextFloat() < 0.3f) {
                col.chars.add(if (Random.nextBoolean()) '0' else '1')
            }
            if (col.headY > height + charHeight * 20) {
                col.headY = -Random.nextFloat() * height * 0.5f
                col.chars.clear()
                col.speed = 2f + Random.nextFloat() * 6f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        for (col in columns) {
            if (tick < col.startDelay) continue
            val visibleChars = (col.headY / charHeight).toInt().coerceAtLeast(0)
            val trailLength = 20

            for (i in 0 until visibleChars.coerceAtMost(col.chars.size)) {
                val y = col.headY - (visibleChars - 1 - i) * charHeight
                if (y < -charHeight || y > height + charHeight) continue

                val distFromHead = visibleChars - 1 - i
                val char = col.chars[i % col.chars.size]

                when {
                    distFromHead == 0 -> {
                        paint.color = Color.WHITE
                        paint.alpha = 255
                        canvas.drawText(char.toString(), col.x, y, paint)
                        paint.color = Color.parseColor("#00FF41")
                    }
                    distFromHead < 3 -> {
                        paint.alpha = 255
                        canvas.drawText(char.toString(), col.x, y, paint)
                    }
                    distFromHead < trailLength -> {
                        val fade = 1f - (distFromHead.toFloat() / trailLength)
                        dimPaint.alpha = (fade * 200).toInt().coerceIn(20, 200)
                        canvas.drawText(char.toString(), col.x, y, dimPaint)
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
