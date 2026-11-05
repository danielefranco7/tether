package it.nuovetech.tether.slave

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ReceivedEvent(
    val id: String,
    val app: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
)

/**
 * Eventi ricevuti, persistiti e ordinati dal più recente. Il dedup usa un insieme [seen] separato dalla
 * lista visibile: un evento cancellato dall'utente non ricompare se la Master lo ritrasmette (ACK perso).
 */
class ReceivedEvents(private val prefs: SharedPreferences) {

    private val items = ArrayList<ReceivedEvent>()
    private val seen = LinkedHashSet<String>()

    init {
        val saved = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrNull() ?: JSONArray()
        for (i in 0 until saved.length()) {
            val o = saved.getJSONObject(i)
            items.add(
                ReceivedEvent(
                    id = o.getString("id"),
                    app = o.optString("app"),
                    appLabel = o.optString("appLabel"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    postedAt = o.optLong("postedAt"),
                )
            )
        }
        val savedSeen = runCatching { JSONArray(prefs.getString(KEY_SEEN, "[]")) }.getOrNull() ?: JSONArray()
        for (i in 0 until savedSeen.length()) seen.add(savedSeen.getString(i))
        seen.addAll(items.map { it.id })
    }

    /** Restituisce false se l'evento era già stato ricevuto (anche se poi cancellato). */
    @Synchronized
    fun add(event: ReceivedEvent): Boolean {
        if (event.id.isEmpty() || !seen.add(event.id)) return false
        items.add(0, event)
        while (items.size > MAX) items.removeAt(items.lastIndex)
        while (seen.size > MAX_SEEN) seen.remove(seen.first())
        save()
        return true
    }

    /** Cancellazione manuale: rimuove solo dalla lista visibile, non dal dedup. */
    @Synchronized
    fun remove(id: String) {
        if (items.removeAll { it.id == id }) save()
    }

    @Synchronized
    fun snapshot(): List<ReceivedEvent> = items.toList()

    private fun save() {
        val array = JSONArray()
        for (e in items) {
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("app", e.app)
                    .put("appLabel", e.appLabel)
                    .put("title", e.title)
                    .put("text", e.text)
                    .put("postedAt", e.postedAt)
            )
        }
        val seenArray = JSONArray()
        for (id in seen) seenArray.put(id)
        prefs.edit()
            .putString(KEY, array.toString())
            .putString(KEY_SEEN, seenArray.toString())
            .apply()
    }

    companion object {
        const val KEY = "events"
        const val KEY_SEEN = "seen_ids"
        const val MAX = 500
        const val MAX_SEEN = 2000
    }
}
