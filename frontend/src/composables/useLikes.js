import { Client } from '@stomp/stompjs';
import { ref } from 'vue';
import { api } from '../api.js';

// Single shared STOMP client for the whole app.
let client = null;
let connectingPromise = null;
const subscriptions = new Map(); // topic -> { unsubscribe, refs: Set<Function> }

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

function subscribe(topic, callback) {
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

/**
 * Composable that exposes a reactive like state for a single (targetType, targetId)
 * with optimistic toggling and live broadcast updates.
 *
 * Usage in a component:
 *   const like = useLikes('REVIEW', review.id, { count: review.likes, liked: review.liked });
 *   onUnmounted(like.dispose);
 *   <button @click="like.toggle()">{{ like.count.value }}</button>
 */
export function useLikes(targetType, targetId, initial = {}) {
  const count = ref(initial.count ?? 0);
  const liked = ref(initial.liked ?? false);
  const busy = ref(false);

  const topic = `/topic/likes/${targetType.toLowerCase()}/${targetId}`;
  const unsubscribe = subscribe(topic, (payload) => {
    if (typeof payload?.count === 'number') {
      count.value = payload.count;
    }
  });

  async function toggle() {
    if (busy.value) return;
    busy.value = true;
    const prevLiked = liked.value;
    const prevCount = count.value;
    // Optimistic update
    liked.value = !prevLiked;
    count.value = prevCount + (prevLiked ? -1 : 1);
    try {
      const state = await api.post(`/likes/${targetType}/${targetId}/toggle`);
      if (state) {
        liked.value = !!state.liked;
        count.value = state.count ?? count.value;
      }
    } catch (e) {
      // Revert on error
      liked.value = prevLiked;
      count.value = prevCount;
      console.error('Ошибка лайка:', e);
    } finally {
      busy.value = false;
    }
  }

  return { count, liked, busy, toggle, dispose: unsubscribe };
}
