package it.nuovetech.tether.master

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import it.nuovetech.tether.shared.NotificationLevel

/**
 * Intercetta le notifiche di sistema. Attivo solo quando la Master è abilitata (REQ-PAU-01).
 *
 * Classifica ogni notifica con il livello assegnato al suo package. Le notifiche di livello 1 o 2
 * vengono accodate nella [NotificationOutbox] e inviate al relay, che le inoltra alla Slave.
 */
class NotificationForwarderService : NotificationListenerService() {

    private val rules by lazy { LevelRules(this) }
    private val outbox by lazy { NotificationOutbox.get(this) }
    private val relay by lazy {
        RelayClient(outbox).also {
            it.onStateChanged = { state ->
                relayState = state
                onRelayStateChanged?.invoke(state)
            }
        }
    }

    override fun onListenerConnected() {
        relay.connect()
    }

    override fun onListenerDisconnected() {
        relay.close()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        rules.rememberPackage(sbn.packageName)
        val level = rules.levelFor(sbn.packageName)
        if (level == NotificationLevel.DISCARDED) return

        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        val entry = EventPayload.build(sbn, appLabel, level)
        // Senza titolo né testo la notifica non contiene nulla da mostrare: la scartiamo.
        if (entry.title.isBlank() && entry.text.isBlank()) return
        relay.send(entry)
        // TODO: cifratura (PROTOCOL.md, sezione 4), rinviata su indicazione.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Gli annullamenti saranno inoltrati come EVENT_CANCEL (PROTOCOL.md, sezione 5.1).
    }

    companion object {
        /** Stato corrente della connessione al relay, letto dalla Main Activity. */
        @Volatile
        var relayState = RelayClient.State.DISCONNECTED

        /** Listener impostato dalla Main Activity mentre è visibile. */
        @Volatile
        var onRelayStateChanged: ((RelayClient.State) -> Unit)? = null
    }
}
