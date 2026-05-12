<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../components/Card.vue';
import { api } from '../../api.js';

const orderedBooks = ref([]);
const loading = ref(true);

onMounted(async () => {
  try {
    orderedBooks.value = await api.get('/orders');
  } catch (e) {
    console.error('Ошибка загрузки заказов:', e);
  } finally {
    loading.value = false;
  }
});

async function handleToggleOrder(bookId) {
  try {
    await api.delete(`/orders/${bookId}`);
    orderedBooks.value = orderedBooks.value.filter(book => book.id !== bookId);
  } catch (e) {
    console.error('Ошибка удаления из заказов:', e);
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-4xl font-bold text-black mb-8">Мои заказы</h1>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <div
        v-else-if="orderedBooks.length > 0"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr"
      >
        <Card
          v-for="book in orderedBooks"
          :id="String(book.id)"
          :key="book.id"
          :title="book.title"
          :genre="book.genre"
          :image-url="book.imageUrl || book.coverUrl"
          :author="book.author"
          :description="book.description"
          :year="book.year"
          :is-ordered="true"
          @toggle-order="handleToggleOrder(book.id)"
        />
      </div>

      <div v-else class="bg-white rounded-2xl p-8 text-center">
        <svg class="w-24 h-24 mx-auto text-black mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
          />
        </svg>
        <h2 class="text-2xl font-semibold text-gray-600 mb-2">Нет заказов</h2>
        <p class="text-gray-500 mb-4">У вас пока нет забронированных книг</p>
        <UButton to="/" color="primary" class="rounded-xl">Перейти в каталог</UButton>
      </div>
    </div>
  </div>
</template>
