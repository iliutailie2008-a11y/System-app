package com.jarvis.assistant

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class OrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private data class Blob(
        val colorHex: String,
        val radiusFactor: Float,
        val speed: Float,
        val phase: Float,
        val orbitFactor: Float
    )

    private val blobs = listOf(
        Blob("#4FACFE", 0.62f, 0.6f, 0f, 0.28f),
        Blob("#A18CD1", 0.58f, 0.5f, 2f, 0.30f),
        Blob("#FBC2EB", 0.50f, 0.7f, 4f, 0.24f),
        Blob("#38F9D7", 0.55f, 0.45f, 1f, 0.26f),
        Blob("#F6D365", 0.42f, 0.65f, 3f, 0.20f)
    )

    private var t = 0f
    private val basePaint = Paint().apply { isAntiAlias = true }

    private val ticker = object : Runnable {
        override fun run() {
            t += 0.02f
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
        val baseRadius = minOf(width, height) / 2.6f
        if (baseRadius <= 0f) return

        for (blob in blobs) {
            val angle = t * blob.speed + blob.phase
            val ox = cx + cos(angle) * baseRadius * blob.orbitFactor
            val oy = cy + sin(angle * 1.3f) * baseRadius * blob.orbitFactor
            val r = baseRadius * blob.radiusFactor

            val color = Color.parseColor(blob.colorHex)
            val paint = Paint(basePaint).apply {
                shader = RadialGradient(
                    ox, oy, r,
                    intArrayOf(applyAlpha(color, 170), applyAlpha(color, 0)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                maskFilter = BlurMaskFilter(r * 0.35f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(ox, oy, r, paint)
        }

        val outlinePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#33FFFFFF")
        }
        canvas.drawCircle(cx, cy, baseRadius, outlinePaint)
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
