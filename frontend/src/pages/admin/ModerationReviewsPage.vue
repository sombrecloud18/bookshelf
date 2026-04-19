<script setup>
import { ref } from 'vue';
import { getGenreColor } from '../../constants/genreColors';

const reviewsQueue = ref([
  {
    id: 'rev-pending-1',
    bookId: 'book-satan',
    bookTitle: 'Скорбь сатаны',
    author: 'Мария Корелли',
    genre: 'Роман',
    coverUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
    rating: 5,
    text: 'В центре повествования — талантливый, но бедный и гордый писатель. Роман читается легко, при этом оставляет сильное послевкусие и заставляет задуматься о выборе и ответственности.\n\nОсобенно понравилась атмосфера и то, как автор раскрывает характеры. Некоторые моменты кажутся наивными, но в целом это не мешает цельности истории.',
    reviewAuthor: 'student1',
    reviewAuthorName: 'Анна Смирнова',
    createdAt: '2026-04-10',
    status: 'pending',
  },
  {
    id: 'rev-pending-2',
    bookId: 'book-hunger-games',
    bookTitle: 'Голодные игры',
    author: 'Сьюзен Коллинз',
    genre: 'Фэнтези',
    coverUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
    rating: 4,
    text: 'Захватывающий сюжет и сильная главная героиня. Книга держит в напряжении до последней страницы. Рекомендую всем любителям антиутопий. Особенно понравилось описание Игр и то, как автор показывает психологию участников.',
    reviewAuthor: 'student3',
    reviewAuthorName: 'Елена Морозова',
    createdAt: '2026-04-11',
    status: 'pending',
  },
]);

const selectedReview = ref(null);
const showDetailsModal = ref(false);

function viewDetails(review) {
  selectedReview.value = review;
  showDetailsModal.value = true;
}

function approveReview(id) {
  const index = reviewsQueue.value.findIndex(r => r.id === id);
  if (index !== -1) {
    console.log('Одобрена рецензия:', reviewsQueue.value[index].bookTitle);
    reviewsQueue.value.splice(index, 1);
  }
  showDetailsModal.value = false;
}

function rejectReview(id) {
  const review = reviewsQueue.value.find(r => r.id === id);
  if (confirm(`Отклонить рецензию на книгу «${review?.bookTitle}»?`)) {
    const index = reviewsQueue.value.findIndex(r => r.id === id);
    if (index !== -1) {
      console.log('Отклонена рецензия:', reviewsQueue.value[index].bookTitle);
      reviewsQueue.value.splice(index, 1);
    }
    showDetailsModal.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-4xl font-bold text-black">Модерация рецензий</h1>
          <p class="text-gray-700 mt-2">На проверке: {{ reviewsQueue.length }} рецензий</p>
        </div>
        <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
      </div>

      <UCard variant="soft" class="bg-white rounded-2xl p-5">
        <div v-if="reviewsQueue.length === 0" class="text-center py-12 text-gray-500">
          <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
              d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
            />
          </svg>
          <p class="text-lg">Нет рецензий на модерации</p>
          <p class="text-sm mt-1">Все рецензии проверены</p>
        </div>

        <div v-else class="space-y-4">
          <UCard
            v-for="review in reviewsQueue"
            :key="review.id"
            variant="soft"
            class="bg-white rounded-xl border border-gray-200 hover:shadow-md transition-shadow"
          >
            <div class="flex flex-col md:flex-row gap-4">
              <div class="flex-shrink-0">
                <img
                  class="w-24 h-36 object-cover rounded-lg shadow-md"
                  :src="review.coverUrl"
                  :alt="review.bookTitle"
                />
              </div>

              <div class="flex-1">
                <div class="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <div class="flex items-center gap-2 flex-wrap">
                      <h2 class="text-xl font-bold text-black">{{ review.bookTitle }}</h2>
                      <span
                        class="inline-flex px-2 py-0.5 text-xs font-medium rounded-full"
                        :class="getGenreColor(review.genre)"
                      >
                        {{ review.genre }}
                      </span>
                    </div>
                    <p class="text-sm text-gray-600">{{ review.author }}</p>
                  </div>
                  <div class="text-right">
                    <div class="flex items-center gap-1">
                      <span class="text-yellow-500">★</span>
                      <span class="font-semibold">{{ review.rating }}</span>
                      <span class="text-gray-400 text-sm">/5</span>
                    </div>
                    <p class="text-xs text-gray-400 mt-1">{{ review.createdAt }}</p>
                  </div>
                </div>

                <p class="mt-3 text-sm text-gray-700 line-clamp-3">{{ review.text }}</p>

                <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
                  <p class="text-xs text-gray-500">
                    Автор рецензии:
                    <span class="font-medium">{{ review.reviewAuthorName || review.reviewAuthor }}</span>
                  </p>
                  <div class="flex gap-2">
                    <UButton size="sm" color="primary" variant="soft" class="rounded-xl" @click="viewDetails(review)">
                      Подробнее
                    </UButton>
                    <UButton size="sm" color="green" class="rounded-xl" @click="approveReview(review.id)">
                      Одобрить
                    </UButton>
                    <UButton size="sm" color="red" variant="soft" class="rounded-xl" @click="rejectReview(review.id)">
                      Отклонить
                    </UButton>
                  </div>
                </div>
              </div>
            </div>
          </UCard>
        </div>
      </UCard>

      <!-- Модальное окно с подробностями рецензии -->
      <UModal v-model="showDetailsModal" class="z-100">
        <template #body>
          <div v-if="selectedReview" class="space-y-4">
            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <div class="flex gap-4">
                <img
                  class="w-28 h-40 object-cover rounded-lg shadow-md flex-shrink-0"
                  :src="selectedReview.coverUrl"
                  :alt="selectedReview.bookTitle"
                />
                <div class="flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h2 class="text-2xl font-bold text-black">{{ selectedReview.bookTitle }}</h2>
                    <span
                      class="inline-flex px-2 py-0.5 text-xs font-medium rounded-full"
                      :class="getGenreColor(selectedReview.genre)"
                    >
                      {{ selectedReview.genre }}
                    </span>
                  </div>
                  <p class="text-md text-gray-700 mt-1">{{ selectedReview.author }}</p>

                  <div class="mt-3 flex items-center gap-4 flex-wrap">
                    <div class="flex items-center gap-1">
                      <span class="text-yellow-500 text-xl">★</span>
                      <span class="text-2xl font-bold">{{ selectedReview.rating }}</span>
                      <span class="text-gray-400">/5</span>
                    </div>
                    <div class="text-sm text-gray-500">
                      <span class="font-semibold">Автор рецензии:</span>
                      {{ selectedReview.reviewAuthorName || selectedReview.reviewAuthor }}
                    </div>
                    <div class="text-sm text-gray-500">
                      <span class="font-semibold">Дата:</span> {{ selectedReview.createdAt }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <h3 class="font-semibold text-black mb-3">Текст рецензии</h3>
              <div class="bg-gray-50 p-4 rounded-lg">
                <p class="text-gray-700 whitespace-pre-wrap leading-relaxed">{{ selectedReview.text }}</p>
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
    </div>
  </div>
</template>
