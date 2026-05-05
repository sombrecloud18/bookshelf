<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';
import { api } from '../../../api.js';

const props = defineProps({
  books: { type: Array, default: () => [] },
});

const orderedBooks = ref(new Set());

onMounted(async () => {
  try {
    const orders = await api.get('/orders');
    orderedBooks.value = new Set(orders.map(b => b.id));
  } catch {
    // user may not be logged in
  }
});

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

function isOrdered(bookId) {
  return orderedBooks.value.has(bookId);
}
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr">
    <Card
      v-for="b in books"
      :id="String(b.id)"
      :key="b.id"
      :title="b.title"
      :genre="b.genre"
      :author="b.author"
      :year="b.year"
      :description="b.description"
      :image-url="b.imageUrl || b.coverUrl"
      :is-ordered="isOrdered(b.id)"
      @toggle-order="toggleOrder(b.id)"
    />
  </div>
</template>
