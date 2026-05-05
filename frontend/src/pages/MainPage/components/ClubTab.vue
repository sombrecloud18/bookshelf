<script setup>
import { computed, ref } from 'vue';
import { api } from '../../../api.js';

const props = defineProps({
  items: { type: Array, default: () => [] }, // [{ id, title, text }]
});

const participants = ref(
  Object.fromEntries(
    props.items.map(i => [
      i.id,
      {
        count: typeof i.participantsCount === 'number' ? i.participantsCount : 0,
        status: null, // 'going' | 'not_going' | null
      },
    ]),
  ),
);

const open = ref(false);
const selectedId = ref(null);
const loadingRegistration = ref(false);

const selectedItem = computed(() => props.items.find(i => i.id === selectedId.value) || null);
const selectedState = computed(() => (selectedId.value ? participants.value[selectedId.value] : null));

async function openDetails(id) {
  selectedId.value = id;
  open.value = true;
  try {
    const data = await api.get(`/events/${id}/registered`);
    if (participants.value[id]) {
      participants.value[id].status = data.registered ? 'going' : null;
    }
  } catch {
    // ignore — user may not be authenticated
  }
}

async function confirmGoing() {
  if (!selectedId.value || loadingRegistration.value) return;
  const st = participants.value[selectedId.value];
  if (!st || st.status === 'going') return;
  loadingRegistration.value = true;
  try {
    await api.post(`/events/${selectedId.value}/register`);
    if (st.status !== 'going') st.count += 1;
    st.status = 'going';
  } catch (e) {
    console.error('Ошибка регистрации:', e);
  } finally {
    loadingRegistration.value = false;
  }
}

async function confirmNotGoing() {
  if (!selectedId.value || loadingRegistration.value) return;
  const st = participants.value[selectedId.value];
  if (!st || st.status === 'not_going') return;
  loadingRegistration.value = true;
  try {
    await api.delete(`/events/${selectedId.value}/register`);
    if (st.status === 'going') st.count = Math.max(0, st.count - 1);
    st.status = 'not_going';
  } catch (e) {
    console.error('Ошибка отмены регистрации:', e);
  } finally {
    loadingRegistration.value = false;
  }
}
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
    <UCard
      v-for="i in items"
      :key="i.id"
      variant="soft"
      class="bg-white rounded-2xl p-5 hover:shadow-xl transition-all duration-300"
    >
      <h3 class="text-lg font-semibold text-black">{{ i.title }}</h3>
      <p class="mt-2 text-sm text-gray-700">{{ i.text }}</p>
      <div class="mt-3 text-xs text-gray-500">Участников: {{ participants[i.id]?.count ?? 0 }}</div>
      <div class="mt-4 flex justify-end">
        <UButton variant="outline" class="rounded-xl" @click="openDetails(i.id)">Подробнее</UButton>
      </div>
    </UCard>
  </div>

  <UModal v-model:open="open" title="Книжный клуб">
    <template #body>
      <div v-if="selectedItem" class="space-y-3">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h3 class="text-xl font-semibold text-black">{{ selectedItem.title }}</h3>
            <p class="mt-2 text-sm text-gray-700 whitespace-pre-line">{{ selectedItem.text }}</p>
          </div>
        </div>

        <div class="text-sm text-gray-800">
          <span class="font-semibold text-black">Участников: </span>
          <span>{{ selectedState?.count ?? 0 }}</span>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex items-center justify-between gap-3 w-full">
        <UButton variant="outline" class="rounded-xl" @click="open = false">Закрыть</UButton>
        <div class="flex gap-3">
          <UButton color="primary" class="rounded-xl" :loading="loadingRegistration" @click="confirmGoing">Подтвердить участие</UButton>
          <UButton color="red" variant="soft" class="rounded-xl" :loading="loadingRegistration" @click="confirmNotGoing">Я не приду</UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
