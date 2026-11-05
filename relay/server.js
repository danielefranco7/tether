// Relay base di Tether: inoltra i messaggi tra i client con lo stesso conversationKey.
// Versione di sviluppo: nessuna cifratura, nessuna persistenza, nessuna autenticazione.

const { WebSocketServer } = require('ws');

const PORT = Number(process.env.PORT) || 8080;
const wss = new WebSocketServer({ port: PORT });

// conversationKey -> Set di socket connessi
const rooms = new Map();

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
      rooms.get(ws.room).add(ws);
      console.log(`JOIN room=${ws.room} peers=${rooms.get(ws.room).size}`);
      return;
    }

    if (!ws.room) return;

    // Inoltra a tutti gli altri client della stessa stanza.
    for (const peer of rooms.get(ws.room) ?? []) {
      if (peer !== ws && peer.readyState === peer.OPEN) {
        peer.send(data.toString());
      }
    }
    console.log(`FWD room=${ws.room} type=${msg.type}`);
  });

  ws.on('close', () => {
    if (ws.room) rooms.get(ws.room)?.delete(ws);
  });
});

console.log(`Tether relay in ascolto sulla porta ${PORT}`);
