# Specifica dei Requisiti Software (SRS) – Tether

**Prodotto:** Tether – inoltro notifiche per la disconnessione consapevole
**Riferimento normativo:** ISO/IEC/IEEE 29148:2018 – Requirements engineering
**Versione:** 0.3 (bozza per revisione)
**Data:** 2026-11-03
**Stato:** Bozza – i requisiti non sono ancora approvati

## Cronologia delle revisioni

| Versione | Data | Autore | Descrizione |
|---|---|---|---|
| 0.3 | 2026-11-03 | Team di prodotto | Aggiunti abilitazione/pausa della Master (REQ-PAU), requisiti di consumo energetico (REQ-PRF-04…07), latenza di consegna resa non critica (REQ-PRF-01).
| 0.2 | 2026-11-03 | Team di prodotto | Rinominazioni e correzioni 
| 0.1 | 2026-11-03 | Team di prodotto | Prima stesura dai requisiti emersi nella fase di concept |

---

## 1. Introduzione

### 1.1 Scopo

Il presente documento specifica i requisiti del sistema Tether, destinato a un utente che desidera lasciare a casa il proprio smartphone principale (ad esempio durante un'escursione) senza perdere notifiche importanti né la possibilità di effettuare una chiamata di emergenza. Il documento è rivolto a sviluppatori, responsabili di prodotto, tester e futuri progettisti dell'hardware dedicato.

### 1.2 Ambito del prodotto

Il sistema è composto da tre elementi:

1. **App Master**: applicazione Android installata sul telefono principale, che rimane a casa, connesso a internet.
2. **App Slave**: applicazione Android installata sul dispositivo secondario portato con sé.
3. **Servizio Relay**: servizio cloud che inoltra messaggi cifrati tra App Master e App Slave senza poterne leggere il contenuto.

Il sistema **inoltra notifiche in una sola direzione** (Master → Slave). Non permette di rispondere né di creare messaggi (vedi 1.2.2).

#### 1.2.1 Obiettivi

- Ridurre lo stress dell'utente garantendo che le informazioni importanti lo raggiungano.
- Rendere visibile in ogni momento lo stato del collegamento, perché l'assenza di notifiche non deve essere scambiata per assenza di eventi.
- Non richiedere la configurazione delle singole app di messaggistica sul dispositivo secondario.

#### 1.2.2 Fuori ambito (versione 1.0)

- Risposta o creazione di messaggi, email o messaggi su app di terze parti.
- Supporto a iOS come App Master (vedi VIN-01).
- Dispositivo hardware dedicato (e-ink, con sportellino, Linux). Il protocollo deve però restarne indipendente (REQ-INT-04).
- Trasferimento audio di chiamate su app di terze parti.
- Comunicazione satellitare.

### 1.3 Definizioni, acronimi e abbreviazioni

| Termine | Definizione |
|---|---|
| App Master | Applicazione sul telefono principale che cattura, filtra e inoltra le notifiche. |
| App Slave | Applicazione sul dispositivo secondario che riceve e mostra le notifiche. |
| Relay | Servizio cloud che inoltra pacchetti cifrati. |
| Evento | Rappresentazione normalizzata di una notifica o di una chiamata persa. |
| Livello 0 | Evento scartato, non inoltrato. |
| Livello 1 | Evento inoltrato in silenzio, visibile quando l'utente accende lo schermo. |
| Livello 2 | Evento inoltrato con avviso immediato (vibrazione/suono) sulla Slave. |
| VIP | Contatto le cui notifiche possono essere promosse a livello superiore. |
| Heartbeat | Segnale periodico inviato dalla Master per attestare che il collegamento è attivo. |
| Abbinamento | Procedura di scambio di chiavi crittografiche tra Master e Slave. |
| E2EE | Cifratura end-to-end. |
| MoSCoW | Priorità: Must (deve), Should (dovrebbe), Could (potrebbe), Won't (non in questa versione). |

### 1.4 Riferimenti

- ISO/IEC/IEEE 29148:2018, *Systems and software engineering – Life cycle processes – Requirements engineering*.
- ISO/IEC 25010:2023, *Systems and software engineering – Product quality model*.
- Regolamento (UE) 2016/679 (GDPR).
- Documentazione Android: `NotificationListenerService`, gestione dell'esecuzione in background e dei servizi in primo piano.

### 1.5 Struttura del documento

La sezione 2 descrive il prodotto in generale. La sezione 3 elenca i requisiti specifici. La sezione 4 definisce l'approccio di verifica. La sezione 5 contiene la tracciabilità e le questioni aperte.

---

## 2. Descrizione generale

### 2.1 Prospettiva del prodotto

Il sistema è autonomo e si appoggia alle funzioni di sistema Android per l'accesso alle notifiche. Utilizza il Relay come unico componente di back-end.

```
[App di terze parti] -> [Telefono Master] -> (cifrato) -> [Relay] -> (cifrato) -> [App Slave]
                         regole + cifratura                                   mostra + vibra
                                 ^------------- heartbeat ------------------------^
```

### 2.2 Funzioni principali

1. Abbinamento sicuro tra Master e Slave.
2. Cattura e normalizzazione delle notifiche sulla Master.
3. Applicazione di regole di filtro e di livello.
4. Inoltro cifrato tramite Relay.
5. Visualizzazione e segnalazione sulla Slave.
6. Segnalazione delle chiamate (perse o ricevute).
7. Monitoraggio e segnalazione dello stato del collegamento.

### 2.3 Caratteristiche degli utenti

| Utente | Descrizione | Competenza tecnica |
|---|---|---|
| Utente sportivo/avventuriero | Persona che si disconnette per alcune ore e vuole una rete di sicurezza. Utente primario. | Bassa-media |
| Utente a casa (opzionale) | Persona che gestisce il telefono Master a casa. Può coincidere con l'utente primario. | Bassa |

### 2.4 Ambiente operativo

- **Master:** smartphone Android, sempre acceso, in carica o con batteria sufficiente, connesso a internet (Wi-Fi o dati).
- **Slave:** smartphone Android (anche usato e a basso costo), con SIM o connettività dati.
- **Relay:** infrastruttura cloud ad alta disponibilità.

### 2.5 Vincoli

| ID | Vincolo |
|---|---|
| VIN-01 | iOS non permette ad applicazioni di terze parti di leggere le notifiche di altre app. L'App Master è quindi limitata ad Android. |
| VIN-02 | Android può limitare i servizi in background; sono necessarie esclusioni dall'ottimizzazione della batteria. |
| VIN-03 | Android 15 e successivi possono nascondere il contenuto sensibile delle notifiche (ad esempio codici monouso) alle app con accesso alle notifiche. |
| VIN-04 | La distribuzione su Google Play è soggetta alle politiche sull'accesso alle notifiche; potrebbe essere necessaria una distribuzione alternativa. |
| VIN-05 | Il contenuto di una notifica è limitato a ciò che l'app di origine mostra. |

### 2.6 Assunzioni e dipendenze

| ID | Assunzione |
|---|---|
| ASS-01 | Il telefono Master resta acceso e connesso a internet per tutta la durata dell'uso. |
| ASS-02 | La Slave dispone di copertura di rete almeno intermittente. |
| ASS-03 | L'utente concede i permessi necessari (accesso notifiche, esclusione batteria). |
| ASS-04 | Le chiamate possono essere gestite in due modi: SIM sulla Master (chiamata persa inoltrata come evento) oppure SIM spostata sulla Slave (chiamata ricevuta direttamente). |

---

## 3. Requisiti specifici

**Convenzioni:** ogni requisito ha un identificativo univoco, una priorità (MoSCoW) e un metodo di verifica: **T** = test, **D** = dimostrazione, **A** = analisi, **I** = ispezione.

### 3.1 Requisiti funzionali

#### 3.1.1 Abbinamento

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-PAI-01 | Il sistema deve permettere l'abbinamento di una Master con una Slave tramite scansione di un codice QR mostrato da una delle due app. | Must | D |
| REQ-PAI-02 | Durante l'abbinamento il sistema deve scambiare le chiavi crittografiche direttamente tra i dispositivi, senza che il Relay conosca le chiavi private. | Must | A, T |
| REQ-PAI-03 | Il sistema deve permettere di revocare l'abbinamento da entrambi i dispositivi; dopo la revoca non devono essere più inoltrati eventi. | Must | T |
| REQ-PAI-04 | Il sistema dovrebbe permettere più Figlie per una Master. | Could | D |

#### 3.1.2 Cattura delle notifiche (Master)

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-CAP-01 | L'App Master deve catturare le notifiche pubblicate da qualsiasi app installata, previa autorizzazione dell'utente. | Must | T |
| REQ-CAP-02 | L'App Master deve normalizzare ogni notifica in un Evento contenente almeno: identificativo, app di origine, mittente o titolo, testo, orario, categoria. | Must | T, I |
| REQ-CAP-03 | L'App Master deve ignorare le notifiche in corso permanente (ad esempio riproduzione multimediale, servizi in primo piano) salvo diversa configurazione. | Should | T |
| REQ-CAP-04 | L'App Master deve raggruppare gli aggiornamenti successivi della stessa conversazione per evitare duplicati. | Should | T |
| REQ-CAP-05 | Quando una notifica viene rimossa sulla Master, l'App Master dovrebbe inviare un evento di annullamento; la Slave deve marcare l'evento come "gestito sulla Master". | Should | T |

#### 3.1.3 Regole e livelli

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-RUL-01 | Il sistema deve supportare tre livelli di trattamento: Livello 0 (scartata), Livello 1 (avviso immediato), Livello 2 (inoltro silenzioso). | Must | T |
| REQ-RUL-02 | L'utente deve poter assegnare un livello a ciascuna app installata sulla Master. Il valore predefinito per le app non configurate deve essere Livello 0. | Must | T |
| REQ-RUL-03 | L'utente deve poter definire contatti VIP; un evento con mittente VIP deve essere promosso a Livello 1. | Must | T |
| REQ-RUL-04 | L'utente dovrebbe poter definire parole chiave che promuovono un evento a Livello 1. | Should | T |
| REQ-RUL-05 | L'utente dovrebbe poter definire regole di escalation (ad esempio: tre tentativi dello stesso mittente entro un intervallo configurabile promuovono l'evento a Livello 1). | Could | T |
| REQ-RUL-06 | La valutazione delle regole deve avvenire sulla Master, prima dell'invio, in modo che gli eventi di Livello 0 non lascino mai il telefono. | Must | A, T |
| REQ-RUL-07 | L'utente dovrebbe poter definire fasce orarie in cui un livello viene degradato (ad esempio Livello 1 → Livello 2). | Could | T |

