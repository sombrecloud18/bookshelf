<!-- BookRecommendations.vue -->
<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';

const props = defineProps({
  bookId: { type: String, required: true },
});

const similarBooks = ref([]);
const loading = ref(true);

async function fetchSimilarBooks() {
  try {
    loading.value = true;
    // API запрос к бэкенду
    const response = await fetch(`/api/recommendations/similar/${props.bookId}`);
    const data = await response.json();
    similarBooks.value = data.similar;
  } catch (error) {
    console.error('Ошибка загрузки похожих книг:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchSimilarBooks();
});
</script>

<template>
  <div v-if="similarBooks.length > 0" class="mt-8">
    <h3 class="text-xl font-bold text-black mb-4">Похожие книги</h3>
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <Card
        v-for="book in similarBooks"
        :id="book.bookId"
        :key="book.id"
        :title="book.title"
        :genre="book.genre"
        :author="book.author"
        :image-url="book.imageUrl"
        :description="book.description"
      />
    </div>
  </div>
</template>
