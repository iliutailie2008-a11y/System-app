package com.jarvis.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Sferă wireframe rotativă, desenată direct pe Canvas (fără imagini/asset-uri externe).
 * Folosită ca fundal decorativ, stil HUD, în spatele conversației.
 */
class SphereView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val outlinePaint = Paint().apply {
        color = Color.parseColor("#6FE6FF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        alpha = 210
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#3DD6F5")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
        alpha = 110
    }

    private var rotation = 0f
    private val meridianCount = 9
    private val latitudeCount = 6

    private val ticker = object : Runnable {
        override fun run() {
            rotation = (rotation + 0.7f) % 360f
            invalidate()
            postDelayed(this, 16)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2.4f
        if (radius <= 0f) return

        canvas.drawCircle(cx, cy, radius, outlinePaint)

        for (i in 0 until meridianCount) {
            val phaseDeg = rotation + i * (180f / meridianCount)
            val phaseRad = Math.toRadians(phaseDeg.toDouble())
            val rx = radius * abs(cos(phaseRad)).toFloat()
            if (rx < 1f) continue
            canvas.drawOval(cx - rx, cy - radius, cx + rx, cy + radius, linePaint)
        }

        for (j in 1 until latitudeCount) {
            val frac = j / latitudeCount.toFloat()
            val t = (frac - 0.5f) * 2f
            val yOffset = radius * t * 0.85f
            val scale = sqrt((1f - t * t).coerceAtLeast(0f))
            val rx2 = radius * scale
            val ry2 = radius * scale * 0.16f
            canvas.drawOval(cx - rx2, cy + yOffset - ry2, cx + rx2, cy + yOffset + ry2, linePaint)
        }
    }
}
