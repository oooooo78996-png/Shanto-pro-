package com.shanto.trader

import kotlin.math.abs
import kotlin.math.max

data class Candle(val t: Long, val o: Double, val h: Double, val l: Double, val c: Double, val v: Double, val closed: Boolean)

object Ind {
    fun ema(x: List<Double>, p: Int): DoubleArray {
        val o = DoubleArray(x.size) { Double.NaN }
        if (x.size < p) return o
        var e = x.take(p).average(); o[p - 1] = e
        val k = 2.0 / (p + 1)
        for (i in p until x.size) { e = x[i] * k + e * (1 - k); o[i] = e }
        return o
    }
    fun rsi(x: List<Double>, p: Int = 14): Double {
        if (x.size <= p) return Double.NaN
        var g = 0.0; var l = 0.0
        for (i in 1..p) { val d = x[i] - x[i - 1]; if (d > 0) g += d else l -= d }
        g /= p; l /= p
        for (i in p + 1 until x.size) {
            val d = x[i] - x[i - 1]
            g = (g * (p - 1) + max(d, 0.0)) / p
            l = (l * (p - 1) + max(-d, 0.0)) / p
        }
        return if (l == 0.0) 100.0 else 100 - 100 / (1 + g / l)
    }
    /** returns macd, signal */
    fun macd(x: List<Double>): Pair<Double, Double> {
        val e12 = ema(x, 12); val e26 = ema(x, 26)
        val m = (25 until x.size).map { e12[it] - e26[it] }
        val s = ema(m, 9)
        return Pair(m.last(), s.last())
    }
    /** returns mid, upper, lower */
    fun boll(x: List<Double>, p: Int = 20): Triple<Double, Double, Double> {
        val w = x.takeLast(p); val m = w.average()
        val sd = Math.sqrt(w.sumOf { (it - m) * (it - m) } / p)
        return Triple(m, m + 2 * sd, m - 2 * sd)
    }
    fun atr(c: List<Candle>, p: Int = 14): Double {
        val tr = (1 until c.size).map {
            maxOf(c[it].h - c[it].l, abs(c[it].h - c[it - 1].c), abs(c[it].l - c[it - 1].c))
        }
        var a = tr.take(p).average()
        for (i in p until tr.size) a = (a * (p - 1) + tr[i]) / p
        return a
    }
}
