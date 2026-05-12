<script setup>
import { ref } from 'vue';
import { api } from '../api.js';

const props = defineProps({
  scope: { type: String, required: true }, // 'avatars' | 'covers'
  modelValue: { type: String, default: '' },
  label: { type: String, default: 'Загрузить изображение' },
  accept: { type: String, default: 'image/png,image/jpeg,image/webp,image/gif' },
  // Defer upload until the parent decides — used at registration where the user
  // has no JWT yet. Parent gets the File via @file and the data-URL preview via
  // @preview, and uploads itself after auth.
  deferred: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue', 'uploaded', 'error', 'file', 'preview']);

const fileInput = ref(null);
const uploading = ref(false);
const error = ref(null);
const localPreview = ref('');

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

  if (props.deferred) {
    const reader = new FileReader();
    reader.onload = () => {
      localPreview.value = String(reader.result || '');
      emit('preview', localPreview.value);
    };
    reader.readAsDataURL(file);
    emit('file', file);
    if (fileInput.value) fileInput.value.value = '';
    return;
  }

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
  if (props.deferred) {
    localPreview.value = '';
    emit('file', null);
    emit('preview', '');
  }
}
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center gap-3">
      <div
        class="w-20 h-20 rounded-xl border border-dashed border-gray-200 bg-gray-50 flex items-center justify-center overflow-hidden flex-none"
      >
        <img v-if="modelValue || localPreview" :src="modelValue || localPreview" alt="" class="w-full h-full object-cover" />
        <span v-else class="text-xs text-gray-400">Нет файла</span>
      </div>
      <div class="flex flex-col gap-2">
        <UButton size="sm" variant="outline" class="bg-white hover:!text-white" :loading="uploading" @click="open">{{ label }}</UButton>
        <UButton v-if="modelValue || localPreview" size="xs" variant="ghost" color="neutral" class="bg-white" @click="clear">Удалить</UButton>
      </div>
    </div>
    <input ref="fileInput" type="file" :accept="accept" class="hidden" @change="onChange" />
    <p v-if="error" class="text-xs text-red-500">{{ error }}</p>
  </div>
</template>
