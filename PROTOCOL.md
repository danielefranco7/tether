# Protocollo Tether – specifica v1

**Versione protocollo:** 1
**Riferimenti:** REQ-INT-03, REQ-INT-04, REQ-SEC-01…05, REQ-FWD-02…04, REQ-STA-01/03/07
**Stato:** Bozza

Questo documento definisce il formato dei messaggi scambiati tra App Master, Relay e App Slave. È indipendente da Android: un client Linux dedicato deve poter implementare lo stesso protocollo.

---

## 1. Principi

1. Il Relay instrada messaggi cifrati e non legge mai il contenuto (REQ-SEC-01).
2. Ogni messaggio ha una versione esplicita; i client ignorano i campi sconosciuti (compatibilità in avanti).
3. Il flusso è unidirezionale per i dati: Master → Slave. Gli ACK sono l'unica risposta della Slave.
4. Gli algoritmi sono standard e noti (REQ-SEC-04).

---

## 2. Algoritmi

| Scopo | Algoritmo |
|---|---|
| Scambio di chiavi | X25519 (ECDH) |
| Derivazione chiave sessione | HKDF-SHA256 |
| Cifratura contenuto | XChaCha20-Poly1305 (AEAD) |
| Firma messaggi di controllo | Ed25519 |
| Trasporto | TLS 1.3 (WebSocket su `wss://`) |

La chiave privata di ciascun dispositivo è conservata nel keystore del sistema (REQ-SEC-03). Il Relay conserva solo le chiavi pubbliche e gli identificativi opachi.

---

## 3. Abbinamento

1. La Slave mostra un QR con: `pairingId` (casuale, 128 bit), chiave pubblica X25519 (`slavePk`), URL del Relay.
2. La Master scansiona il QR, genera la propria coppia di chiavi e calcola `shared = X25519(masterSk, slavePk)`.
3. Entrambi derivano `K = HKDF(shared, salt = pairingId, info = "tether-v1-session")`. Il Relay non conosce `shared`.
4. La Master invia `PAIR_HELLO` cifrato con `K` contenente la propria chiave pubblica di firma Ed25519. La Slave risponde con `PAIR_ACK`.
5. Da questo momento i messaggi sono autenticati con le chiavi di firma concordate.

**Autenticazione dell'abbinamento:** la chiave della Slave arriva alla Master tramite QR, cioè fuori banda; il Relay non può sostituirla. Se il Relay alterasse la chiave della Master, i due lati calcolerebbero chiavi diverse e la decifratura di `PAIR_HELLO` fallirebbe: l'abbinamento si interrompe senza esporre dati (solo disservizio). Non serve quindi un codice di confronto.

**Conferma sulla Slave:** dopo la scansione, la Slave mostra "Abbinare questo telefono di casa?" con un solo tocco di conferma. Questo impedisce a chi inquadra il QR prima del proprietario di abbinare il proprio dispositivo. Il QR è valido 5 minuti e per un solo abbinamento.

**Revoca (REQ-PAI-03):** un messaggio `PAIR_REVOKE` firmato, oppure cancellazione locale su una delle due app. Il Relay elimina il `pairingId` e rifiuta ogni messaggio successivo per quell'abbinamento.

---

## 4. Struttura del messaggio (busta)

Ogni messaggio sul filo è una busta JSON UTF-8 con questi campi **in chiaro**:

| Campo | Tipo | Descrizione |
|---|---|---|
| `v` | uint | Versione del protocollo (1) |
| `pid` | string | `pairingId` opaco |
| `kind` | string | `DATA` oppure `CTRL` (vedi sotto) |
| `id` | string | UUID v4 del messaggio, usato per deduplicazione e ACK |
| `seq` | uint64 | Contatore monotono per mittente |
| `ts` | int64 | Epoch in millisecondi, ora del mittente |
| `nonce` | base64 | Nonce di 24 byte |
| `ct` | base64 | Payload cifrato con AEAD (autenticazione inclusa) |
| `sig` | base64 | Firma Ed25519 su tutti i campi precedenti |

Il `kind` è in chiaro perché il Relay deve distinguere i messaggi di controllo (heartbeat) per il monitoraggio dello stato (REQ-STA-06). Il contenuto semantico di ogni messaggio è sempre cifrato.

Il payload cifrato (`ct`) contiene un oggetto JSON con i campi del tipo di messaggio (sezione 5).

**Associazione dei dati (AAD):** `v`, `pid`, `kind`, `id`, `seq`, `ts` sono passati come dati autenticati, così che ogni modifica in transito faccia fallire la decifratura.

---

## 5. Tipi di messaggio

### 5.1 DATA (kind = `DATA`)

Payload cifrato:

```json
{
  "type": "EVENT",
  "level": 1,
  "event": {
    "eventId": "uuid",
    "app": "com.whatsapp",
    "appLabel": "WhatsApp",
    "sender": "Mamma",
    "title": "Mamma",
    "text": "Torni per cena?",
    "category": "message",
    "postedAt": 1762170000000,
    "conversationKey": "wa:mamma"
  }
}
```

| `type` | Uso |
|---|---|
| `EVENT` | Notifica normalizzata (REQ-CAP-02). `level` è 1 (avviso) o 2 (silenzioso). Il livello 0 non viene mai inviato (REQ-RUL-06). |
| `EVENT_CANCEL` | La notifica è stata rimossa sulla Master (REQ-CAP-05). Riferisce `eventId`. |
| `MISSED_CALL` | Chiamata persa (REQ-CAL-01). Campi: `callerName`, `callerNumber`, `postedAt`, `level`. |
| `EVENT` con `level` 1 e VIP | Stesso formato: la promozione è già applicata dalla Master. |

