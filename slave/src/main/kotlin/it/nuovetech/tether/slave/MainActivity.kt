package it.nuovetech.tether.slave

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.statusText).setText(R.string.status_waiting)
    }

    override fun onStart() {
        super.onStart()
        connect()
    }

    override fun onStop() {
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
                Log.w( "event", text)
                val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (json.optString("type") != "EVENT") return
                val event = json.getJSONObject("event")
                setStatus("${event.optString("appLabel")}: ${event.optString("title")} - ${event.optString("text")}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                setStatus(getString(R.string.status_relay_unreachable))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
            }
        })
    }

    private fun setStatus(text: String) {
        ui.post { findViewById<TextView>(R.id.statusText).text = text }
    }
}
