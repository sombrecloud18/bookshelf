<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../../api.js';

const stats = ref([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await api.get('/admin/dashboard');
    stats.value = [
      { label: 'Рецензий на модерации', value: data.pendingReviews ?? 0 },
      { label: 'Подборок на модерации', value: data.pendingCollections ?? 0 },
      { label: 'Учебных подборок на модерации', value: data.pendingSubjectCollections ?? 0 },
      { label: 'Предстоящих мероприятий', value: data.upcomingEvents ?? 0 },
      { label: 'Всего пользователей', value: data.totalUsers ?? 0 },
      { label: 'Всего книг', value: data.totalBooks ?? 0 },
    ];
  } catch (e) {
    console.error('Ошибка загрузки статистики:', e);
    stats.value = [
      { label: 'Рецензий на модерации', value: '—' },
      { label: 'Подборок на модерации', value: '—' },
      { label: 'Учебных подборок на модерации', value: '—' },
      { label: 'Предстоящих мероприятий', value: '—' },
      { label: 'Всего пользователей', value: '—' },
      { label: 'Всего книг', value: '—' },
    ];
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-4xl font-bold text-black mb-8">Панель администратора</h1>

      <div v-if="loading" class="flex justify-center py-8">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <template v-else>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <UCard v-for="s in stats" :key="s.label" variant="soft" class="bg-white rounded-2xl p-5">
            <div class="text-sm text-gray-600">{{ s.label }}</div>
            <div class="mt-2 text-3xl font-extrabold text-black">{{ s.value }}</div>
          </UCard>
        </div>

        <div class="mt-8 grid grid-cols-1 md:grid-cols-3 gap-6">
          <UCard variant="soft" class="bg-white rounded-2xl p-5">
            <h2 class="text-lg font-semibold text-black">Рецензии</h2>
            <p class="text-sm text-gray-500 mt-1">Модерация пользовательских рецензий</p>
            <div class="mt-4">
              <UButton to="/admin/reviews" class="rounded-xl w-full" color="primary"> Перейти к рецензиям </UButton>
            </div>
          </UCard>

          <UCard variant="soft" class="bg-white rounded-2xl p-5">
            <h2 class="text-lg font-semibold text-black">Подборки</h2>
            <p class="text-sm text-gray-500 mt-1">Модерация пользовательских подборок</p>
            <div class="mt-4">
              <UButton to="/admin/collections" class="rounded-xl w-full" color="primary"> Перейти к подборкам </UButton>
            </div>
          </UCard>

          <UCard variant="soft" class="bg-white rounded-2xl p-5">
            <h2 class="text-lg font-semibold text-black">Подборки по предметам</h2>
            <p class="text-sm text-gray-500 mt-1">Модерация учебных подборок</p>
            <div class="mt-4">
              <UButton to="/admin/subject-collections" class="rounded-xl w-full" color="primary">
                Перейти к модерации
              </UButton>
            </div>
          </UCard>

          <UCard variant="soft" class="bg-white rounded-2xl p-5">
            <h2 class="text-lg font-semibold text-black">Мероприятия</h2>
            <p class="text-sm text-gray-500 mt-1">Управление мероприятиями</p>
            <div class="mt-4">
              <UButton to="/admin/events" class="rounded-xl w-full bg-white hover:!text-white" variant="outline"> Управление мероприятиями </UButton>
            </div>
          </UCard>
        </div>
      </template>
    </div>
  </div>
</template>
