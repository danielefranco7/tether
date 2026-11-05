// Client di prova: due socket nella stessa stanza, il secondo deve ricevere il messaggio del primo.
const WebSocket = require('ws');

const URL = process.env.RELAY_URL || 'ws://104.197.160.133:8080';
const room = 'test-room';

const master = new WebSocket(URL);
const slave = new WebSocket(URL);

let received = null;

slave.on('message', (data) => {
  received = JSON.parse(data.toString());
});

Promise.all([
  new Promise((res) => master.on('open', res)),
  new Promise((res) => slave.on('open', res)),
]).then(() => {
  master.send(JSON.stringify({ type: 'JOIN', conversationKey: room }));
  slave.send(JSON.stringify({ type: 'JOIN', conversationKey: room }));

  setTimeout(() => {
    master.send(JSON.stringify({ type: 'EVENT', event: { app: 'com.google.android.gm', level: 2 } }));
    setTimeout(() => {
      const ok = received && received.type === 'EVENT' && received.event.level === 2;
      console.log(ok ? 'PASS: slave ha ricevuto EVENT' : 'FAIL: nessun inoltro');
      master.close();
      slave.close();
      process.exit(ok ? 0 : 1);
    }, 300);
  }, 300);
});
