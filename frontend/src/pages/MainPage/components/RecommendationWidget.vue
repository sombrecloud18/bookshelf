<!-- components/RecommendationsWidget.vue -->
<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';

const personalRecommendations = ref([]);
const popularBooks = ref([]);
const newBooks = ref([]);
const loading = ref({
  personal: true,
  popular: true,
  new: true,
});

// Моковые данные для персональных рекомендаций
function getMockPersonalRecommendations() {
  return [
    {
      id: 'rec-1',
      bookId: 'book-master', // ✅ правильный ID из booksMock
      title: 'Мастер и Маргарита',
      genre: 'Роман',
      author: 'Михаил Булгаков',
      imageUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
      description: 'Классический роман о любви и дьяволе.',
      matchScore: 0.95,
    },
    {
      id: 'rec-2',
      bookId: 'book-institute',
      title: 'Институт',
      genre: 'Фантастика',
      author: 'Стивен Кинг',
      imageUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
      description: 'Триллер о детях с необычными способностями.',
      matchScore: 0.88,
    },
    {
      id: 'rec-3',
      bookId: 'book-crows',
      title: 'Шестерка воронов',
      genre: 'Фэнтези',
      author: 'Ли Бардуго',
      imageUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
      description: 'Фэнтези об ограблении века.',
      matchScore: 0.82,
    },
    {
      id: 'rec-4',
      bookId: 'book-hunger-games',
      title: 'Голодные игры',
      genre: 'Фэнтези',
      author: 'Сьюзен Коллинз',
      imageUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
      description: 'Антиутопия о выживании.',
      matchScore: 0.79,
    },
  ];
}

// Моковые данные для популярных книг
function getMockPopularBooks() {
  return [
    {
      id: 'pop-1',
      bookId: 'book-hunger-games',
      title: 'Голодные игры',
      genre: 'Фэнтези',
      author: 'Сьюзен Коллинз',
      imageUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
      description: 'Антиутопия о выживании.',
      views: 15420,
    },
    {
      id: 'pop-2',
      bookId: 'book-satan',
      title: 'Скорбь сатаны',
      genre: 'Роман',
      author: 'Мария Корелли',
      imageUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
      description: 'Мистический роман о сделке с дьяволом.',
      views: 12350,
    },
    {
      id: 'pop-3',
      bookId: 'book-kwebe',
      title: 'Правда о деле Гарри Квеберта',
      genre: 'Детектив',
      author: 'Жоэль Диккер',
      imageUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
      description: 'Захватывающий детектив.',
      views: 9870,
    },
    {
      id: 'pop-4',
      bookId: 'book-ave-maria',
      title: 'Проект "Ave Maria"',
      genre: 'Научная фантастика',
      author: 'Энди Вейер',
      imageUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
      description: 'Научная фантастика о спасении человечества.',
      views: 8760,
    },
  ];
}

// Моковые данные для новинок
function getMockNewBooks() {
  return [
    {
      id: 'new-1',
      bookId: 'book-institute',
      title: 'Институт',
      genre: 'Фантастика',
      author: 'Стивен Кинг',
      imageUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
      description: 'Новый триллер от короля ужасов.',
      addedDate: '2026-04-01',
    },
    {
      id: 'new-2',
      bookId: 'book-crows',
      title: 'Шестерка воронов',
      genre: 'Фэнтези',
      author: 'Ли Бардуго',
      imageUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
      description: 'Популярное фэнтези.',
      addedDate: '2026-03-25',
    },
    {
      id: 'new-3',
      bookId: 'book-master',
      title: 'Мастер и Маргарита',
      genre: 'Роман',
      author: 'Михаил Булгаков',
      imageUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
      description: 'Новое издание классики.',
      addedDate: '2026-03-20',
    },
  ];
}

async function fetchPersonalRecommendations() {
  try {
    loading.value.personal = true;
    // Имитация загрузки
    setTimeout(() => {
      personalRecommendations.value = getMockPersonalRecommendations();
      loading.value.personal = false;
    }, 500);
  } catch (error) {
    console.error('Ошибка загрузки рекомендаций:', error);
    personalRecommendations.value = getMockPersonalRecommendations();
    loading.value.personal = false;
  }
}

async function fetchPopularBooks() {
  try {
    loading.value.popular = true;
    setTimeout(() => {
      popularBooks.value = getMockPopularBooks();
      loading.value.popular = false;
    }, 500);
  } catch (error) {
    console.error('Ошибка загрузки популярных книг:', error);
    popularBooks.value = getMockPopularBooks();
    loading.value.popular = false;
  }
}

async function fetchNewBooks() {
  try {
    loading.value.new = true;
    setTimeout(() => {
      newBooks.value = getMockNewBooks();
      loading.value.new = false;
    }, 500);
  } catch (error) {
    console.error('Ошибка загрузки новинок:', error);
    newBooks.value = getMockNewBooks();
    loading.value.new = false;
  }
}

onMounted(() => {
  fetchPersonalRecommendations();
  fetchPopularBooks();
  fetchNewBooks();
});
</script>

<template>
  <div class="space-y-10">
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
            :id="rec.bookId"
            :title="rec.title"
            :genre="rec.genre"
            :author="rec.author"
            :description="rec.description"
            :image-url="rec.imageUrl"
          />
          <div
            class="absolute -top-2 -left-2 bg-linear-to-r from-yellow-400 to-orange-500 text-black text-xs font-bold px-2 py-1 rounded-full shadow-lg"
          >
            {{ Math.round(rec.matchScore * 100) }}% совпадение
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loading.personal" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>

    <!-- Пустое состояние -->
    <div
      v-if="
        personalRecommendations.length === 0 && popularBooks.length === 0 && newBooks.length === 0 && !loading.personal
      "
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
  </div>
</template>
