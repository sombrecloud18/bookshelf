<!-- components/RecommendationsWidget.vue -->
<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';
import { api } from '../../../api.js';

const personalRecommendations = ref([]);
const popularBooks = ref([]);
const newBooks = ref([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await api.get('/recommendations');
    personalRecommendations.value = data.personal || [];
    popularBooks.value = data.popular || [];
    newBooks.value = data.newBooks || [];
  } catch (error) {
    console.error('Ошибка загрузки рекомендаций:', error);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="space-y-10">
    <div v-if="loading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>

    <template v-else>
      <!-- Персональные рекомендации -->
      <div v-if="personalRecommendations.length > 0">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="text-2xl font-bold text-black">Рекомендации для вас</h2>
            <p class="text-sm text-gray-600 mt-1">Подобрано на основе ваших предпочтений</p>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <div v-for="rec in personalRecommendations" :key="rec.id" class="relative">
            <Card
              :id="String(rec.bookId)"
              :title="rec.title"
              :genre="rec.genre"
              :author="rec.author"
              :description="rec.description"
              :image-url="rec.imageUrl"
            />
            <div
              v-if="rec.matchScore"
              class="absolute -top-2 -left-2 bg-linear-to-r from-yellow-400 to-orange-500 text-black text-xs font-bold px-2 py-1 rounded-full shadow-lg"
            >
              {{ Math.round(rec.matchScore * 100) }}% совпадение
            </div>
          </div>
        </div>
      </div>

      <!-- Популярные книги -->
      <div v-if="popularBooks.length > 0">
        <h2 class="text-2xl font-bold text-black mb-4">Популярное</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <Card
            v-for="rec in popularBooks"
            :id="String(rec.bookId)"
            :key="rec.id"
            :title="rec.title"
            :genre="rec.genre"
            :author="rec.author"
            :description="rec.description"
            :image-url="rec.imageUrl"
          />
        </div>
      </div>

      <!-- Новинки -->
      <div v-if="newBooks.length > 0">
        <h2 class="text-2xl font-bold text-black mb-4">Новинки</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <Card
            v-for="rec in newBooks"
            :id="String(rec.bookId)"
            :key="rec.id"
            :title="rec.title"
            :genre="rec.genre"
            :author="rec.author"
            :description="rec.description"
            :image-url="rec.imageUrl"
          />
        </div>
      </div>

      <!-- Пустое состояние -->
      <div
        v-if="personalRecommendations.length === 0 && popularBooks.length === 0 && newBooks.length === 0"
        class="text-center py-12 bg-white rounded-3xl"
      >
        <svg class="w-24 h-24 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
          />
        </svg>
        <h3 class="text-xl font-semibold text-gray-600 mb-2">Нет рекомендаций</h3>
        <p class="text-gray-500">Оцените больше книг, чтобы получать персонализированные рекомендации</p>
        <UButton to="/" color="primary" class="rounded-xl mt-4">Перейти в каталог</UButton>
      </div>
    </template>
  </div>
</template>
