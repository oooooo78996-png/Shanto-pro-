package com.shanto.trader

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val BG = 0xFF0A1428.toInt(); private val CARD = 0xFF111E3A.toInt()
    private val GREEN = 0xFF22C55E.toInt(); private val RED = 0xFFEF4444.toInt(); private val YELLOW = 0xFFEAB308.toInt()
    private val tfs = listOf("1m", "5m", "15m", "30m", "1h", "4h")
    private val tfMs = mapOf("1m" to 60000L, "5m" to 300000L, "15m" to 900000L, "30m" to 1800000L, "1h" to 3600000L, "4h" to 14400000L)
    private val pairs = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT")

    private lateinit var vm: MainViewModel
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var voiceOn = true
    private var lastClosed = 0
    private var st = UiState()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var priceT: TextView; private lateinit var statusT: TextView; private lateinit var countT: TextView
    private lateinit var chart: ChartView; private lateinit var sigT: TextView; private lateinit var explainT: TextView
    private lateinit var agreeT: TextView; private lateinit var detailT: TextView; private lateinit var sigCard: LinearLayout
    private val tfBtns = ArrayList<Button>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun box(color: Int, r: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
    private fun tv(size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        vm = ViewModelProvider(this)[MainViewModel::class.java]
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)) }
        val sv = ScrollView(this).apply { setBackgroundColor(BG); addView(root) }
        setContentView(sv)

        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv(20f, GREEN, true).apply { text = "ST"; gravity = Gravity.CENTER; background = box(CARD, 12); setPadding(dp(12), dp(8), dp(12), dp(8)) })
        head.addView(tv(20f, Color.WHITE, true).apply { text = "  SHANTO TRADER" })
        root.addView(head)

        statusT = tv(12f, 0xFF94A3B8.toInt()).apply { setPadding(0, dp(8), 0, dp(4)) }; root.addView(statusT)

        val sp = Spinner(this); sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pairs)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (vm.symbol != pairs[pos] || st.candles.isEmpty()) { vm.symbol = pairs[pos]; vm.start() }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(sp)

        val row = LinearLayout(this)
        tfs.forEach { t ->
            val bt = Button(this).apply {
                text = t; isAllCaps = false; textSize = 12f; minWidth = 0; minimumWidth = 0
                setPadding(0, 0, 0, 0)
                setOnClickListener { vm.tf = t; vm.start(); styleTf() }
            }
            tfBtns.add(bt); row.addView(bt, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), dp(6), dp(2), dp(6)) })
        }
        root.addView(row); styleTf()

        priceT = tv(30f, Color.WHITE, true); root.addView(priceT)
        countT = tv(13f, YELLOW); root.addView(countT)
        chart = ChartView(this); chart.background = box(CARD)
        root.addView(chart, LinearLayout.LayoutParams(-1, dp(220)).apply { setMargins(0, dp(8), 0, dp(4)) })
        root.addView(tv(11f, 0xFF94A3B8.toInt()).apply { text = "ভরাট = সম্পূর্ণ ক্যান্ডেল, হলুদ বর্ডার = এখনো তৈরি হচ্ছে (বিশ্লেষণে ধরা হয় না)" })

        sigCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = box(CARD) }
        sigT = tv(34f, YELLOW, true); sigCard.addView(sigT)
        explainT = tv(14f); sigCard.addView(explainT)
        agreeT = tv(12f, 0xFF94A3B8.toInt()).apply { setPadding(0, dp(6), 0, dp(6)) }; sigCard.addView(agreeT)
        detailT = tv(13f, 0xFFCBD5E1.toInt()); sigCard.addView(detailT)
        root.addView(sigCard, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(10), 0, dp(10)) })

        root.addView(SwitchCompat(this).apply {
            text = "বাংলা ভয়েস"; setTextColor(Color.WHITE); isChecked = true
            setOnCheckedChangeListener { _, on -> voiceOn = on; if (on) speak("ভয়েস চালু।") }
        })
        root.addView(Button(this).apply {
            text = "ম্যানুয়াল রিফ্রেশ"; setOnClickListener { speak("মার্কেট বিশ্লেষণ চলছে।"); vm.start() }
        })
        root.addView(tv(11f, 0xFF94A3B8.toInt()).apply {
            setPadding(0, dp(10), 0, 0)
            text = "সতর্কতা: এটি শুধুমাত্র টেকনিক্যাল বিশ্লেষণের সহায়ক টুল। ফলাফল নিশ্চিত নয়, কোনো সিগন্যাল ১০০% সঠিক নয়। কোনো ট্রেড এক্সিকিউশন নেই। ঝুঁকি আপনার নিজের।\n" +
                "স্কোর/সম্মতি জেতার সম্ভাবনা নয়: ৫টি ইন্ডিকেটরের মধ্যে কয়টি একই দিকে আছে তা গোনা হয়।"
        })

        vm.state.observe(this) { st = it; chart.candles = it.candles; render() }
        tick()
    }

    private fun styleTf() {
        tfBtns.forEachIndexed { i, b ->
            val sel = tfs[i] == vm.tf
            b.background = box(if (sel) GREEN else CARD, 10); b.setTextColor(if (sel) Color.BLACK else Color.WHITE)
        }
    }

    private fun online(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val c = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun tick() { render(); handler.postDelayed({ tick() }, 1000) }

    private fun render() {
        val now = System.currentTimeMillis()
        val age = (now - st.lastMsg) / 1000
        val fresh = st.connected && st.lastMsg > 0 && now - st.lastMsg < 10000
        val closed = st.candles.filter { it.closed }
        val r = Analyzer.analyze(closed, fresh && closed.size >= 60)
        if (st.closedEvents != lastClosed) { lastClosed = st.closedEvents; speak("নতুন ক্যান্ডেল তৈরি হয়েছে। " + r.spoken) }

        val conn = if (st.connected) "সংযুক্ত" else "সংযোগ বিচ্ছিন্ন (আবার চেষ্টা চলছে)"
        val fr = if (st.lastMsg == 0L) "ডেটা: অপেক্ষা" else if (fresh) "ডেটা: লাইভ (${age}s আগে)" else "ডেটা: পুরনো (${age}s)"
        statusT.text = "${st.symbol} • ${st.tf} • $conn • $fr • ইন্টারনেট: ${if (online()) "আছে" else "নেই"}" + (st.error?.let { "\n⚠ $it" } ?: "")
        statusT.setTextColor(if (fresh) GREEN else YELLOW)

        val last = st.candles.lastOrNull()
        if (last != null) {
            priceT.text = Analyzer.fmt(last.c)
            priceT.setTextColor(if (last.c >= last.o) GREEN else RED)
            val left = ((last.t + (tfMs[st.tf] ?: 60000L) - now) / 1000).coerceAtLeast(0)
            countT.text = "পরের ক্যান্ডেল: %02d:%02d  |  O ${Analyzer.fmt(last.o)}  H ${Analyzer.fmt(last.h)}  L ${Analyzer.fmt(last.l)}  Vol ${String.format(Locale.US, "%.2f", last.v)}".format(left / 60, left % 60)
        } else { priceT.text = "—"; countT.text = "ডেটা লোড হচ্ছে..." }

        sigT.text = r.title
        sigT.setTextColor(when (r.bias) { Bias.UP -> GREEN; Bias.DOWN -> RED; else -> YELLOW })
        explainT.text = r.explain; agreeT.text = r.agree; detailT.text = r.details
    }

    private fun speak(t: String) { if (voiceOn && ttsReady) tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "st") }

    override fun onInit(s: Int) {
        if (s == TextToSpeech.SUCCESS) {
            val r = tts?.setLanguage(Locale("bn", "BD"))
            ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (!ttsReady) Toast.makeText(this, "ফোনে বাংলা TTS ভয়েস ইনস্টল নেই (Google Text-to-speech এ বাংলা ডাউনলোড করুন)", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); tts?.shutdown(); super.onDestroy() }
}
