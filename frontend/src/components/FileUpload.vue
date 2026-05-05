<script setup>
import { ref } from 'vue';
import { api } from '../api.js';

const props = defineProps({
  scope: { type: String, required: true }, // 'avatars' | 'covers'
  modelValue: { type: String, default: '' },
  label: { type: String, default: 'Загрузить изображение' },
  accept: { type: String, default: 'image/png,image/jpeg,image/webp,image/gif' },
});

const emit = defineEmits(['update:modelValue', 'uploaded', 'error']);

const fileInput = ref(null);
const uploading = ref(false);
const error = ref(null);

function open() {
  fileInput.value?.click();
}

async function onChange(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    error.value = 'Файл слишком большой (максимум 5 МБ)';
    emit('error', error.value);
    return;
  }

  error.value = null;
  uploading.value = true;
  try {
    const res = await api.upload(`/files/${props.scope}`, file);
    if (res?.url) {
      emit('update:modelValue', res.url);
      emit('uploaded', res.url);
    }
  } catch (e) {
    error.value = e.message || 'Ошибка загрузки файла';
    emit('error', error.value);
  } finally {
    uploading.value = false;
    if (fileInput.value) fileInput.value.value = '';
  }
}

function clear() {
  emit('update:modelValue', '');
}
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center gap-3">
      <div
        class="w-20 h-20 rounded-xl border border-dashed border-gray-300 bg-gray-50 flex items-center justify-center overflow-hidden flex-none"
      >
        <img v-if="modelValue" :src="modelValue" alt="" class="w-full h-full object-cover" />
        <span v-else class="text-xs text-gray-400">Нет файла</span>
      </div>
      <div class="flex flex-col gap-2">
        <UButton size="sm" variant="outline" :loading="uploading" @click="open">{{ label }}</UButton>
        <UButton v-if="modelValue" size="xs" variant="ghost" color="neutral" @click="clear">Удалить</UButton>
      </div>
    </div>
    <input ref="fileInput" type="file" :accept="accept" class="hidden" @change="onChange" />
    <p v-if="error" class="text-xs text-red-500">{{ error }}</p>
  </div>
</template>
