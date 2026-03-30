<template>
  <UFormField :label="label" :name="name" class="flex-1">
    <USelect
      :model-value="modelValue"
      :items="items"
      :placeholder="placeholder"
      :disabled="disabled"
      color="secondary"
      size="xl"
      variant="outline"
      highlight
      class="min-w-46"
      @update:model-value="handleUpdate"
    />
  </UFormField>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  name: {
    type: String,
    required: true,
  },
  modelValue: {
    type: String,
    default: '',
  },
  items: {
    type: Array,
    default: () => [],
  },
  placeholder: {
    type: String,
    default: 'Выберите значение',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:modelValue', 'change']);

const handleUpdate = value => {
  emit('update:modelValue', value);
  emit('change', value);
};
</script>

<style scoped>
.custom-select button {
  border: 2px solid #58d0f2 !important;
  border-radius: 0.375rem;
  min-height: 48px;
  font-size: 16px;
}

.custom-select button:focus {
  border-color: #2563eb !important;
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
</style>
