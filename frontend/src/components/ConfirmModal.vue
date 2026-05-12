<script setup>
import { computed } from 'vue';
import { useConfirmState, resolveConfirm } from '../composables/useConfirm.js';

const state = useConfirmState();

// Modal остаётся открытым, пока есть отложенный resolve — обработчик update:open
// гарантирует, что закрытие по крестику/Esc тоже резолвится как «отказ».
const open = computed({
  get: () => state.value.open,
  set: v => {
    if (!v) resolveConfirm(false);
  },
});
</script>

<template>
  <UModal v-model:open="open" :title="state.title" class="z-100">
    <template #body>
      <div class="text-sm text-white">{{ state.message }}</div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" class="bg-white hover:!text-white" @click="resolveConfirm(false)">{{ state.cancelLabel }}</UButton>
        <UButton
          :color="state.variant === 'danger' ? 'error' : 'primary'"
          :variant="state.variant === 'danger' ? 'soft' : 'solid'"
          class="hover:!text-white"
          @click="resolveConfirm(true)"
        >
          {{ state.confirmLabel }}
        </UButton>
      </div>
    </template>
  </UModal>
</template>
