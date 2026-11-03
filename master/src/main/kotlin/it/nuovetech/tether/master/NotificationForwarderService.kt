package it.nuovetech.tether.master

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import it.nuovetech.tether.shared.NotificationLevel

/**
 * Intercetta le notifiche di sistema. Attivo solo quando la Master è abilitata (REQ-PAU-01).
 *
 * Classifica ogni notifica con il livello assegnato al suo package. L'inoltro verso la Slave
 * non è ancora implementato: nulla lascia il dispositivo.
 */
class NotificationForwarderService : NotificationListenerService() {

    private val rules by lazy { LevelRules(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val level = rules.levelFor(sbn.packageName)
        if (level == NotificationLevel.DISCARDED) return
        // TODO: cifratura e invio alla Slave tramite relay (PROTOCOL.md, sezioni 4 e 5.1).
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Gli annullamenti saranno inoltrati come EVENT_CANCEL (PROTOCOL.md, sezione 5.1).
    }
}
