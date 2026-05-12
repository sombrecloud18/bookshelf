<script setup>
import { computed, ref, watch, onUnmounted } from 'vue';
import { api } from '../../../api.js';
import { subscribeStomp } from '../../../composables/useStomp.js';

const props = defineProps({
  items: { type: Array, default: () => [] },
  query: { type: String, default: '' },
});

const filteredItems = computed(() => {
  const q = props.query.trim().toLowerCase();
  if (!q) return props.items;
  return props.items.filter(i =>
    (i.title || '').toLowerCase().includes(q) ||
    (i.text || '').toLowerCase().includes(q) ||
    (i.location || '').toLowerCase().includes(q) ||
    (i.organizer || '').toLowerCase().includes(q),
  );
});

// Local mirror of the participant counts and registration state, keyed by event id.
const stateById = ref({});

// Active STOMP subscriptions keyed by event id, so we can clean them up
// when items disappear or the component unmounts.
const subscriptions = new Map();

function subscribeTo(eventId) {
  if (subscriptions.has(eventId)) return;
  const topic = `/topic/events/${eventId}`;
  const dispose = subscribeStomp(topic, (payload) => {
    const slot = stateById.value[eventId];
    if (!slot) return;
    if (typeof payload?.currentParticipants === 'number') {
      slot.count = payload.currentParticipants;
    }
    if (payload?.maxParticipants != null) {
      slot.max = payload.maxParticipants;
    }
  });
  subscriptions.set(eventId, dispose);
}

watch(() => props.items, (items) => {
  const seen = new Set();
  for (const item of items) {
    seen.add(item.id);
    if (!stateById.value[item.id]) {
      stateById.value[item.id] = {
        count: typeof item.participantsCount === 'number' ? item.participantsCount : 0,
        max: item.maxParticipants ?? null,
        registered: false,
      };
    } else {
      stateById.value[item.id].count = typeof item.participantsCount === 'number'
        ? item.participantsCount
        : stateById.value[item.id].count;
      stateById.value[item.id].max = item.maxParticipants ?? stateById.value[item.id].max;
    }
    subscribeTo(item.id);
  }
  // Drop subscriptions for events that no longer exist in props.items.
  for (const [eventId, dispose] of subscriptions) {
    if (!seen.has(eventId)) {
      dispose();
      subscriptions.delete(eventId);
    }
  }
}, { immediate: true });

onUnmounted(() => {
  subscriptions.forEach((dispose) => dispose());
  subscriptions.clear();
});

const open = ref(false);
const selectedId = ref(null);
const loadingRegistration = ref(false);
const errorMessage = ref(null);

const selectedItem = computed(() => props.items.find(i => i.id === selectedId.value) || null);
const selectedState = computed(() => (selectedId.value ? stateById.value[selectedId.value] : null));
const isFull = computed(() => {
  const s = selectedState.value;
  if (!s || s.max == null) return false;
  return s.count >= s.max;
});

async function refreshState(eventId) {
  try {
    const [event, regResp] = await Promise.all([
      api.get(`/events/${eventId}`),
      api.get(`/events/${eventId}/registered`).catch(() => ({ registered: false })),
    ]);
    stateById.value[eventId] = {
      count: event?.currentParticipants ?? 0,
      max: event?.maxParticipants ?? null,
      registered: !!regResp?.registered,
    };
  } catch (e) {
    console.error('Не удалось обновить данные мероприятия:', e);
  }
}

async function openDetails(id) {
  selectedId.value = id;
  open.value = true;
  errorMessage.value = null;
  await refreshState(id);
}

async function confirmGoing() {
  if (!selectedId.value || loadingRegistration.value) return;
  errorMessage.value = null;
  loadingRegistration.value = true;
  try {
    await api.post(`/events/${selectedId.value}/register`);
    await refreshState(selectedId.value);
  } catch (e) {
    errorMessage.value = e.message || 'Не удалось зарегистрироваться';
  } finally {
    loadingRegistration.value = false;
  }
}

