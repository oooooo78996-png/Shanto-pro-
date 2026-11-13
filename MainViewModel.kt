package com.shanto.trader

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UiState(
    val candles: List<Candle> = emptyList(), val symbol: String = "BTCUSDT", val tf: String = "5m",
    val connected: Boolean = false, val lastMsg: Long = 0L, val error: String? = null, val closedEvents: Int = 0
)

class MainViewModel : ViewModel() {
    val state = MutableLiveData(UiState())
    var symbol = "BTCUSDT"; var tf = "5m"
    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private val h = Handler(Looper.getMainLooper())
    private var ws: WebSocket? = null
    private val candles = ArrayList<Candle>()
    private var gen = 0
    private var connected = false; private var lastMsg = 0L; private var error: String? = null; private var closedEvents = 0

    private fun publish() {
        state.value = UiState(candles.toList(), symbol, tf, connected, lastMsg, error, closedEvents)
    }

    fun start() {
        gen++; val g = gen
        ws?.cancel(); ws = null
        connected = false; error = null; candles.clear(); publish()
        load(g)
    }

    private fun load(g: Int) {
        val url = "https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$tf&limit=200"
        client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                h.post { if (g == gen) { error = "API পাওয়া যাচ্ছে না: ${e.message}"; publish(); h.postDelayed({ if (g == gen) load(g) }, 5000) } }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.use { if (it.isSuccessful) it.body?.string() else null }
                val list = try {
                    if (body == null) null else {
                        val a = JSONArray(body); val now = System.currentTimeMillis()
                        (0 until a.length()).map {
                            val k = a.getJSONArray(it)
                            Candle(k.getLong(0), k.getString(1).toDouble(), k.getString(2).toDouble(), k.getString(3).toDouble(),
                                k.getString(4).toDouble(), k.getString(5).toDouble(), k.getLong(6) < now)
                        }
                    }
                } catch (e: Exception) { null }
                h.post {
                    if (g != gen) return@post
                    if (list == null) {
                        error = "API থেকে সঠিক ডেটা আসেনি"; publish()
                        h.postDelayed({ if (g == gen) load(g) }, 5000)
                    } else {
                        error = null; candles.clear(); candles.addAll(list); publish(); connectWs(g)
                    }
                }
            }
        })
    }

    private fun connectWs(g: Int) {
        val url = "wss://stream.binance.com:9443/ws/${symbol.lowercase()}@kline_$tf"
        ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                h.post { if (g == gen) { connected = true; publish() } }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val k = try { JSONObject(text).getJSONObject("k") } catch (e: Exception) { return }
                val cd = try {
                    Candle(k.getLong("t"), k.getString("o").toDouble(), k.getString("h").toDouble(), k.getString("l").toDouble(),
                        k.getString("c").toDouble(), k.getString("v").toDouble(), k.getBoolean("x"))
                } catch (e: Exception) { return }
                h.post {
                    if (g != gen) return@post
                    lastMsg = System.currentTimeMillis(); connected = true
                    if (candles.isNotEmpty() && candles.last().t == cd.t) candles[candles.size - 1] = cd
                    else if (candles.isEmpty() || cd.t > candles.last().t) { candles.add(cd); if (candles.size > 300) candles.removeAt(0) }
                    if (cd.closed) closedEvents++
                    publish()
                }
            }
            private fun lost() {
                h.post {
                    if (g == gen) {
                        connected = false; publish()
                        h.postDelayed({ if (g == gen) load(g) }, 3000)
                    }
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = lost()
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = lost()
        })
    }

    override fun onCleared() { gen++; ws?.cancel() }
}
