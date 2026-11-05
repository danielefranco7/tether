# Tether

Inoltro di notifiche per la disconnessione consapevole. Lasci a casa il telefono principale (Master) e porti con te un secondo dispositivo Android (Slave), che riceve solo ciò che hai scelto, più la possibilità di chiamare in emergenza.

> Stato: specifica v0.3 e protocollo v1 in bozza. Inoltro end-to-end Master → Relay → Slave funzionante in sviluppo, in chiaro (vedi [PROTOCOL.md](PROTOCOL.md) sezione 0).

## Componenti

| Componente | Descrizione |
|---|---|
| **App Master** | Android, sul telefono di casa. Intercetta le notifiche, applica le regole a 3 livelli e invia eventi cifrati. Ha un toggle di abilitazione: spenta non svolge alcuna attività in background. |
| **App Slave** | Android, sul telefono portato con sé. Riceve, mostra e segnala gli eventi, con stato del collegamento. |
| **Relay** | Servizio WebSocket (Node.js, cartella `relay/`) che inoltra i messaggi agli altri peer della stessa stanza. Attualmente non conserva né cifra nulla: la conservazione di 72 ore è ancora da implementare. |

Il flusso è unidirezionale: Master → Relay → Slave.

## Struttura del progetto

```
tether/
├── shared/   Modelli condivisi (livelli, tipi di messaggio, evento inoltrato)
├── master/   App Master: NotificationForwarderService (listener), client Relay e schermata con toggle e stato Relay
├── slave/    App Slave: client Relay e visualizzazione eventi
├── relay/    Relay WebSocket (server.js) e client di test
├── SPECIFICATION.md
└── PROTOCOL.md
```

Build: `.\gradlew.bat :master:assembleDebug :slave:assembleDebug`

## Relay

Avvio locale o su VM:

```
cd relay
npm install
node server.js          # porta da env PORT, predefinita 8080
```

Attenzione: `node server.js` è il relay; `test-client.js` è solo un client di prova. Il client di prova non va avviato sulla VM.

Le app puntano al relay tramite `gradle.properties` alla radice:

```
relayUrl=ws://<IP-del-relay>:8080
```

Dopo la modifica, ricompila e reinstalla Master e Slave. La porta del relay deve essere aperta nel firewall della VM. Non usare porte di altri servizi (per esempio 3306 di MariaDB). Chiudi le regole firewall di test con origine `0.0.0.0/0`.

Il traffico in chiaro `ws://` è ammesso solo per lo sviluppo (`usesCleartextTraffic`).

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

1. Crittografia e pairing via QR (modulo `shared`).
2. Coda di invio nella Master quando il Relay è irraggiungibile.
3. Conservazione degli eventi per 72 ore nel Relay, ACK e deduplicazione.
4. Compatibilità Android 4/6 per la Slave (minSdk 23 attuale, da verificare).

## Licenza

Da definire.