#### 3.1.4 Inoltro e ricezione

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-FWD-01 | La Master deve inviare ogni evento di Livello 1 e 2 alla Slave tramite il Relay, in forma cifrata. | Must | T |
| REQ-FWD-02 | Se la Slave non è raggiungibile, il Relay deve conservare gli eventi cifrati per un periodo configurabile (valore predefinito: 72 ore) e consegnarli al ripristino, preservando l'orario originale. | Must | T |
| REQ-FWD-03 | La Slave deve confermare la ricezione di ogni evento; il Relay deve ritentare la consegna fino a conferma o scadenza. | Must | T |
| REQ-FWD-04 | La Slave deve eliminare automaticamente i duplicati di un evento. | Must | T |
| REQ-FWD-05 | Il sistema non deve inoltrare allegati multimediali nella versione 1.0. | Must | I |

#### 3.1.5 Presentazione sulla Slave

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-UI-01 | L'App Slave deve fare vibrare il dispositivo alla ricezione di un evento di Livello 1. | Must | T |
| REQ-UI-02 | L'App Slave non deve produrre vibrazione né suono alla ricezione di un evento di Livello 2. | Must | T |
| REQ-UI-03 | L'App Slave deve mostrare l'elenco degli eventi ricevuti, ordinati per orario, con indicazione di app, mittente, testo e livello. | Must | D |
| REQ-UI-04 | L'App Slave deve conservare in locale, cifrati, gli ultimi N eventi (N configurabile, predefinito 200) e renderli consultabili offline. | Must | T |
| REQ-UI-05 | L'App Slave deve distinguere visivamente gli eventi non letti. | Should | D |
| REQ-UI-06 | L'App Slave dovrebbe offrire una modalità a basso consumo (schermo minimale, a basso contrasto, senza animazioni). | Should | D |
| REQ-UI-07 | L'App Slave dovrebbe offrire una modalità "dedicata" che limita l'uso del dispositivo all'app e al tasto di chiamata di emergenza. | Could | D |
| REQ-UI-08 | L'App Slave deve offrire un accesso diretto, sempre disponibile, alla chiamata di emergenza. | Must | D |

