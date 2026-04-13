<script setup>
import { ref } from 'vue';

const events = ref([
  {
    id: 'e-1',
    date: '2026-04-20',
    title: 'Встреча книжного клуба',
    description: 'Обсуждаем классику и делимся впечатлениями.',
    location: 'Библиотека №1, ул. Центральная 15',
    time: '18:00',
    maxParticipants: 20,
    currentParticipants: 8,
    organizer: 'Анна Смирнова',
  },
  {
    id: 'e-2',
    date: '2026-04-25',
    title: 'Лекция "Современная литература"',
    description: 'Лекция о современных тенденциях в литературе и новых именах.',
    location: 'Онлайн (Zoom)',
    time: '19:00',
    maxParticipants: 50,
    currentParticipants: 23,
    organizer: 'Михаил Петров',
  },
]);

const form = ref({
  date: '',
  title: '',
  description: '',
  location: '',
  time: '',
  maxParticipants: '',
});

const selectedEvent = ref(null);
const showDetailsModal = ref(false);
const showEditModal = ref(false);

function viewDetails(event) {
  selectedEvent.value = { ...event };
  showDetailsModal.value = true;
}

function openEdit(event) {
  selectedEvent.value = { ...event };
  showEditModal.value = true;
}

function saveEdit() {
  if (!selectedEvent.value) return;
  if (!selectedEvent.value.title.trim()) return;

  const index = events.value.findIndex(e => e.id === selectedEvent.value.id);
  if (index !== -1) {
    events.value[index] = { ...selectedEvent.value };
  }
  showEditModal.value = false;
}

function addEvent() {
  if (!form.value.date || !form.value.title.trim()) return;
  events.value.unshift({
    id: `e-${Math.random().toString(36).slice(2, 9)}`,
    date: form.value.date,
    title: form.value.title.trim(),
    description: form.value.description.trim(),
    location: form.value.location.trim() || 'Не указано',
    time: form.value.time.trim() || 'Не указано',
    maxParticipants: form.value.maxParticipants ? Number(form.value.maxParticipants) : null,
    currentParticipants: 0,
    organizer: 'Администратор',
  });
  form.value = { date: '', title: '', description: '', location: '', time: '', maxParticipants: '' };
}

