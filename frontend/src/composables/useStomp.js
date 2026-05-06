import { Client } from '@stomp/stompjs';

// Single shared STOMP client for the whole app — multiple subscriptions
// (likes, events, ...) share one WebSocket connection.
let client = null;
let connectingPromise = null;
const subscriptions = new Map(); // topic -> { subscription, refs: Set<Function> }

function buildBrokerUrl() {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws`;
}

function ensureConnected() {
  if (client && client.connected) return Promise.resolve();
  if (connectingPromise) return connectingPromise;

  client = new Client({
    brokerURL: buildBrokerUrl(),
    reconnectDelay: 5000,
    debug: () => {},
  });

  connectingPromise = new Promise((resolve, reject) => {
    client.onConnect = () => {
      // Re-subscribe everything that was registered before the connection was up.
      subscriptions.forEach((entry, topic) => {
        if (!entry.subscription) {
          entry.subscription = client.subscribe(topic, (msg) => {
            try {
              const payload = JSON.parse(msg.body);
              entry.refs.forEach((cb) => cb(payload));
            } catch (e) {
              console.error('STOMP payload parse error:', e);
            }
          });
        }
      });
      resolve();
    };
    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      reject(new Error('STOMP error'));
    };
    client.activate();
  });

  return connectingPromise;
}

/**
 * Subscribe to a STOMP topic. Multiple callbacks for the same topic share a single broker
 * subscription. Returns an unsubscribe function — call it on component unmount.
 */
export function subscribeStomp(topic, callback) {
  let entry = subscriptions.get(topic);
  if (!entry) {
    entry = { subscription: null, refs: new Set() };
    subscriptions.set(topic, entry);
  }
  entry.refs.add(callback);

  ensureConnected().then(() => {
    if (!entry.subscription && client && client.connected) {
      entry.subscription = client.subscribe(topic, (msg) => {
        try {
          const payload = JSON.parse(msg.body);
          entry.refs.forEach((cb) => cb(payload));
        } catch (e) {
          console.error('STOMP payload parse error:', e);
        }
      });
    }
  });

  return () => {
    entry.refs.delete(callback);
    if (entry.refs.size === 0) {
      try {
        entry.subscription?.unsubscribe();
      } catch {}
      subscriptions.delete(topic);
    }
  };
}
