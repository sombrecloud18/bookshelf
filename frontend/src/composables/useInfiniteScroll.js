import { onBeforeUnmount, onMounted, watch } from 'vue';

/**
 * Universal "load when sentinel is visible" hook.
 *
 * Usage:
 *   const sentinel = ref(null);
 *   useInfiniteScroll(sentinel, {
 *     hasMore: () => props.hasMore,
 *     loading: () => props.loadingMore,
 *     onLoad: () => emit('load-more'),
 *     reactiveTriggers: () => [props.hasMore, props.loadingMore, props.items.length],
 *   });
 *
 * `reactiveTriggers` re-checks whether the sentinel is already in viewport
 * after data changes — needed when one page does not fill the screen, so
 * IntersectionObserver does not see a transition.
 */
export function useInfiniteScroll(sentinelRef, { hasMore, loading, onLoad, reactiveTriggers, rootMargin = '400px 0px' } = {}) {
  let observer = null;

  function tryLoad() {
    if (typeof hasMore === 'function' && !hasMore()) return;
    if (typeof loading === 'function' && loading()) return;
    onLoad();
  }

  onMounted(() => {
    observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) tryLoad();
      }
    }, { rootMargin });
    if (sentinelRef.value) observer.observe(sentinelRef.value);
  });

  onBeforeUnmount(() => {
    if (observer) observer.disconnect();
  });

  if (typeof reactiveTriggers === 'function') {
    watch(reactiveTriggers, () => {
      if (!sentinelRef.value) return;
      const rect = sentinelRef.value.getBoundingClientRect();
      const px = parseInt(rootMargin, 10) || 0;
      if (rect.top < window.innerHeight + px) tryLoad();
    });
  }
}
