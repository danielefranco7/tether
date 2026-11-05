package it.nuovetech.tether.master

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Client WebSocket verso il relay. Si iscrive alla stanza (JOIN), invia le voci della [NotificationOutbox]
 * e riceve gli ACK della Slave. Alla riconnessione ritrasmette tutte le voci non confermate.
 * Versione di sviluppo: in chiaro, senza autenticazione (vedi PROTOCOL.md, sezione 4 per la cifratura).
 */
class RelayClient(
    private val outbox: NotificationOutbox,
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
                        outbox.requeueUnacked()
                        flushLocked(webSocket)
                    }
                    onStateChanged?.invoke(State.CONNECTED)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                    if (json.optString("type") != "ACK") return
                    val ackId = json.optString("ackId")
                    if (outbox.setStatus(ackId, OutboxEntry.Status.ACKED)) Log.i(TAG, "ACK ricevuto: $ackId")
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
     * Accoda una notifica nella [NotificationOutbox] e la invia subito se il relay è connesso;
     * altrimenti apre la connessione. La voce resta in coda finché non arriva l'ACK.
     */
    fun send(entry: OutboxEntry) {
        outbox.add(entry)
        synchronized(lock) {
            socket?.takeIf { connected }?.let { flushLocked(it) }
        }
        if (socket == null) connect()
    }

    /** Invia le voci QUEUED in ordine finché il socket accetta. Chiamare con [lock] tenuto. */
    private fun flushLocked(ws: WebSocket) {
        for (entry in outbox.unacked()) {
            if (entry.status != OutboxEntry.Status.QUEUED) continue
            if (!ws.send(entry.payload)) break
            outbox.setStatus(entry.id, OutboxEntry.Status.SENT)
        }
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
        private const val RETRY_DELAY_MS = 5_000L
    }
}
