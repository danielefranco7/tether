package it.nuovetech.tether.shared

/** Livello di inoltro assegnato a una notifica dalla Master (SPECIFICATION.md, sezione 3.1). */
enum class NotificationLevel(val code: Int) {
    /** Scartata: non lascia mai la Master. */
    DISCARDED(0),

    /** Inoltrata alla Slave in silenzio, visibile quando lo schermo si accende. */
    SILENT(1),

    /** Inoltrata alla Slave con avviso immediato (vibrazione/suono). */
    ALERT(2);
}

/** Tipi di messaggio definiti in PROTOCOL.md, sezione 5. */
enum class MessageType {
    EVENT,
    EVENT_CANCEL,
    MISSED_CALL,
    HEARTBEAT,
    ACK,
    PAIR_REVOKE,
    PAUSED,
    RESUMED;
}

/** Evento inoltrato dalla Master alla Slave. Il contenuto è cifrato sul canale (PROTOCOL.md, sezione 4). */
data class ForwardedEvent(
    val type: MessageType,
    val level: NotificationLevel,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestampMillis: Long,
)
