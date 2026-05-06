<script setup>
import { ref, onMounted } from 'vue';
import { getGenreColor } from '../../constants/genreColors';
import { api } from '../../api.js';

const reviewsQueue = ref([]);
const loading = ref(true);

function formatDate(dateStr) {
  if (!dateStr) return '';
  return String(dateStr).split('T')[0];
}

onMounted(async () => {
  try {
    const data = await api.get('/reviews/pending?size=50');
    reviewsQueue.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки рецензий:', e);
  } finally {
    loading.value = false;
  }
});

const selectedReview = ref(null);
const showDetailsModal = ref(false);

function viewDetails(review) {
  selectedReview.value = review;
  showDetailsModal.value = true;
}

async function approveReview(id) {
  try {
    await api.post(`/reviews/${id}/approve`);
    reviewsQueue.value = reviewsQueue.value.filter(r => r.id !== id);
    showDetailsModal.value = false;
  } catch (e) {
    console.error('Ошибка одобрения:', e);
  }
}

const showRejectModal = ref(false);
const rejectingReview = ref(null);
const rejectReason = ref('');
const rejectError = ref(null);

function openRejectModal(review) {
  rejectingReview.value = review;
  rejectReason.value = '';
  rejectError.value = null;
  showRejectModal.value = true;
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    rejectError.value = 'Укажите причину отклонения, чтобы автор смог исправить рецензию';
    return;
  }
  try {
    await api.post(`/reviews/${rejectingReview.value.id}/reject`, {
      moderatorComment: rejectReason.value.trim(),
    });
    reviewsQueue.value = reviewsQueue.value.filter(r => r.id !== rejectingReview.value.id);
    showRejectModal.value = false;
    showDetailsModal.value = false;
    rejectingReview.value = null;
  } catch (e) {
    rejectError.value = e.message || 'Не удалось отклонить рецензию';
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

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <UCard v-else variant="soft" class="bg-white rounded-2xl p-5">
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
                  class="w-24 h-36 object-cover rounded-lg shadow-md bg-gray-100"
                  :src="review.coverUrl || ''"
                  :alt="review.bookTitle"
                />
              </div>

              <div class="flex-1 min-w-0">
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
                    <p class="text-xs text-gray-400 mt-1">{{ formatDate(review.createdAt) }}</p>
                  </div>
                </div>

                <p class="mt-3 text-sm text-gray-700 line-clamp-3 break-words" style="overflow-wrap: anywhere">{{ review.text }}</p>

                <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
                  <p class="text-xs text-gray-500">
                    Автор рецензии:
                    <span class="font-medium">{{ review.reviewAuthorName || review.userName }}</span>
                  </p>
                  <div class="flex gap-2">
                    <UButton size="sm" color="primary" variant="soft" class="rounded-xl" @click="viewDetails(review)">
                      Подробнее
                    </UButton>
                    <UButton size="sm" color="green" class="rounded-xl" @click="approveReview(review.id)">
                      Одобрить
                    </UButton>
                    <UButton size="sm" color="red" variant="soft" class="rounded-xl" @click="openRejectModal(review)">
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
      <UModal v-model:open="showDetailsModal" class="z-100">
        <template #body>
          <div v-if="selectedReview" class="space-y-4">
            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <div class="flex gap-4">
                <img
                  class="w-28 h-40 object-cover rounded-lg shadow-md flex-shrink-0 bg-gray-100"
                  :src="selectedReview.coverUrl || ''"
                  :alt="selectedReview.bookTitle"
                />
                <div class="flex-1 min-w-0">
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
                      {{ selectedReview.reviewAuthorName || selectedReview.userName }}
                    </div>
                    <div class="text-sm text-gray-500">
                      <span class="font-semibold">Дата:</span> {{ formatDate(selectedReview.createdAt) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <h3 class="font-semibold text-black mb-3">Текст рецензии</h3>
              <div class="bg-gray-50 p-4 rounded-lg">
                <p class="text-gray-700 whitespace-pre-wrap leading-relaxed break-words" style="overflow-wrap: anywhere">{{ selectedReview.text }}</p>
              </div>
            </div>
          </div>
        </template>

        <template #footer>
          <div class="flex justify-between gap-3 w-full">
            <div class="flex gap-2">
              <UButton color="green" class="rounded-xl" @click="approveReview(selectedReview?.id)">Одобрить</UButton>
              <UButton color="red" variant="soft" class="rounded-xl" @click="openRejectModal(selectedReview)">Отклонить</UButton>
            </div>
            <UButton variant="outline" @click="showDetailsModal = false">Закрыть</UButton>
          </div>
        </template>
      </UModal>

      <UModal v-model:open="showRejectModal" class="z-100">
        <template #body>
          <div v-if="rejectingReview" class="space-y-3">
            <h3 class="text-xl font-bold text-black">Отклонить рецензию</h3>
            <p class="text-sm text-gray-600">
              Рецензия на «{{ rejectingReview.bookTitle }}» — укажите причину отклонения,
              чтобы автор мог исправить и переотправить.
            </p>
            <UTextarea
              v-model="rejectReason"
              :rows="4"
              placeholder="Например: текст рецензии не раскрывает книгу, оценка не соответствует тексту..."
              class="w-full"
            />
            <UAlert v-if="rejectError" color="error" variant="soft" :description="rejectError" />
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-3 w-full">
            <UButton variant="outline" @click="showRejectModal = false">Отмена</UButton>
            <UButton color="red" @click="confirmReject">Отклонить</UButton>
          </div>
        </template>
      </UModal>
    </div>
  </div>
</template>
