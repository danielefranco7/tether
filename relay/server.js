// Relay base di Tether: inoltra i messaggi tra i client con lo stesso conversationKey.
// Se nella stanza non c'è nessun altro peer, i messaggi sono trattenuti (su file) e inoltrati
// in ordine al primo peer che entra. Versione di sviluppo: nessuna cifratura, nessuna autenticazione.

const fs = require('fs');
const path = require('path');
const { WebSocketServer } = require('ws');

const PORT = Number(process.env.PORT) || 8080;
const QUEUE_FILE = process.env.QUEUE_FILE || path.join(__dirname, 'queue.json');
const MAX_QUEUED = 500;
const wss = new WebSocketServer({ port: PORT });

// conversationKey -> Set di socket connessi
const rooms = new Map();

// conversationKey -> messaggi in attesa di un peer (ordine di arrivo)
const queues = new Map(
  Object.entries(fs.existsSync(QUEUE_FILE) ? JSON.parse(fs.readFileSync(QUEUE_FILE, 'utf8')) : {})
);

function saveQueues() {
  fs.writeFileSync(QUEUE_FILE, JSON.stringify(Object.fromEntries(queues)));
}

wss.on('connection', (ws) => {
  ws.room = null;

  ws.on('message', (data) => {
    let msg;
    try {
      msg = JSON.parse(data.toString());
    } catch {
      return;
    }

    // Il primo messaggio deve essere JOIN con la conversationKey.
    if (msg.type === 'JOIN' && typeof msg.conversationKey === 'string') {
      if (ws.room) rooms.get(ws.room)?.delete(ws);
      ws.room = msg.conversationKey;
      if (!rooms.has(ws.room)) rooms.set(ws.room, new Set());
      const peers = rooms.get(ws.room);
      peers.add(ws);
      console.log(`JOIN room=${ws.room} peers=${peers.size}`);

      // Consegna i messaggi trattenuti al nuovo peer, se nella stanza c'è anche un altro client.
      const queued = queues.get(ws.room);
      if (queued?.length && peers.size > 1) {
        for (const payload of queued) ws.send(payload);
        console.log(`DRAIN room=${ws.room} messages=${queued.length}`);
        queues.delete(ws.room);
        saveQueues();
      }
      return;
    }

    if (!ws.room) return;

    const others = [...(rooms.get(ws.room) ?? [])].filter(
      (peer) => peer !== ws && peer.readyState === peer.OPEN
    );

    if (others.length === 0) {
      // Nessun altro peer: trattieni il messaggio.
      const queued = queues.get(ws.room) ?? [];
      if (queued.length >= MAX_QUEUED) queued.shift();
      queued.push(data.toString());
      queues.set(ws.room, queued);
      saveQueues();
      console.log(`QUEUE room=${ws.room} type=${msg.type} pending=${queued.length}`);
      return;
    }

    for (const peer of others) peer.send(data.toString());
    console.log(`FWD room=${ws.room} type=${msg.type}`);
  });

  ws.on('close', () => {
    if (ws.room) rooms.get(ws.room)?.delete(ws);
  });
});

console.log(`Tether relay in ascolto sulla porta ${PORT}`);
