package it.nuovetech.tether.master

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Notifica inoltrata alla Slave, con il suo stato di consegna.
 * [payload] è il JSON EVENT già serializzato, rispedito tal quale in caso di ritrasmissione.
 */
data class OutboxEntry(
    val id: String,
    val app: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val payload: String,
    val status: Status,
) {
    /** QUEUED: non ancora inviata sulla connessione corrente. SENT: inviata al relay. ACKED: confermata dalla Slave. */
    enum class Status { QUEUED, SENT, ACKED }
}

/**
 * Coda delle notifiche della Master, persistita in SharedPreferences (REQ-FWD-03).
 * Ogni voce resta in coda finché la Slave non invia l'ACK; alla riconnessione le voci non confermate
 * vengono ritrasmesse. La stessa lista alimenta la schermata della Master.
 */
class NotificationOutbox private constructor(private val prefs: android.content.SharedPreferences) {

    private val entries = ArrayDeque<OutboxEntry>().apply {
        val saved = JSONArray(prefs.getString(KEY_ENTRIES, "[]"))
        for (i in 0 until saved.length()) fromJson(saved.getJSONObject(i))?.let { add(it) }
    }

    /** Chiamato dopo ogni modifica (thread arbitrario). */
    @Volatile
    var onChanged: (() -> Unit)? = null

    /** Copia della lista, dalla più recente alla più vecchia. */
    @Synchronized
    fun snapshot(): List<OutboxEntry> = entries.reversed()

    @Synchronized
    fun add(entry: OutboxEntry) {
        entries.addLast(entry)
        trim()
        persist()
        notifyChanged()
    }

    /** Voci da inviare alla connessione corrente: non ancora confermate, in ordine di arrivo. */
    @Synchronized
    fun unacked(): List<OutboxEntry> = entries.filter { it.status != OutboxEntry.Status.ACKED }

    /** Imposta lo stato di una voce. Restituisce false se la voce non esiste più. */
    @Synchronized
    fun setStatus(id: String, status: OutboxEntry.Status): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return false
        entries[index] = entries[index].copy(status = status)
        persist()
        notifyChanged()
        return true
    }

    /** Elimina una voce dalla coda (cancellazione manuale): non verrà più ritrasmessa. */
    @Synchronized
    fun remove(id: String) {
        if (entries.removeAll { it.id == id }) {
            persist()
            notifyChanged()
        }
    }

    /** Riporta a QUEUED tutte le voci non confermate, prima di una nuova connessione. */
    @Synchronized
    fun requeueUnacked() {
        for (i in entries.indices) {
            if (entries[i].status == OutboxEntry.Status.SENT) {
                entries[i] = entries[i].copy(status = OutboxEntry.Status.QUEUED)
            }
        }
        persist()
        notifyChanged()
    }

    /** Elimina le voci più vecchie confermate, poi le più vecchie in assoluto, oltre [MAX_ENTRIES]. */
    private fun trim() {
        while (entries.size > MAX_ENTRIES) {
            val acked = entries.indexOfFirst { it.status == OutboxEntry.Status.ACKED }
            entries.removeAt(if (acked >= 0) acked else 0)
        }
    }

    private fun persist() {
        val array = JSONArray()
        entries.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun notifyChanged() {
        onChanged?.invoke()
    }

    private fun toJson(e: OutboxEntry): JSONObject = JSONObject()
        .put("id", e.id)
        .put("app", e.app)
        .put("appLabel", e.appLabel)
        .put("title", e.title)
        .put("text", e.text)
        .put("postedAt", e.postedAt)
        .put("payload", e.payload)
        .put("status", e.status.name)

    private fun fromJson(o: JSONObject): OutboxEntry? = runCatching {
        OutboxEntry(
            id = o.getString("id"),
            app = o.getString("app"),
            appLabel = o.getString("appLabel"),
            title = o.getString("title"),
            text = o.getString("text"),
            postedAt = o.getLong("postedAt"),
            payload = o.getString("payload"),
            status = OutboxEntry.Status.valueOf(o.getString("status")),
        )
    }.getOrNull()

    companion object {
        private const val PREFS_NAME = "relay_queue"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_ENTRIES = 500

        @Volatile
        private var instance: NotificationOutbox? = null

        /** Istanza unica condivisa tra il servizio di ascolto, il relay client e la schermata. */
        fun get(context: Context): NotificationOutbox = instance ?: synchronized(this) {
            instance ?: NotificationOutbox(
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ).also { instance = it }
        }
    }
}