Regole:
- `text` è limitato a 1000 caratteri. Il contenuto più lungo viene troncato sulla Master (REQ-FWD-05: nessun allegato).
- `conversationKey` serve a raggruppare aggiornamenti della stessa conversazione (REQ-CAP-04).

### 5.2 CTRL (kind = `CTRL`)

Payload cifrato, o campo `ct` vuoto per l'heartbeat, come descritto sotto.

| `type` | Mittente | Contenuto | Uso |
|---|---|---|---|
| `HEARTBEAT` | Master | `battery` (0–100, opzionale), `state` (`OK`, `DEGRADED`, `NO_NOTIFICATION_ACCESS`, `NO_NETWORK`) | REQ-STA-01, REQ-STA-05, REQ-STA-07 |
| `PAUSED` | Master | nessuno | Inviato una volta alla disattivazione; la Slave mostra "Master in pausa" (REQ-PAU-03) |
| `RESUMED` | Master | nessuno | Inviato alla riattivazione; sostituisce la pausa (REQ-PAU-04) |
| `ACK` | Slave | `ackId` (id del messaggio ricevuto) | REQ-FWD-03 |
| `PAIR_REVOKE` | Entrambi | motivo opzionale | REQ-PAI-03 |

**Heartbeat:** per permettere al Relay di rilevare l'assenza di segnali senza leggere il contenuto (REQ-STA-06), l'heartbeat viene inviato con `ct` cifrato come gli altri messaggi. Il Relay conosce solo `kind = CTRL` e la data di arrivo. Il campo `state` è dentro il payload cifrato e non è visibile al Relay.

**Frequenza:** ogni 60 secondi per default (REQ-STA-01), configurabile dall'utente sulla Master in base alle proprie necessità di connessione e consumo energetico. La Slave non presuppone un intervallo fisso: la soglia di allarme (REQ-STA-03) deve essere almeno tre volte l'intervallo configurato. La Slave segnala il collegamento come in ritardo dopo 10 minuti senza heartbeat (REQ-STA-03).

---

## 6. Consegna e affidabilità

- **ACK (REQ-FWD-03):** la Slave invia un `ACK` per ogni `DATA` ricevuto, anche se duplicato. Il Relay conserva il messaggio finché non riceve l'ACK o scade.
- **Ritentativi:** backoff esponenziale tra 5 secondi e 5 minuti. Il Relay conserva i messaggi non confermati per 72 ore di default (REQ-FWD-02).
- **Ordinamento:** la Slave ordina per `postedAt`, non per ordine di arrivo. Gli eventi sono mostrati con l'orario originale.
- **Deduplicazione (REQ-FWD-04):** la Slave ignora un `id` già visto. Mantiene un registro degli ultimi 2000 `id` o delle ultime 72 ore, quale delle due è più ampia.

---

## 7. Sicurezza

### 7.1 Protezione dal replay (REQ-SEC-05)
- La Slave accetta un messaggio solo se `seq` è maggiore dell'ultimo `seq` visto per quel mittente.
- Eccezione: se il `ts` del messaggio è più vecchio di 72 ore, viene scartato anche se `seq` è valido.
- Un `id` già visto viene ignorato (vedi 6).

### 7.2 Integrità
- Ogni messaggio è autenticato con AEAD sul payload (`ct`) e firmato Ed25519 sulla busta (`sig`).
- Un messaggio con firma non valida viene scartato e registrato localmente, senza notifica all'utente.

### 7.3 Cosa vede il Relay (REQ-SEC-02)

| Dato | Visibile al Relay |
|---|---|
| `pid` (opaco) | Sì |
| `kind` (DATA / CTRL) | Sì |
| Dimensione del messaggio | Sì |
| Orario di arrivo | Sì |
| Contenuto, mittente, app, testo, livello, batteria | **No** |
| Chiavi private | **No** |

---

## 8. Versionamento e compatibilità (REQ-INT-03, REQ-MNT-02)

- Il campo `v` indica la versione del protocollo. Una versione nuova deve accettare la precedente per almeno una release.
- Campi nuovi nel payload cifrato devono avere un valore predefinito; i client ignorano campi sconosciuti.
- Un `type` sconosciuto viene ignorato e registrato, senza errore per l'utente.
- Un `v` non supportato viene rifiutato con un messaggio di stato all'utente (REQ-REL-04).

---

## 9. Note per client alternativi (REQ-INT-04)

Un client Linux dedicato deve implementare:
- WebSocket TLS verso il Relay;
- X25519, HKDF-SHA256, XChaCha20-Poly1305, Ed25519 (librerie standard, es. libsodium);
- Il parsing della busta JSON e dei tipi `DATA` e `CTRL`;
- Il calcolo dell'ACK e la deduplicazione.

Non richiede Google Play Services (REQ-INT-05).

---

## 10. Questioni aperte

| ID | Domanda |
|---|---|
| PRO-OPEN-01 | **Chiuso:** il `kind` in chiaro è accettato come compromesso per REQ-SEC-02 (decisione del 2026-11-03). |
| PRO-OPEN-02 | **Chiuso:** nessun codice di confronto. Abbinamento tramite QR a validità 5 minuti e conferma con un tocco sulla Slave (sezione 3). |
| PRO-OPEN-03 | **Chiuso:** `pid` stabile per tutta la vita dell'abbinamento, per semplicità d'uso (decisione del 2026-11-03). La correlabilità è mitigata dal fatto che il Relay non conosce le identità reali degli utenti. |
