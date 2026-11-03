package it.nuovetech.tether.master

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Intercetta le notifiche di sistema. Attivo solo quando la Master è abilitata (REQ-PAU-01).
 *
 * Stato attuale: scheletro. Tutte le notifiche sono scartate (livello 0), quindi nulla
 * lascia il dispositivo. La classificazione per livello e l'inoltro arrivano nei prossimi passi.
 */
class NotificationForwarderService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Livello 0 (DISCARDED) per default: nessun inoltro finché le regole non sono implementate.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Gli annullamenti saranno inoltrati come EVENT_CANCEL (PROTOCOL.md, sezione 5.1).
    }
}
