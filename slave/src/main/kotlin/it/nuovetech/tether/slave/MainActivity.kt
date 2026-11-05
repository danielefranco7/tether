package it.nuovetech.tether.slave

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Schermata della Slave. Versione di sviluppo: si iscrive alla stanza del relay e mostra
 * l'ultimo evento ricevuto. Nessuna cifratura né notifica ancora.
 */
class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private val ui = Handler(Looper.getMainLooper())
    private var started = false
    private val retry = Runnable { connect() }
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private val events by lazy { ReceivedEvents(prefs) }
    private val adapter by lazy { EventListAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.statusText).setText(R.string.status_waiting)
        val list = findViewById<ListView>(R.id.eventList)
        list.adapter = adapter
        adapter.onSwipeDelete = { item ->
            events.remove(item.id)
            refreshList()
        }
        refreshList()
    }

    override fun onStart() {
        super.onStart()
        started = true
        connect()
    }

    override fun onStop() {
        started = false
        ui.removeCallbacks(retry)
        socket?.close(1000, "slave in background")
        socket = null
        super.onStop()
    }

    private fun connect() {
        if (socket != null) return
        Log.w("relay url", BuildConfig.RELAY_URL)
        val request = Request.Builder().url(BuildConfig.RELAY_URL).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(
                    JSONObject().put("type", "JOIN").put("conversationKey", BuildConfig.CONVERSATION_KEY).toString()
                )
                setStatus(getString(R.string.status_connected))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.w("event", text)
                val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (json.optString("type") != "EVENT") return
                val event = json.getJSONObject("event")
                val eventId = event.optString("eventId")

                // ACK sempre, anche per i duplicati: la Master ritrasmette finché non lo riceve.
                webSocket.send(JSONObject().put("type", "ACK").put("ackId", eventId).toString())
                val received = ReceivedEvent(
                    id = eventId,
                    app = event.optString("app"),
                    appLabel = event.optString("appLabel"),
                    title = event.optString("title"),
                    text = event.optString("text"),
                    postedAt = event.optLong("postedAt"),
                )
                if (events.add(received)) refreshList()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                setStatus(getString(R.string.status_relay_unreachable))
                scheduleRetry()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                scheduleRetry()
            }
        })
    }

    private fun scheduleRetry() {
        if (started) ui.postDelayed(retry, RETRY_DELAY_MS)
    }

    private fun setStatus(text: String) {
        ui.post { findViewById<TextView>(R.id.statusText).text = text }
    }

    private fun refreshList() {
        ui.post {
            val list = events.snapshot()
            adapter.submit(list)
            val empty = list.isEmpty()
            findViewById<TextView>(R.id.emptyText).visibility = if (empty) View.VISIBLE else View.GONE
            findViewById<ListView>(R.id.eventList).visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    companion object {
        private const val RETRY_DELAY_MS = 5_000L
        private const val PREFS_NAME = "tether_slave"
    }
}