async function cancelGoing() {
  if (!selectedId.value || loadingRegistration.value) return;
  errorMessage.value = null;
  loadingRegistration.value = true;
  try {
    await api.delete(`/events/${selectedId.value}/register`);
    await refreshState(selectedId.value);
  } catch (e) {
    errorMessage.value = e.message || 'Не удалось отменить участие';
  } finally {
    loadingRegistration.value = false;
  }
}
</script>

<template>
  <div
    v-if="filteredItems.length === 0 && props.query"
    class="bg-white rounded-2xl p-6 text-sm text-gray-500 text-center"
  >
    По запросу «{{ props.query }}» мероприятий не найдено.
  </div>
  <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-6">
    <UCard
      v-for="i in filteredItems"
      :key="i.id"
      variant="soft"
      class="bg-white rounded-2xl p-5 hover:shadow-xl transition-all duration-300"
    >
      <h3 class="text-lg font-semibold text-black">{{ i.title }}</h3>
      <p class="mt-2 text-sm text-gray-700">{{ i.text }}</p>
      <div class="mt-3 text-xs text-gray-500">
        Участников: {{ stateById[i.id]?.count ?? 0 }}{{ i.maxParticipants ? ` / ${i.maxParticipants}` : '' }}
      </div>
      <div v-if="i.date" class="mt-1 text-xs text-gray-500">📅 {{ i.date }}<span v-if="i.time"> • ⏰ {{ i.time }}</span></div>
      <div class="mt-4 flex justify-end">
        <UButton variant="outline" class="rounded-xl bg-white hover:!text-white" @click="openDetails(i.id)">Подробнее</UButton>
      </div>
    </UCard>
  </div>

  <UModal v-model:open="open" title="Книжный клуб" class="z-100">
    <template #body>
      <div v-if="selectedItem" class="space-y-3">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h3 class="text-xl font-semibold text-black">{{ selectedItem.title }}</h3>
            <p class="mt-2 text-sm text-gray-700 whitespace-pre-line">{{ selectedItem.text }}</p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 text-sm text-gray-700">
          <div v-if="selectedItem.date">
            <span class="font-semibold text-black">Дата:</span> {{ selectedItem.date }}
          </div>
          <div v-if="selectedItem.time">
            <span class="font-semibold text-black">Время:</span> {{ selectedItem.time }}
          </div>
          <div v-if="selectedItem.location">
            <span class="font-semibold text-black">Место:</span> {{ selectedItem.location }}
          </div>
          <div v-if="selectedItem.organizer">
            <span class="font-semibold text-black">Организатор:</span> {{ selectedItem.organizer }}
          </div>
        </div>

        <div class="text-sm text-gray-800 mt-2">
          <span class="font-semibold text-black">Участников: </span>
          <span>{{ selectedState?.count ?? 0 }}</span>
          <span v-if="selectedState?.max != null">
            / {{ selectedState.max }}
            <span v-if="isFull && !selectedState.registered" class="ml-2 text-red-500 font-medium">(мест нет)</span>
          </span>
        </div>

        <UAlert v-if="errorMessage" color="error" variant="soft" :description="errorMessage" />
      </div>
    </template>

    <template #footer>
      <div class="flex items-center justify-end gap-3 w-full">
        <UButton
          v-if="selectedState?.registered"
          color="error"
          variant="soft"
          class="rounded-xl"
          :loading="loadingRegistration"
          @click="cancelGoing"
        >
          Я не приду
        </UButton>
        <UButton
          v-else
          color="primary"
          class="rounded-xl"
          :loading="loadingRegistration"
          :disabled="isFull"
          @click="confirmGoing"
        >
          {{ isFull ? 'Мест нет' : 'Подтвердить участие' }}
        </UButton>
      </div>
    </template>
  </UModal>
</template>
