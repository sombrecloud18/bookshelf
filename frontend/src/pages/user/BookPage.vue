<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getGenreColor } from '../../constants/genreColors';
import BookRecommentations from './components/BookRecommentations.vue';
import LikeButton from '../../components/LikeButton.vue';
import { api } from '../../api.js';

const route = useRoute();
const router = useRouter();

const book = ref(null);
const reviews = ref([]);
const loading = ref(true);
const coverFailed = ref(false);
const newReview = ref({ rating: 0, text: '' });
const showReviewForm = ref(false);
const reviewSubmitted = ref(false);
const reviewError = ref(null);

const showCommentFormForReview = ref(null);
const newCommentText = ref('');

function formatDate(dateStr) {
  if (!dateStr) return '';
  return String(dateStr).split('T')[0];
}

onMounted(async () => {
  const bookId = route.params.id;
  try {
    const [bookData, reviewsData] = await Promise.all([
      api.get(`/books/${bookId}`),
      api.get(`/reviews/book/${bookId}?size=20`),
    ]);
    book.value = bookData;
    reviews.value = reviewsData.content || [];
  } catch (e) {
    console.error('Ошибка загрузки:', e);
    router.push('/404');
    return;
  } finally {
    loading.value = false;
  }
});

function setRating(value) {
  newReview.value.rating = value;
}

async function submitReview() {
  reviewError.value = null;
  if (newReview.value.rating === 0) {
    reviewError.value = 'Пожалуйста, поставьте оценку';
    return;
  }
  if (!newReview.value.text.trim()) {
    reviewError.value = 'Пожалуйста, напишите рецензию';
    return;
  }
  if (newReview.value.text.trim().length < 200) {
    reviewError.value = 'Рецензия должна содержать не менее 200 символов';
    return;
  }

  try {
    await api.post('/reviews', {
      bookId: book.value.id,
      rating: newReview.value.rating,
      text: newReview.value.text.trim(),
    });
    newReview.value = { rating: 0, text: '' };
    showReviewForm.value = false;
    reviewSubmitted.value = true;
  } catch (e) {
    reviewError.value = e.message || 'Ошибка при отправке рецензии';
  }
}

function showCommentForm(reviewId) {
  showCommentFormForReview.value = reviewId;
  newCommentText.value = '';
}

function hideCommentForm() {
  showCommentFormForReview.value = null;
  newCommentText.value = '';
}

async function submitComment(reviewId) {
  if (!newCommentText.value.trim()) return;
  try {
    const comment = await api.post(`/comments/review/${reviewId}`, { text: newCommentText.value.trim() });
    const review = reviews.value.find(r => r.id === reviewId);
    if (review) {
      if (!review.comments) review.comments = [];
      review.comments.unshift(comment);
    }
    hideCommentForm();
  } catch (e) {
    console.error('Ошибка отправки комментария:', e);
  }
}

const averageRating = computed(() => {
  if (book.value?.averageRating) return Number(book.value.averageRating).toFixed(1);
  if (reviews.value.length === 0) return 0;
  const sum = reviews.value.reduce((acc, r) => acc + r.rating, 0);
  return (sum / reviews.value.length).toFixed(1);
});

function reserve() {
  window.open('https://library.bsuir.by/', '_blank', 'noopener,noreferrer');
}
</script>

