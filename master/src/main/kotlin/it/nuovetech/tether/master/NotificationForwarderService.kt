package it.nuovetech.tether.master

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import it.nuovetech.tether.shared.NotificationLevel

/**
 * Intercetta le notifiche di sistema. Attivo solo quando la Master è abilitata (REQ-PAU-01).
 *
 * Classifica ogni notifica con il livello assegnato al suo package. Le notifiche di livello 1 o 2
 * vengono convertite nel payload EVENT e, per ora, mostrate nella debug area della Main Activity.
 * L'invio alla Slave non è ancora implementato: nulla lascia il dispositivo.
 */
class NotificationForwarderService : NotificationListenerService() {

    private val rules by lazy { LevelRules(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        rules.rememberPackage(sbn.packageName)
        val level = rules.levelFor(sbn.packageName)
        if (level == NotificationLevel.DISCARDED) return

        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        val payload = EventPayload.build(sbn, appLabel, level)

        remember(payload)
        sendBroadcast(
            Intent(ACTION_DEBUG_PAYLOAD)
                .setPackage(packageName)
                .putExtra(EXTRA_PAYLOAD, payload)
        )
        // TODO: cifratura e invio alla Slave tramite relay (PROTOCOL.md, sezioni 4 e 5.1).
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Gli annullamenti saranno inoltrati come EVENT_CANCEL (PROTOCOL.md, sezione 5.1).
    }

    companion object {
        const val ACTION_DEBUG_PAYLOAD = "it.nuovetech.tether.master.DEBUG_PAYLOAD"
        const val EXTRA_PAYLOAD = "payload"
        private const val BUFFER_SIZE = 50

        /** Ultimi payload inoltrati, in memoria, letti dalla Main Activity alla riapertura. */
        private val recent = ArrayDeque<String>()

        @Synchronized
        private fun remember(payload: String) {
            recent.addFirst(payload)
            while (recent.size > BUFFER_SIZE) recent.removeLast()
        }

        @Synchronized
        fun recentPayloads(): List<String> = recent.toList()
    }
}
