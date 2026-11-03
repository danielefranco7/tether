package it.nuovetech.tether.master

import android.app.Notification
import android.service.notification.StatusBarNotification
import it.nuovetech.tether.shared.NotificationLevel
import org.json.JSONObject
import java.util.UUID

/**
 * Costruisce il payload JSON di tipo EVENT (PROTOCOL.md, sezione 5.1).
 * Per ora il JSON non è cifrato: la cifratura sarà applicata prima dell'invio.
 */
object EventPayload {

    fun build(sbn: StatusBarNotification, appLabel: String, level: NotificationLevel): String {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        val event = JSONObject()
            .put("eventId", UUID.randomUUID().toString())
            .put("app", sbn.packageName)
            .put("appLabel", appLabel)
            .put("sender", title)
            .put("title", title)
            .put("text", text)
            .put("category", sbn.notification.category.orEmpty())
            .put("postedAt", sbn.postTime)

        return JSONObject()
            .put("type", "EVENT")
            .put("level", level.code)
            .put("event", event)
            .toString(2)
    }
}