<template>
  <div v-if="loading" class="flex justify-center items-center min-h-100">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
      <p class="mt-4 text-gray-600">Загрузка...</p>
    </div>
  </div>

  <div v-else-if="book" class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <!-- Кнопка назад -->
      <button
        class="mb-6 flex items-center gap-2 text-black hover:text-black transition-colors"
        @click="router.back()"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
        Назад
      </button>

      <!-- Информация о книге -->
      <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-8">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 p-6 md:p-8">
          <!-- Обложка -->
          <div class="flex justify-center md:justify-center">
            <img
              v-if="(book.coverUrl || book.imageUrl) && !coverFailed"
              class="w-full rounded-2xl shadow-lg object-cover"
              :src="book.coverUrl || book.imageUrl"
              :alt="book.title"
              @error="coverFailed = true"
            />
            <div
              v-else
              class="w-full aspect-2/3 rounded-2xl shadow-lg bg-gray-100 flex items-center justify-center text-gray-400"
            >
              Нет обложки
            </div>
          </div>

          <!-- Детали книги -->
          <div class="md:col-span-2">
            <div class="flex items-center gap-3 flex-wrap mb-3">
              <span class="inline-flex px-4 py-1 text-sm font-medium rounded-full" :class="getGenreColor(book.genre)">
                {{ book.genre }}
              </span>
              <span class="text-sm text-gray-500">{{ book.year }}</span>
            </div>

            <h1 class="text-3xl md:text-4xl font-bold text-black mb-2">{{ book.title }}</h1>
            <p class="text-xl text-gray-700 mb-4">{{ book.author }}</p>

            <div class="flex items-center gap-4 mb-6">
              <div class="flex items-center gap-1">
                <span class="text-yellow-500 text-2xl">★</span>
                <span v-if="reviews.length == 0 && !book.averageRating">Нет оценок</span>
                <div v-else class="text-gray-700">
                  <span class="text-2xl font-bold text-gray-700">{{ averageRating }}</span>
                  <span class="text-gray-700">/5</span>
                </div>
              </div>
              <span class="text-gray-500">•</span>
              <span class="text-gray-600"
                >{{ reviews.length }} {{ reviews.length === 1 ? 'рецензия' : 'рецензий' }}</span
              >
            </div>

            <div class="prose prose-lg max-w-none mb-6">
              <h3 class="text-lg font-semibold text-black mb-2">О книге</h3>
              <p class="text-gray-700 whitespace-pre-line">{{ book.fullDescription || book.description }}</p>
            </div>

            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-gray-200">
              <div>
                <p class="text-xs text-gray-700">Издательство</p>
                <p class="text-sm font-medium text-gray-700">{{ book.publisher || 'Не указано' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-700">Год издания</p>
                <p class="text-sm font-medium text-gray-700">{{ book.publishYear || book.year }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-700">Страниц</p>
                <p class="text-sm font-medium text-gray-700">{{ book.pages || 'Не указано' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-700">ISBN</p>
                <p class="text-sm font-medium text-gray-700">{{ book.isbn || 'Не указан' }}</p>
              </div>
            </div>

            <div class="mt-6 flex gap-3">
              <UButton color="primary" class="rounded-xl" @click="reserve"> Забронировать </UButton>
            </div>
          </div>
        </div>
      </div>

      <!-- Рецензии -->
      <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-8">
        <div class="p-6 md:p-8">
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-black">Рецензии</h2>
            <UButton color="primary" class="rounded-xl" @click="showReviewForm = !showReviewForm; reviewSubmitted = false; reviewError = null">
              {{ showReviewForm ? 'Отмена' : 'Написать рецензию' }}
            </UButton>
          </div>

          <!-- Сообщение об отправке на модерацию -->
          <UAlert
            v-if="reviewSubmitted"
            color="success"
            variant="soft"
            description="Ваша рецензия отправлена на модерацию и появится после проверки."
            class="mb-6"
          />

          <!-- Форма добавления рецензии -->
          <div v-if="showReviewForm" class="mb-8 p-5 bg-gray-50 rounded-2xl">
            <h3 class="text-lg font-semibold text-black mb-4">Ваша рецензия</h3>

            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">Оценка</label>
              <div class="flex items-center gap-1">
                <button
                  v-for="i in 5"
                  :key="i"
                  type="button"
                  class="transition-transform hover:scale-105"
                  @click="setRating(i)"
                >
                  <svg
                    viewBox="0 0 24 24"
                    class="w-10 h-10"
                    :class="i <= newReview.rating ? 'text-yellow-300' : 'text-gray-200'"
                    fill="currentColor"
                  >
                    <path
                      d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Текст рецензии
                <span class="text-gray-400">(минимум 200 символов)</span>
              </label>
              <UTextarea
                v-model="newReview.text"
                :rows="6"
                placeholder="Поделитесь впечатлениями о книге..."
                class="w-full"
              />
              <p class="text-xs text-gray-400 mt-1">{{ newReview.text.length }} / 200</p>
            </div>

            <UAlert v-if="reviewError" color="error" variant="soft" :description="reviewError" class="mb-4" />

            <div class="flex justify-end">
              <UButton color="success" variant="soft" class="rounded-xl" @click="submitReview"> Опубликовать рецензию </UButton>
            </div>
          </div>

          <!-- Список рецензий -->
          <div v-if="reviews.length > 0" class="space-y-6">
            <div v-for="review in reviews" :key="review.id" class="border-b border-gray-200 pb-6 last:border-0">
              <!-- Информация о рецензии -->
              <div class="flex justify-between items-start mb-3">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center">
                    <span class="text-gray-600 font-semibold">
                      {{ (review.userName || '?').charAt(0).toUpperCase() }}
                    </span>
                  </div>
                  <div>
                    <p class="font-semibold text-black">{{ review.userName }}</p>
                    <p class="text-xs text-gray-500">{{ formatDate(review.createdAt) }}</p>
                  </div>
                </div>
                <div class="flex items-center gap-1">
                  <span class="text-yellow-500">★</span>
                  <span class="font-semibold">{{ review.rating }}</span>
                </div>
              </div>

              <p class="text-gray-700 whitespace-pre-wrap break-words ml-12" style="overflow-wrap: anywhere">{{ review.text }}</p>

              <!-- Кнопки действий с рецензией -->
              <div class="ml-12 mt-3 flex items-center gap-4">
                <LikeButton
                  target-type="REVIEW"
                  :target-id="String(review.id)"
                  :initial-count="Number(review.likes || 0)"
                  :initial-liked="!!review.liked"
                />

                <button
                  class="flex items-center gap-1 text-sm text-black hover:text-blue-500 transition-colors"
                  @click="showCommentForm(review.id)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                    />
                  </svg>
                  <span>{{ review.comments?.length || 0 }} комментариев</span>
                </button>
              </div>

              <!-- Форма добавления комментария -->
              <div v-if="showCommentFormForReview === review.id" class="ml-12 mt-4">
                <div class="flex gap-3">
                  <div class="flex-1">
                    <UTextarea
                      v-model="newCommentText"
                      :rows="3"
                      placeholder="Написать комментарий..."
                      class="w-full"
                    />
                  </div>
                </div>
                <div class="flex justify-end gap-2 mt-2">
                  <UButton size="sm" variant="outline" class="rounded-xl bg-white hover:!text-white" @click="hideCommentForm"> Отмена </UButton>
                  <UButton size="sm" color="primary" class="rounded-xl" @click="submitComment(review.id)">
                    Отправить
                  </UButton>
                </div>
              </div>

              <!-- Список комментариев к рецензии -->
              <div v-if="review.comments && review.comments.length > 0" class="ml-12 mt-4 space-y-3">
                <div v-for="comment in review.comments" :key="comment.id" class="bg-gray-50 rounded-xl p-3">
                  <div class="flex justify-between items-start mb-2">
                    <div class="flex items-center gap-2">
                      <div class="w-6 h-6 bg-gray-400 rounded-full flex items-center justify-center">
                        <span class="text-white text-xs font-semibold">
                          {{ (comment.userName || '?').charAt(0).toUpperCase() }}
                        </span>
                      </div>
                      <div>
                        <p class="font-semibold text-black text-sm">{{ comment.userName }}</p>
                        <p class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</p>
                      </div>
                    </div>
                  </div>
                  <p class="text-gray-700 text-sm ml-8 break-words" style="overflow-wrap: anywhere">{{ comment.text }}</p>
                  <div class="ml-8 mt-2">
                    <LikeButton
                      target-type="COMMENT"
                      :target-id="String(comment.id)"
                      :initial-count="Number(comment.likes || 0)"
                      :initial-liked="!!comment.liked"
                      size="xs"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="text-center py-8 text-gray-500">
            <p>Пока нет рецензий. Будьте первым!</p>
          </div>
        </div>
      </div>

      <!-- Похожие книги -->
      <BookRecommentations :book-id="String(book.id)" />
    </div>
  </div>
</template>
