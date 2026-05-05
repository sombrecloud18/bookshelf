<script setup>
import { onUnmounted, watch } from 'vue';
import { useLikes } from '../composables/useLikes.js';

const props = defineProps({
  targetType: { type: String, required: true }, // 'REVIEW' | 'COMMENT'
  targetId: { type: String, required: true },
  initialCount: { type: Number, default: 0 },
  initialLiked: { type: Boolean, default: false },
  size: { type: String, default: 'sm' }, // 'sm' | 'xs'
});

let state = useLikes(props.targetType, props.targetId, {
  count: props.initialCount,
  liked: props.initialLiked,
});

// Re-sync if parent updates the initial values (rare).
watch(() => props.initialCount, v => { state.count.value = v; });
watch(() => props.initialLiked, v => { state.liked.value = v; });

onUnmounted(() => state.dispose());
</script>

<template>
  <button
    type="button"
    class="inline-flex items-center gap-1 transition-colors"
    :class="[
      state.liked.value ? 'text-red-500' : 'text-gray-500 hover:text-red-500',
      size === 'xs' ? 'text-xs' : 'text-sm',
      state.busy.value ? 'opacity-60 cursor-wait' : 'cursor-pointer',
    ]"
    :disabled="state.busy.value"
    @click="state.toggle()"
  >
    <svg
      :class="size === 'xs' ? 'w-3 h-3' : 'w-4 h-4'"
      :fill="state.liked.value ? 'currentColor' : 'none'"
      stroke="currentColor"
      stroke-width="2"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
      />
    </svg>
    <span>{{ state.count.value }}</span>
  </button>
</template>
