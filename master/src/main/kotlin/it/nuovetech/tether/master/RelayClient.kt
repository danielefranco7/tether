package it.nuovetech.tether.master

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Client WebSocket verso il relay. Si iscrive alla stanza (JOIN) e invia i payload EVENT.
 * Versione di sviluppo: in chiaro, senza autenticazione (vedi PROTOCOL.md, sezione 4 per la cifratura).
 */
class RelayClient(
    private val url: String = BuildConfig.RELAY_URL,
    private val conversationKey: String = BuildConfig.CONVERSATION_KEY,
) {
    enum class State { DISCONNECTED, CONNECTED }

    /** Chiamato su ogni cambio di stato della connessione (thread di OkHttp). */
    var onStateChanged: ((State) -> Unit)? = null

    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    fun connect() {
        if (socket != null) return
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i(TAG, "connesso al relay")
                webSocket.send(JSONObject().put("type", "JOIN").put("conversationKey", conversationKey).toString())
                onStateChanged?.invoke(State.CONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.w(TAG, "relay non raggiungibile: ${t.message}")
                socket = null
                onStateChanged?.invoke(State.DISCONNECTED)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                onStateChanged?.invoke(State.DISCONNECTED)
            }
        })
    }

    /** Invia un payload già serializzato. Restituisce false se il relay non è connesso. */
    fun send(payload: String): Boolean {
        val ws = socket ?: run {
            connect()
            return false
        }
        return ws.send(payload)
    }

    fun close() {
        socket?.close(1000, "master disabilitata")
        socket = null
    }

    companion object {
        private const val TAG = "TetherRelay"
    }
}
