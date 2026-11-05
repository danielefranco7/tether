package it.nuovetech.tether.master

import android.app.Notification
import android.service.notification.StatusBarNotification
import it.nuovetech.tether.shared.NotificationLevel
import org.json.JSONObject
import java.util.UUID

/**
 * Costruisce la voce di coda con il payload JSON di tipo EVENT (PROTOCOL.md, sezione 5.1).
 * `eventId` è l'ID usato per ACK e deduplicazione (PROTOCOL.md, sezione 6).
 * Per ora il JSON non è cifrato: la cifratura sarà applicata prima dell'invio.
 */
object EventPayload {

    fun build(sbn: StatusBarNotification, appLabel: String, level: NotificationLevel): OutboxEntry {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val eventId = UUID.randomUUID().toString()

        val event = JSONObject()
            .put("eventId", eventId)
            .put("app", sbn.packageName)
            .put("appLabel", appLabel)
            .put("sender", title)
            .put("title", title)
            .put("text", text)
            .put("category", sbn.notification.category.orEmpty())
            .put("postedAt", sbn.postTime)

        val payload = JSONObject()
            .put("type", "EVENT")
            .put("level", level.code)
            .put("event", event)
            .toString()

        return OutboxEntry(
            id = eventId,
            app = sbn.packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            postedAt = sbn.postTime,
            payload = payload,
            status = OutboxEntry.Status.QUEUED,
        )
    }
}
