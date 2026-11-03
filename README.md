# Tether

Inoltro di notifiche per la disconnessione consapevole. Lasci a casa il telefono principale (Master) e porti con te un secondo dispositivo Android (Slave), che riceve solo ciò che hai scelto, più la possibilità di chiamare in emergenza.

> Stato: specifica v0.3 e protocollo v1 in bozza. Scheletro Android creato (compila), senza inoltro né rete.

## Componenti

| Componente | Descrizione |
|---|---|
| **App Master** | Android, sul telefono di casa. Intercetta le notifiche, applica le regole a 3 livelli e invia eventi cifrati. Ha un toggle di abilitazione: spenta non svolge alcuna attività in background. |
| **App Slave** | Android, sul telefono portato con sé. Riceve, mostra e segnala gli eventi, con stato del collegamento. |
| **Relay** | Servizio che inoltra solo dati cifrati e conserva gli eventi per 72 ore se la Slave è offline. Non memorizza credenziali delle app. |

Il flusso è unidirezionale: Master → Relay → Slave.

## Struttura del progetto

```
tether/
├── shared/   Modelli condivisi (livelli, tipi di messaggio, evento inoltrato)
├── master/   App Master: NotificationForwarderService (listener) e schermata con toggle
├── slave/    App Slave: schermata di stato
├── SPECIFICATION.md
└── PROTOCOL.md
```

Build: `.\gradlew.bat :master:assembleDebug :slave:assembleDebug`

## Documentazione

- [SPECIFICATION.md](SPECIFICATION.md): specifica dei requisiti (ISO/IEC/IEEE 29148), con identificativi `REQ-*`, priorità e metodo di verifica.
- [PROTOCOL.md](PROTOCOL.md): protocollo v1 (cifratura, pairing via QR, formato dei messaggi, heartbeat, pausa).

## Requisiti principali

- Livelli di notifica: 0 (scartata), 1 (silenziosa, visibile all'accensione dello schermo), 2 (avviso immediato con vibrazione/suono).
- Cifratura end-to-end: il Relay non può leggere il contenuto.
- Abilitazione/pausa della Master, con consumo energetico minimo quando spenta (REQ-PAU, REQ-PRF-04).
- Intervallo dell'heartbeat configurabile dall'utente (predefinito 60 s).
- Segnalazione dello stato del collegamento e avviso se il telefono di casa è irraggiungibile.
- Nessuna configurazione delle app sul dispositivo Slave.

## Prossimi passi

1. Classificazione delle notifiche per livello nella Master.
2. Crittografia e pairing via QR (modulo `shared`).
3. Connessione al Relay e inoltro verso la Slave.

## Licenza

Da definire.