function removeEvent(id) {
  const event = events.value.find(e => e.id === id);
  if (confirm(`Удалить мероприятие «${event?.title}»?`)) {
    events.value = events.value.filter(e => e.id !== id);
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <h1 class="text-4xl font-bold text-black">Управление мероприятиями</h1>
        <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
      </div>

      <UCard variant="soft" class="bg-white rounded-2xl p-5">
        <h2 class="text-lg font-semibold text-black">Создать мероприятие</h2>
        <div class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <UFormField label="Дата">
            <UInput v-model="form.date" type="date" />
          </UFormField>
          <UFormField label="Время">
            <UInput v-model="form.time" type="time" placeholder="18:00" />
          </UFormField>
          <UFormField label="Название">
            <UInput v-model="form.title" placeholder="Название мероприятия" />
          </UFormField>
          <UFormField label="Локация">
            <UInput v-model="form.location" placeholder="Место проведения" />
          </UFormField>
          <UFormField label="Максимум участников">
            <UInput v-model="form.maxParticipants" type="number" placeholder="Не ограничено" />
          </UFormField>
          <UFormField label="Описание" class="md:col-span-2">
            <UTextarea v-model="form.description" :rows="3" placeholder="Краткое описание мероприятия" />
          </UFormField>
        </div>
        <div class="mt-4 flex justify-end">
          <UButton color="primary" class="rounded-xl" @click="addEvent">Добавить мероприятие</UButton>
        </div>
      </UCard>

      <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
        <UCard
          v-for="e in events"
          :key="e.id"
          variant="soft"
          class="bg-white rounded-2xl p-5 hover:shadow-xl transition-all duration-300"
        >
          <div class="flex justify-between items-start">
            <div>
              <div class="flex items-center gap-2 text-xs text-gray-500">
                <span>📅 {{ e.date }}</span>
                <span>⏰ {{ e.time }}</span>
              </div>
              <h3 class="mt-2 text-lg font-semibold text-black">{{ e.title }}</h3>
              <p class="mt-2 text-sm text-gray-700 line-clamp-2">{{ e.description }}</p>
              <div class="mt-2 flex items-center gap-3 text-xs text-gray-500">
                <span>📍 {{ e.location }}</span>
                <span v-if="e.maxParticipants">👥 {{ e.currentParticipants || 0 }}/{{ e.maxParticipants }}</span>
              </div>
            </div>
          </div>
          <div class="mt-4 flex justify-end gap-2">
            <UButton size="xs" variant="soft" class="rounded-xl" @click="viewDetails(e)"> Подробнее </UButton>
            <UButton size="xs" variant="soft" color="primary" class="rounded-xl" @click="openEdit(e)">
              Редактировать
            </UButton>
            <UButton size="xs" color="red" variant="soft" class="rounded-xl" @click="removeEvent(e.id)">
              Удалить
            </UButton>
          </div>
        </UCard>
      </div>

      <!-- Модальное окно просмотра мероприятия -->
      <UModal v-model="showDetailsModal" class="z-100">
        <template #body>
          <div v-if="selectedEvent" class="space-y-4">
            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <h2 class="text-2xl font-bold text-black">{{ selectedEvent.title }}</h2>
              <div class="mt-4 space-y-2">
                <p class="text-sm"><span class="font-semibold">📅 Дата:</span> {{ selectedEvent.date }}</p>
                <p class="text-sm"><span class="font-semibold">⏰ Время:</span> {{ selectedEvent.time }}</p>
                <p class="text-sm"><span class="font-semibold">📍 Место:</span> {{ selectedEvent.location }}</p>
                <p class="text-sm">
                  <span class="font-semibold">👥 Участники:</span> {{ selectedEvent.currentParticipants || 0
                  }}{{ selectedEvent.maxParticipants ? ` / ${selectedEvent.maxParticipants}` : '' }}
                </p>
                <p class="text-sm"><span class="font-semibold">👤 Организатор:</span> {{ selectedEvent.organizer }}</p>
                <div class="mt-3 pt-3 border-t border-gray-200">
                  <p class="font-semibold text-sm">Описание:</p>
                  <p class="text-sm text-gray-700 mt-1">{{ selectedEvent.description }}</p>
                </div>
              </div>
            </div>
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-3 w-full">
            <UButton variant="outline" @click="showDetailsModal = false">Закрыть</UButton>
          </div>
        </template>
      </UModal>

      <!-- Модальное окно редактирования мероприятия -->
      <UModal v-model="showEditModal" class="z-100">
        <template #body>
          <div v-if="selectedEvent" class="space-y-4">
            <h2 class="text-xl font-bold text-black mb-4">Редактировать мероприятие</h2>
            <div class="grid grid-cols-1 gap-4">
              <UFormField label="Название">
                <UInput v-model="selectedEvent.title" />
              </UFormField>
              <UFormField label="Дата">
                <UInput v-model="selectedEvent.date" type="date" />
              </UFormField>
              <UFormField label="Время">
                <UInput v-model="selectedEvent.time" type="time" />
              </UFormField>
              <UFormField label="Локация">
                <UInput v-model="selectedEvent.location" />
              </UFormField>
              <UFormField label="Максимум участников">
                <UInput v-model="selectedEvent.maxParticipants" type="number" />
              </UFormField>
              <UFormField label="Описание">
                <UTextarea v-model="selectedEvent.description" :rows="4" />
              </UFormField>
            </div>
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-3 w-full">
            <UButton variant="outline" @click="showEditModal = false">Отмена</UButton>
            <UButton color="primary" @click="saveEdit">Сохранить</UButton>
          </div>
        </template>
      </UModal>
    </div>
  </div>
</template>
