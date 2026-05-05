<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';
import { api } from '../../../api.js';

const personalRecommendations = ref([]);
const popularBooks = ref([]);
const newBooks = ref([]);
const loading = ref(true);
const orderedBooks = ref(new Set());

async function loadOrders() {
  try {
    const orders = await api.get('/orders');
    orderedBooks.value = new Set((orders || []).map(b => b.id));
  } catch {
    // anonymous viewer is OK
  }
}

onMounted(async () => {
  try {
    const [data, _] = await Promise.all([api.get('/recommendations'), loadOrders()]);
    personalRecommendations.value = data.personal || [];
    popularBooks.value = data.popular || [];
    newBooks.value = data.newBooks || [];
  } catch (error) {
    console.error('Ошибка загрузки рекомендаций:', error);
  } finally {
    loading.value = false;
  }
});

async function toggleOrder(bookId) {
  if (!bookId) return;
  if (orderedBooks.value.has(bookId)) {
    try {
      await api.delete(`/orders/${bookId}`);
      orderedBooks.value = new Set([...orderedBooks.value].filter(id => id !== bookId));
    } catch (e) {
      console.error('Ошибка удаления из заказов:', e);
    }
  } else {
    try {
      await api.post('/orders', { bookId });
      orderedBooks.value = new Set([...orderedBooks.value, bookId]);
    } catch (e) {
      console.error('Ошибка добавления в заказы:', e);
    }
  }
}

function isOrdered(bookId) {
  return orderedBooks.value.has(bookId);
}
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
              :is-ordered="isOrdered(rec.bookId)"
              @toggle-order="toggleOrder(rec.bookId)"
            />
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
            :is-ordered="isOrdered(rec.bookId)"
            @toggle-order="toggleOrder(rec.bookId)"
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
            :is-ordered="isOrdered(rec.bookId)"
            @toggle-order="toggleOrder(rec.bookId)"
          />
        </div>
      </div>

      <!-- Пустое состояние -->
      <div
        v-if="personalRecommendations.length === 0 && popularBooks.length === 0 && newBooks.length === 0"
        class="text-center py-12 bg-white rounded-3xl"
      >
        <h3 class="text-xl font-semibold text-gray-600 mb-2">Нет рекомендаций</h3>
        <p class="text-gray-500">Оцените больше книг, чтобы получать персонализированные рекомендации</p>
        <UButton to="/" color="primary" class="rounded-xl mt-4">Перейти в каталог</UButton>
      </div>
    </template>
  </div>
</template>
