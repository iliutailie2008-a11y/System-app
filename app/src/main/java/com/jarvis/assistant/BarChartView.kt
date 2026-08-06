package com.jarvis.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Grafic simplu de bare, stil HUD, folosit pentru a vizualiza date eCommerce
 * (ex: comparații de preț, volum de căutări, scor recenzii) trimise de model
 * prin tag-ul [VISUAL:chart|Etichetă:valoare,...].
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<Pair<String, Float>> = emptyList()

    private val barPaint = Paint().apply {
        color = Color.parseColor("#3DD6F5")
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#D7F4FF")
        textSize = 26f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint().apply {
        color = Color.parseColor("#6FE6FF")
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val axisPaint = Paint().apply {
        color = Color.parseColor("#1F4A56")
        strokeWidth = 2f
    }

    fun setData(pairs: List<Pair<String, Float>>) {
        data = pairs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxVal = data.maxOf { it.second }.coerceAtLeast(1f)
        val bottom = height - 50f
        val top = 40f
        val usableHeight = bottom - top
        val slot = width / data.size.toFloat()
        val barWidth = slot * 0.5f

        canvas.drawLine(10f, bottom, width - 10f, bottom, axisPaint)

        data.forEachIndexed { index, (label, value) ->
            val centerX = slot * index + slot / 2f
            val barHeight = (value / maxVal) * usableHeight
            canvas.drawRoundRect(
                centerX - barWidth / 2f, bottom - barHeight,
                centerX + barWidth / 2f, bottom,
                6f, 6f, barPaint
            )
            canvas.drawText(shorten(label), centerX, bottom + 34f, textPaint)
            canvas.drawText(formatValue(value), centerX, bottom - barHeight - 10f, valuePaint)
        }
    }

    private fun shorten(label: String): String =
        if (label.length > 10) label.take(9) + "…" else label

    private fun formatValue(value: Float): String =
        if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()
}
