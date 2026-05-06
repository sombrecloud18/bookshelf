import { ref } from 'vue';
import { api } from '../api.js';
import { subscribeStomp } from './useStomp.js';

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
  const unsubscribe = subscribeStomp(topic, (payload) => {
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
