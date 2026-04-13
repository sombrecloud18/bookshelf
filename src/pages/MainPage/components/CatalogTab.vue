<script setup>
import { computed, ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';

const props = defineProps({
  query: { type: String, default: '' },
  books: { type: Array, default: () => [] },
});

// Хранилище заказов (ID книг, которые добавлены в заказы)
const orderedBooks = ref(new Set());

// Загрузка заказов из localStorage при монтировании
onMounted(() => {
  const savedOrders = localStorage.getItem('userOrders');
  if (savedOrders) {
    const orders = JSON.parse(savedOrders);
    orderedBooks.value = new Set(orders);
  }
});

// Сохранение заказов в localStorage
function saveOrders() {
  localStorage.setItem('userOrders', JSON.stringify([...orderedBooks.value]));
}

// Добавление/удаление книги из заказов
function toggleOrder(bookId) {
  if (orderedBooks.value.has(bookId)) {
    orderedBooks.value.delete(bookId);
  } else {
    orderedBooks.value.add(bookId);
  }
  saveOrders();
}

// Проверка, добавлена ли книга в заказы
function isOrdered(bookId) {
  return orderedBooks.value.has(bookId);
}

const filteredBooks = computed(() => {
  const q = (props.query || '').trim().toLowerCase();
  if (!q) return props.books;
  return props.books.filter(
    b =>
      (b.title || '').toLowerCase().includes(q) ||
      (b.author || '').toLowerCase().includes(q) ||
      (b.genre || '').toLowerCase().includes(q),
  );
});
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr">
    <Card
      v-for="b in filteredBooks"
      :id="b.id"
      :key="b.id"
      :title="b.title"
      :genre="b.genre"
      :author="b.author"
      :year="b.year"
      :description="b.description"
      :image-url="b.imageUrl"
      :is-ordered="isOrdered(b.id)"
      @toggle-order="toggleOrder(b.id)"
    />
  </div>
</template>
