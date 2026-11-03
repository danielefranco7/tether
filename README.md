# Tether

Inoltro di notifiche per la disconnessione consapevole. Lasci a casa il telefono principale (Madre) e porti con te un secondo dispositivo Android (Figlia), che riceve solo ciò che hai scelto, più la possibilità di chiamare in emergenza.

> Stato: fase di specifica. Il codice Android non è ancora stato creato.

## Componenti

| Componente | Descrizione |
|---|---|
| **App Madre** | Android, sul telefono di casa. Intercetta le notifiche, applica le regole a 3 livelli e invia eventi cifrati. |
| **App Figlia** | Android, sul telefono portato con sé. Riceve, mostra e segnala gli eventi, con stato del collegamento. |
| **Relay** | Servizio cloud che inoltra solo dati cifrati e conserva gli eventi se la Figlia è offline. |

Il flusso è unidirezionale: Madre → Relay → Figlia.

## Documentazione

- [SPECIFICATION.md](SPECIFICATION.md): specifica dei requisiti (ISO/IEC/IEEE 29148), con identificativi `REQ-*`, priorità e metodo di verifica.

## Requisiti principali

- Livelli di notifica: Livello 0 (scartata), Livello 1 (vibrazione), Livello 2 (silenziosa).
- Cifratura end-to-end: il Relay non può leggere il contenuto.
- Segnalazione dello stato del collegamento e avviso se il telefono di casa è irraggiungibile.
- Nessuna configurazione delle app sul dispositivo Figlia.

## Piattaforma

- Android (App Madre e App Figlia). iOS non è supportato come Madre per limiti del sistema (VIN-01).

## Prossimi passi

1. Approvazione della specifica e chiusura di `OPEN-01`…`OPEN-07`.
2. Definizione del protocollo di eventi (formato versionato).
3. Creazione del progetto Android (Kotlin, Gradle) con moduli `madre`, `figlia`, `shared`.

## Licenza

Da definire.
