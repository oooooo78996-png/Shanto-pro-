package com.shanto.trader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class ChartView(ctx: Context) : View(ctx) {
    var candles: List<Candle> = emptyList()
        set(v) { field = v; invalidate() }
    private val up = 0xFF22C55E.toInt(); private val dn = 0xFFEF4444.toInt()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val wick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f }

    override fun onDraw(c: Canvas) {
        val d = candles.takeLast(60)
        if (d.isEmpty()) return
        val hi = d.maxOf { it.h }; val lo = d.minOf { it.l }
        val rg = if (hi > lo) hi - lo else 1.0
        val w = width.toFloat(); val h = height.toFloat() - 10f
        val step = w / 60f
        fun y(p: Double) = (5f + (hi - p) / rg * h).toFloat()
        d.forEachIndexed { i, k ->
            val x = (60 - d.size + i) * step + step / 2
            val col = if (k.c >= k.o) up else dn
            wick.color = col; c.drawLine(x, y(k.h), x, y(k.l), wick)
            val top = y(maxOf(k.o, k.c)); val bot = maxOf(y(minOf(k.o, k.c)), top + 2f)
            if (k.closed) { fill.color = col; c.drawRect(x - step * 0.35f, top, x + step * 0.35f, bot, fill) }
            else { stroke.color = 0xFFEAB308.toInt(); c.drawRect(x - step * 0.35f, top, x + step * 0.35f, bot, stroke) }
        }
    }
}
