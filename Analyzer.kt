package com.shanto.trader

import java.util.Locale
import kotlin.math.abs

enum class Bias { UP, DOWN, NO_TRADE }

data class Result(
    val bias: Bias, val title: String, val explain: String,
    val details: String, val agree: String, val spoken: String
)

object Analyzer {
    fun fmt(v: Double): String = if (v < 1) String.format(Locale.US, "%.5f", v) else String.format(Locale.US, "%.2f", v)

    fun analyze(cs: List<Candle>, dataOk: Boolean): Result {
        val noSpoken = "এখন ট্রেড নয়।"
        if (cs.size < 60) return Result(Bias.NO_TRADE, "NO TRADE",
            "পর্যাপ্ত সম্পূর্ণ ক্যান্ডেল নেই (কমপক্ষে ৬০টি দরকার, আছে ${cs.size}টি)। তাই বিশ্লেষণ করা হচ্ছে না।",
            "ডেটা অসম্পূর্ণ", "-", noSpoken)
        val cl = cs.map { it.c }
        val price = cl.last()
        val e9 = Ind.ema(cl, 9).last(); val e21 = Ind.ema(cl, 21).last(); val e50 = Ind.ema(cl, 50).last()
        val rsi = Ind.rsi(cl)
        val (macd, sig) = Ind.macd(cl)
        val (mid, bu, bl) = Ind.boll(cl)
        val atr = Ind.atr(cs)
        val last = cs.last()
        val prevW = cs.takeLast(21).dropLast(1)
        val res = prevW.maxOf { it.h }; val sup = prevW.minOf { it.l }
        val volRatio = last.v / prevW.map { it.v }.average()
        val range = last.h - last.l
        val bodyPct = if (range > 0) abs(last.c - last.o) / range else 0.0

        val emaV = if (e9 > e21 && e21 > e50) 1 else if (e9 < e21 && e21 < e50) -1 else 0
        val macdV = if (macd > sig && macd > 0) 1 else if (macd < sig && macd < 0) -1 else 0
        val rsiV = if (rsi in 55.0..70.0) 1 else if (rsi in 30.0..45.0) -1 else 0
        val candleV = if (last.c > last.o && bodyPct > 0.5) 1 else if (last.c < last.o && bodyPct > 0.5) -1 else 0
        val bbV = if (price > mid) 1 else -1
        val votes = listOf(emaV, macdV, rsiV, candleV, bbV)
        val bull = votes.count { it > 0 }; val bear = votes.count { it < 0 }

        var bias = Bias.NO_TRADE
        if (bull >= 4 && bear == 0) bias = Bias.UP
        if (bear >= 4 && bull == 0) bias = Bias.DOWN

        val blocks = ArrayList<String>()
        if (!dataOk) blocks.add("ডেটা সংযোগ/ফ্রেশনেস ঠিক নেই")
        if (atr / price < 0.0002) blocks.add("মার্কেট খুব ধীর (ATR কম)")
        if (volRatio < 0.5) blocks.add("ভলিউম দুর্বল")
        if (bias == Bias.UP && (res - price) < 0.5 * atr) blocks.add("রেজিস্ট্যান্সের খুব কাছে")
        if (bias == Bias.UP && rsi >= 75) blocks.add("RSI অতিরিক্ত বেশি")
        if (bias == Bias.DOWN && (price - sup) < 0.5 * atr) blocks.add("সাপোর্টের খুব কাছে")
        if (bias == Bias.DOWN && rsi <= 25) blocks.add("RSI অতিরিক্ত কম")
        val raw = bias
        if (blocks.isNotEmpty()) bias = Bias.NO_TRADE

        val trend = when (emaV) { 1 -> "আপট্রেন্ড (EMA 9 > 21 > 50)"; -1 -> "ডাউনট্রেন্ড (EMA 9 < 21 < 50)"; else -> "স্পষ্ট ট্রেন্ড নেই" }
        val macdS = if (macd > sig) "MACD সিগন্যালের উপরে" else "MACD সিগন্যালের নিচে"
        val details = "ট্রেন্ড: $trend\nRSI(14): ${String.format(Locale.US, "%.1f", rsi)}\n" +
            "EMA9/21/50: ${fmt(e9)} / ${fmt(e21)} / ${fmt(e50)}\nMACD: $macdS\n" +
            "বলিংগার: ${fmt(bl)} – ${fmt(bu)}\nATR(14): ${fmt(atr)}\n" +
            "সাপোর্ট: ${fmt(sup)}  রেজিস্ট্যান্স: ${fmt(res)}\n" +
            "ভলিউম: গড়ের ${String.format(Locale.US, "%.1f", volRatio)} গুণ\n" +
            "ক্যান্ডেল বডি: ${(bodyPct * 100).toInt()}%\n" +
            "ডেটা মান: " + (if (dataOk) "ভালো (লাইভ, ${cs.size} ক্যান্ডেল)" else "খারাপ")
        val agree = "বুলিশ $bull/5, বেয়ারিশ $bear/5 (EMA, MACD, RSI, ক্যান্ডেল, BB) — এটি জেতার সম্ভাবনা নয়, শুধু ইন্ডিকেটরের মিল"
        return when (bias) {
            Bias.UP -> Result(bias, "UP", "সম্ভাব্য আপট্রেন্ড: বেশিরভাগ ইন্ডিকেটর উপরের দিকে একমত। এটি নিশ্চিত ফলাফল নয়।", details, agree, "সম্ভাব্য আপট্রেন্ড।")
            Bias.DOWN -> Result(bias, "DOWN", "সম্ভাব্য ডাউনট্রেন্ড: বেশিরভাগ ইন্ডিকেটর নিচের দিকে একমত। এটি নিশ্চিত ফলাফল নয়।", details, agree, "সম্ভাব্য ডাউনট্রেন্ড।")
            else -> Result(bias, "NO TRADE",
                if (raw != Bias.NO_TRADE) "ইন্ডিকেটর একমত হলেও ফিল্টার আটকেছে: " + blocks.joinToString(", ")
                else "ইন্ডিকেটরগুলো একমত নয় বা মার্কেট অস্পষ্ট। অপেক্ষা করাই ভালো." + if (blocks.isNotEmpty()) " (" + blocks.joinToString(", ") + ")" else "",
                details, agree, noSpoken)
        }
    }
}
