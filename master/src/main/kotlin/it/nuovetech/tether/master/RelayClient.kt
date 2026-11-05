package it.nuovetech.tether.master

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Client WebSocket verso il relay. Si iscrive alla stanza (JOIN) e invia i payload EVENT.
 * I payload non ancora trasmessi sono salvati in SharedPreferences e inviati alla prossima connessione,
 * anche dopo un riavvio dell'app.
 * Versione di sviluppo: in chiaro, senza autenticazione (vedi PROTOCOL.md, sezione 4 per la cifratura).
 */
class RelayClient(
    context: Context,
    private val url: String = BuildConfig.RELAY_URL,
    private val conversationKey: String = BuildConfig.CONVERSATION_KEY,
) {
    enum class State { DISCONNECTED, CONNECTED }

    /** Chiamato su ogni cambio di stato della connessione (thread di OkHttp). */
    var onStateChanged: ((State) -> Unit)? = null

    private val client = OkHttpClient()
    private val lock = Any()
    private val ui = Handler(Looper.getMainLooper())
    private val retry = Runnable { connect() }
    private var socket: WebSocket? = null
    private var connected = false
    private var wantConnected = false

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Payload in attesa di connessione, inviati in ordine dopo il JOIN. Persistiti in [prefs]. */
    private val pending = ArrayDeque<String>().apply {
        val saved = JSONArray(prefs.getString(KEY_PENDING, "[]"))
        for (i in 0 until saved.length()) add(saved.getString(i))
    }

    fun connect() {
        synchronized(lock) {
            wantConnected = true
            if (socket != null) return
            val request = Request.Builder().url(url).build()
            socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    Log.i(TAG, "connesso al relay")
                    synchronized(lock) {
                        connected = true
                        webSocket.send(JSONObject().put("type", "JOIN").put("conversationKey", conversationKey).toString())
                        flushLocked(webSocket)
                        persist()
                    }
                    onStateChanged?.invoke(State.CONNECTED)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    Log.w(TAG, "relay non raggiungibile: ${t.message}")
                    markDisconnected()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    markDisconnected()
                }
            })
        }
    }

    /**
     * Invia un payload già serializzato. Se il relay non è connesso il payload viene accodato
     * (fino a [MAX_PENDING]) e inviato alla prossima connessione. Restituisce true se inviato subito.
     */
    fun send(payload: String): Boolean {
        val sent = synchronized(lock) {
            pending.addLast(payload)
            if (pending.size > MAX_PENDING) pending.removeFirst()
            socket?.takeIf { connected }?.let { flushLocked(it) }
            persist()
            pending.isEmpty()
        }
        if (!sent) connect()
        return sent
    }

    /** Invia i payload in coda in ordine finché il socket accetta. Chiamare con [lock] tenuto. */
    private fun flushLocked(ws: WebSocket) {
        while (pending.isNotEmpty()) {
            if (!ws.send(pending.first())) break
            pending.removeFirst()
        }
    }

    private fun persist() {
        prefs.edit().putString(KEY_PENDING, JSONArray(pending.toList()).toString()).apply()
    }

    fun close() {
        synchronized(lock) {
            wantConnected = false
            ui.removeCallbacks(retry)
            socket?.close(1000, "master disabilitata")
            socket = null
            connected = false
        }
    }

    private fun markDisconnected() {
        synchronized(lock) {
            socket = null
            connected = false
            if (wantConnected) ui.postDelayed(retry, RETRY_DELAY_MS)
        }
        onStateChanged?.invoke(State.DISCONNECTED)
    }

    companion object {
        private const val TAG = "TetherRelay"
        private const val MAX_PENDING = 500
        private const val PREFS_NAME = "relay_queue"
        private const val KEY_PENDING = "pending"
        private const val RETRY_DELAY_MS = 5_000L
    }
}