#### 3.1.6 Chiamate

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-CAL-01 | Se la SIM è sulla Master, l'App Master deve rilevare le chiamate perse e inoltrarle come eventi del tipo "chiamata persa", contenenti nome o numero del chiamante e orario. | Must | T |
| REQ-CAL-02 | Un evento di chiamata persa da un contatto VIP deve essere promosso a Livello 1. | Must | T |
| REQ-CAL-03 | L'App Slave deve permettere di richiamare un numero indicato in un evento di chiamata, tramite il numero del dispositivo Slave. | Should | D |
| REQ-CAL-04 | Se la SIM è spostata sulla Slave, il sistema non deve richiedere configurazioni aggiuntive per ricevere le chiamate; l'App Slave non deve interferire con la telefonia nativa. | Must | T |
| REQ-CAL-05 | L'App Slave deve informare l'utente che il numero mostrato in uscita è quello del dispositivo Slave, quando diverso dal numero principale. | Should | I |

#### 3.1.7 Stato del collegamento e affidabilità percepita

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-STA-01 | La Master deve inviare un heartbeat a un intervallo configurabile dall'utente, in base alle proprie necessità di connessione e di consumo energetico (predefinito: 60 secondi). | Must | T |
| REQ-STA-02 | L'App Slave deve mostrare in modo permanente lo stato del collegamento (collegato, in ritardo, irraggiungibile) e l'ora dell'ultimo segnale ricevuto. | Must | D |
| REQ-STA-03 | Se non riceve heartbeat per un tempo superiore a una soglia configurabile (predefinita: 10 minuti), e la Master non è in pausa (REQ-PAU-03), la Slave deve avvisare l'utente con vibrazione e indicare "telefono di casa irraggiungibile". | Must | T |
| REQ-STA-04 | Al ripristino del collegamento, la Slave deve informare l'utente e mostrare gli eventi accumulati. | Must | T |
| REQ-STA-05 | La Master deve segnalare localmente (all'utente) le condizioni che compromettono il servizio: accesso notifiche revocato, esclusione batteria assente, assenza di rete, batteria bassa. | Must | T |
| REQ-STA-06 | Il Relay deve rilevare l'assenza prolungata di heartbeat e notificarla alla Slave, anche se la Master non è in grado di inviare messaggi. | Must | T |
| REQ-STA-07 | La Master dovrebbe inviare alla Slave il livello di batteria del proprio dispositivo. | Should | T |

#### 3.1.8 Abilitazione e pausa

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-PAU-01 | L'App Master deve avere un interruttore di abilitazione facilmente accessibile dalla schermata principale e dalle notifiche di sistema. | Must | D |
| REQ-PAU-02 | Quando l'abilitazione è disattivata, la Master deve interrompere la cattura delle notifiche, i servizi in background e l'inoltro, senza richiedere altre azioni all'utente. | Must | T |
| REQ-PAU-03 | Quando la Master è disattivata, deve informare il Relay con un messaggio `PAUSED` (vedi PROTOCOL.md). La Slave deve mostrare "Master in pausa" e non deve generare allarmi di irraggiungibilità (REQ-STA-03). | Must | T |
| REQ-PAU-04 | Alla riattivazione, la Master deve riprendere l'inoltro senza nuovo abbinamento e mantenendo lo stesso `pid`. | Must | T |
| REQ-PAU-05 | Gli eventi generati durante la pausa non devono essere inoltrati. La Master non deve accumularli per consegnarli dopo la riattivazione. | Must | T |
| REQ-PAU-06 | Il sistema non deve garantire la consegna immediata di notifiche arrivate sulla Master in standby: l'utente accetta che le notifiche possano arrivare con ritardo, purché la Slave ne riceva ogni evento di Livello 1 e 2 (REQ-REL-02). | Must | I |

#### 3.1.9 Gestione dei dati

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-DAT-01 | L'utente deve poter cancellare la cronologia sulla Slave. | Must | T |
| REQ-DAT-02 | L'utente deve poter cancellare tutti i dati associati al proprio account sul Relay. | Must | T |
| REQ-DAT-03 | Il sistema deve fornire un'informativa sul trattamento dei dati personali prima dell'abbinamento. | Must | I |

### 3.2 Requisiti di interfaccia

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-INT-01 | L'App Master deve utilizzare l'interfaccia di sistema Android per l'accesso alle notifiche, e richiedere esplicitamente l'autorizzazione all'utente. | Must | I |
| REQ-INT-02 | La comunicazione tra le app e il Relay deve utilizzare un protocollo a basso consumo (ad esempio MQTT o WebSocket) su connessione protetta TLS. | Must | I |
| REQ-INT-03 | Il formato degli Eventi e dei messaggi di controllo deve essere documentato e versionato. | Must | I |
| REQ-INT-04 | Il protocollo non deve dipendere da funzioni specifiche di Android lato Slave, in modo da consentire client alternativi (ad esempio un dispositivo Linux dedicato). | Should | A |
| REQ-INT-05 | L'App Slave non deve dipendere da Google Play Services per la ricezione degli eventi. | Should | A, T |

### 3.3 Requisiti non funzionali

#### 3.3.1 Prestazioni

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-PRF-01 | Con la Master attiva e connessa, il sistema dovrebbe consegnare le notifiche alla Slave entro 60 secondi nel 95% dei casi. La latenza non è un requisito critico: l'utente è disconnesso e può accettare ritardi, purché gli eventi siano consegnati. | Should | T |
| REQ-PRF-02 | L'App Slave, in modalità a basso consumo e con schermo spento, dovrebbe consumare meno del 3% di batteria all'ora. *(Valore obiettivo da validare con prove su dispositivi reali.)* | Should | T |
| REQ-PRF-03 | L'App Master non deve ridurre in modo percepibile le prestazioni del telefono su cui è installata. | Must | T |
| REQ-PRF-04 | Con la funzione di abilitazione disattivata (REQ-PAU-01), l'App Master non deve eseguire alcun processo in background, non deve trattenere wake lock, non deve effettuare richieste di rete e non deve inviare heartbeat. | Must | T |
| REQ-PRF-05 | L'App Master dovrebbe raggruppare heartbeat, invii e operazioni di sistema in finestre periodiche, usando lo scheduler del sistema (ad esempio WorkManager), senza servizi in primo piano permanenti. | Should | A, T |
| REQ-PRF-06 | Con la Master attiva e in standby, il consumo aggiuntivo dovuto all'App Master dovrebbe essere inferiore al 1% di batteria al giorno. *(Valore obiettivo da validare con prove su dispositivi reali.)* | Should | T |
| REQ-PRF-07 | L'App Master deve ridurre automaticamente la frequenza degli heartbeat e dei controlli quando la batteria è sotto una soglia configurabile (predefinita: 20%). | Should | T |

#### 3.3.2 Sicurezza e privacy

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-SEC-01 | Il contenuto degli eventi deve essere cifrato end-to-end tra Master e Slave; il Relay non deve poterlo leggere. | Must | A, T |
| REQ-SEC-02 | Il Relay deve conservare solo i metadati strettamente necessari alla consegna (identificativi opachi, dimensione, orario). | Must | A |
| REQ-SEC-03 | Le chiavi private devono essere conservate nell'archivio protetto del sistema operativo (keystore). | Must | I |
| REQ-SEC-04 | Il sistema deve usare algoritmi crittografici standard e riconosciuti, senza implementazioni proprietarie. | Must | A |
| REQ-SEC-05 | Il sistema deve proteggere dagli attacchi di replay degli eventi. | Must | T |
| REQ-SEC-06 | I dati memorizzati localmente sulla Slave devono essere cifrati a riposo. | Must | I |
| REQ-SEC-07 | L'App Master non deve inoltrare notifiche di Livello 0 né dati diversi da quelli definiti nell'Evento. | Must | A, T |
| REQ-SEC-08 | L'utente dovrebbe poter escludere categorie sensibili (ad esempio notifiche bancarie, codici monouso) dall'inoltro. | Should | T |
| REQ-SEC-09 | Il trattamento dei dati personali deve rispettare il GDPR (minimizzazione, finalità, diritto alla cancellazione). | Must | A |

#### 3.3.3 Affidabilità e disponibilità

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-REL-01 | Il Relay deve avere una disponibilità mensile obiettivo di almeno 99,5%. | Should | A |
| REQ-REL-02 | Nessun evento di Livello 1 o 2 accettato dal Relay deve andare perso prima della consegna o della scadenza. | Must | T |
| REQ-REL-03 | L'App Master deve riavviarsi automaticamente dopo il riavvio del telefono e riprendere l'inoltro senza intervento dell'utente. | Must | T |
| REQ-REL-04 | In caso di guasto di un componente, il sistema deve segnalarlo all'utente invece di fallire in silenzio (cfr. REQ-STA). | Must | T |

#### 3.3.4 Usabilità

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-USA-01 | L'abbinamento e la configurazione iniziale devono poter essere completati in meno di 5 minuti da un utente non esperto. | Should | T |
| REQ-USA-02 | L'interfaccia della Slave deve essere utilizzabile con una sola mano e con testo leggibile all'aperto. | Should | D |
| REQ-USA-03 | L'app deve guidare l'utente nella concessione dei permessi necessari, spiegandone il motivo. | Must | D |
| REQ-USA-04 | Le interfacce devono essere disponibili almeno in italiano e inglese. | Should | I |

#### 3.3.5 Manutenibilità e portabilità

| ID | Requisito | Priorità | Verifica |
|---|---|---|---|
| REQ-MNT-01 | Il protocollo e il motore di regole devono essere separati dall'interfaccia utente, per consentire il riuso su altre piattaforme. | Should | I |
| REQ-MNT-02 | Il sistema deve supportare gli aggiornamenti del protocollo mantenendo la compatibilità con la versione precedente per almeno una versione. | Should | T |

### 3.4 Vincoli di progetto

| ID | Requisito | Priorità |
|---|---|---|
| REQ-CON-01 | L'App Master e l'App Slave sono sviluppate per Android. | Must |
| REQ-CON-02 | La versione 1.0 non include hardware dedicato. | Must |
| REQ-CON-03 | Il Relay non deve richiedere nessun account sul dispositivo Slave. | Must |

---

## 4. Verifica

| Metodo | Descrizione | Applicazione tipica |
|---|---|---|
| Test (T) | Esecuzione controllata con risultati misurabili | Funzioni, latenza, ripristino |
| Dimostrazione (D) | Uso operativo senza misure dettagliate | Interfaccia, flussi utente |
| Analisi (A) | Esame di progetto, calcoli, modelli | Sicurezza, privacy, architettura |
| Ispezione (I) | Revisione di documenti, codice, configurazioni | Conformità, documentazione |

**Piano di validazione con utenti:** prova con 10-20 utenti per almeno due settimane, misurando: notifiche perse o in ritardo, frequenza di caduta del collegamento, serenità percepita (questionario), tempo di configurazione.

---

## 5. Appendici

### 5.1 Matrice di tracciabilità (obiettivi → requisiti)

| Obiettivo | Requisiti principali |
|---|---|
| Ricevere solo ciò che conta | REQ-RUL-01…07, REQ-CAP-03, REQ-CAP-04 |
| Fidarsi del sistema | REQ-STA-01…07, REQ-REL-01…04, REQ-FWD-02, REQ-FWD-03 |
| Nessuna configurazione delle app sul secondo dispositivo | REQ-CAP-01, REQ-INT-05, REQ-CON-03 |
| Privacy e sicurezza | REQ-PAI-02, REQ-SEC-01…09, REQ-DAT-01…03 |
| Chiamata di emergenza | REQ-UI-08, REQ-CAL-04 |
| Evoluzione verso il dispositivo dedicato | REQ-INT-03, REQ-INT-04, REQ-MNT-01 |

### 5.2 Questioni aperte

| ID | Questione | Responsabile | Scadenza |
|---|---|---|---|
| OPEN-01 | Strategia di distribuzione dell'App Master (Google Play o distribuzione alternativa) alla luce delle politiche sull'accesso alle notifiche. | Prodotto | Da definire |
| OPEN-02 | Comportamento di Android 15+ con notifiche sensibili: verificare il contenuto effettivamente disponibile. | Sviluppo | Da definire |
| OPEN-03 | Scelta del trasporto (MQTT, WebSocket) e del servizio Relay (auto-ospitato o gestito). | Architettura | Da definire |
| OPEN-04 | Modello di business: abbonamento al Relay, acquisto dell'app, hardware. | Prodotto | Da definire |
| OPEN-05 | Verifica di unicità e della concorrenza sul mercato (smartwatch LTE, telefoni minimali, comunicatori satellitari) e ricerca di brevetti. | Prodotto | Da definire |
| OPEN-06 | Strategia per gli utenti con telefono principale iOS (funzioni limitate, ad esempio solo inoltro delle chiamate). | Prodotto | Da definire |
| OPEN-07 | Valori obiettivo di consumo e latenza (REQ-PRF-01, REQ-PRF-02) da validare con prove su dispositivi reali. | Sviluppo | Da definire |

### 5.3 Rischi principali

| Rischio | Impatto | Mitigazione |
|---|---|---|
| Android termina il servizio in background | Notifiche perse senza che l'utente lo sappia | REQ-STA-03, REQ-STA-05, REQ-STA-06; guida alle esclusioni batteria |
| Rifiuto o limitazioni da parte di Google Play | Impossibile raggiungere gli utenti | Distribuzione alternativa (OPEN-01) |
| Limitazioni sul contenuto delle notifiche | Esperienza ridotta | Gestione esplicita di VIN-03 e VIN-05 |
| Utenti iOS esclusi | Mercato ridotto | OPEN-06 |
| Perdita di fiducia per ritardi o cadute | Abbandono del prodotto | Indicatori di stato chiari e avvisi attivi |
