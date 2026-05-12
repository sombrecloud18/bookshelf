<!-- BookRecommendations.vue -->
<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';
import { api } from '../../../api.js';

const props = defineProps({
  bookId: { type: String, required: true },
});

const similarBooks = ref([]);
const loading = ref(true);

// Состояние «в заказах» книги. Без него кнопка-звёздочка в Card.vue ни на что
// не реагирует: компонент эмитит `toggle-order`, а раньше родитель этот
// сигнал просто игнорировал.
const orderedBooks = ref(new Set());

async function fetchSimilarBooks() {
  try {
    loading.value = true;
    const data = await api.get(`/recommendations/similar/${props.bookId}`);
    similarBooks.value = data || [];
  } catch (error) {
    console.error('Ошибка загрузки похожих книг:', error);
    similarBooks.value = [];
  } finally {
    loading.value = false;
  }
}

async function fetchOrders() {
  try {
    const orders = await api.get('/orders');
    orderedBooks.value = new Set(orders.map(b => b.id));
  } catch {
    // not logged in or no orders — ignore
  }
}

function isOrdered(bookId) {
  return orderedBooks.value.has(bookId);
}

async function toggleOrder(bookId) {
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

onMounted(() => {
  fetchSimilarBooks();
  fetchOrders();
});
</script>

<template>
  <div v-if="similarBooks.length > 0" class="mt-8">
    <h3 class="text-xl font-bold text-black mb-4">Похожие книги</h3>
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <Card
        v-for="book in similarBooks"
        :id="String(book.bookId)"
        :key="book.id"
        :title="book.title"
        :genre="book.genre"
        :author="book.author"
        :image-url="book.imageUrl"
        :description="book.description"
        :is-ordered="isOrdered(book.bookId)"
        @toggle-order="toggleOrder(book.bookId)"
      />
    </div>
  </div>
</template>
