<script setup>
import { ref, computed, onMounted } from 'vue';
import { getGenreColor } from '../../constants/genreColors';
import { api } from '../../api.js';

const collectionsQueue = ref([]);
const booksCache = ref({});
const loading = ref(true);

function formatDate(dateStr) {
  if (!dateStr) return '';
  return String(dateStr).split('T')[0];
}

async function loadBookCovers(bookIds) {
  const missing = (bookIds || []).filter(id => !booksCache.value[id]);
  if (missing.length === 0) return;
  try {
    const books = await api.post('/books/by-ids', { ids: missing });
    books.forEach(b => { booksCache.value[b.id] = b; });
  } catch (e) {
    console.error('Ошибка загрузки книг:', e);
  }
}

onMounted(async () => {
  try {
    const data = await api.get('/collections/pending?size=50');
    collectionsQueue.value = data.content || [];
    const allIds = [...new Set(collectionsQueue.value.flatMap(c => c.bookIds || []))];
    if (allIds.length > 0) await loadBookCovers(allIds);
  } catch (e) {
    console.error('Ошибка загрузки подборок:', e);
  } finally {
    loading.value = false;
  }
});

const selectedCollection = ref(null);
const showDetailsModal = ref(false);

const booksInCollection = computed(() => {
  if (!selectedCollection.value) return [];
  return (selectedCollection.value.bookIds || []).map(id => booksCache.value[id]).filter(Boolean);
});

function viewDetails(collection) {
  selectedCollection.value = collection;
  showDetailsModal.value = true;
}

async function approveCollection(id) {
  try {
    await api.post(`/collections/${id}/approve`);
    collectionsQueue.value = collectionsQueue.value.filter(c => c.id !== id);
    showDetailsModal.value = false;
  } catch (e) {
    console.error('Ошибка одобрения:', e);
  }
}

const showRejectModal = ref(false);
const rejectingCollection = ref(null);
const rejectReason = ref('');

function openRejectModal(collection) {
  rejectingCollection.value = collection;
  rejectReason.value = '';
  showRejectModal.value = true;
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    alert('Пожалуйста, укажите причину отклонения');
    return;
  }
  try {
    await api.post(`/collections/${rejectingCollection.value.id}/reject`, {
      moderatorComment: rejectReason.value.trim(),
    });
    collectionsQueue.value = collectionsQueue.value.filter(c => c.id !== rejectingCollection.value.id);
    showRejectModal.value = false;
    showDetailsModal.value = false;
    rejectingCollection.value = null;
  } catch (e) {
    console.error('Ошибка отклонения:', e);
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-4xl font-bold text-black">Модерация подборок</h1>
          <p class="text-gray-700 mt-2">На проверке: {{ collectionsQueue.length }} подборок</p>
        </div>
        <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <UCard v-else variant="soft" class="bg-white rounded-2xl p-5">
        <div v-if="collectionsQueue.length === 0" class="text-center py-12 text-gray-500">
          <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <p class="text-lg">Нет подборок на модерации</p>
          <p class="text-sm mt-1">Все подборки проверены</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <UCard
            v-for="collection in collectionsQueue"
            :key="collection.id"
            variant="soft"
            class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white border border-gray-200"
          >
            <div class="flex flex-col">
              <div class="flex items-center justify-between">
                <span
                  v-if="collection.genre"
                  class="inline-flex px-3 py-1 text-sm font-medium rounded-md"
                  :class="getGenreColor(collection.genre)"
                >
                  {{ collection.genre }}
                </span>
                <span class="text-xs text-gray-400">{{ formatDate(collection.createdAt) }}</span>
              </div>

              <div class="mt-3">
                <h2 class="text-xl font-bold text-black line-clamp-2">{{ collection.title }}</h2>
                <p class="text-sm text-gray-600 mt-1">Автор: {{ collection.authorName || collection.author }}</p>
                <p v-if="collection.description" class="mt-2 text-sm text-gray-700 line-clamp-2">
                  {{ collection.description }}
                </p>
                <p class="mt-2 text-xs text-gray-500">Книг: {{ (collection.bookIds || []).length }}</p>
              </div>

              <div class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center">
                <div class="relative h-[80%] w-[78%]">
                  <div v-if="(collection.bookIds || []).length === 0" class="text-xs text-gray-500 text-center">Нет книг</div>
                  <template v-else>
                    <img
                      v-for="(b, idx) in (collection.bookIds || []).slice(-2).map(id => booksCache[id]).filter(Boolean).reverse()"
                      :key="b.id"
                      class="absolute h-full rounded-2xl shadow-lg border border-white bg-white/90 p-1 object-cover"
                      :style="{
                        right: `${idx * 22}px`,
                        top: `calc(50% + ${idx * 10}px)`,
                        transform: 'translateY(-50%)',
                        zIndex: 10 + idx,
                      }"
                      :src="b.coverUrl || b.imageUrl || ''"
                      :alt="b.title"
                    />
                  </template>
                </div>
              </div>

              <div class="flex gap-2 mt-4 pt-2">
                <UButton
                  size="sm"
                  color="primary"
                  variant="soft"
                  class="flex-1 rounded-xl"
                  @click="viewDetails(collection)"
                >
                  Подробнее
                </UButton>
                <UButton size="sm" color="green" class="rounded-xl" @click="approveCollection(collection.id)">
                  Одобрить
                </UButton>
                <UButton
                  size="sm"
                  color="red"
                  variant="soft"
                  class="rounded-xl"
                  @click="openRejectModal(collection)"
                >
                  Отклонить
                </UButton>
              </div>
            </div>
          </UCard>
        </div>
      </UCard>

      <!-- Модальное окно с подробностями подборки -->
      <UModal v-model:open="showDetailsModal" class="z-100">
        <template #body>
          <div v-if="selectedCollection" class="space-y-4">
            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center gap-3 flex-wrap">
                    <h2 class="text-2xl font-bold text-black">{{ selectedCollection.title }}</h2>
                    <span
                      v-if="selectedCollection.genre"
                      class="inline-flex px-3 py-1 text-sm font-medium rounded-md"
                      :class="getGenreColor(selectedCollection.genre)"
                    >
                      {{ selectedCollection.genre }}
                    </span>
                  </div>
                  <div class="mt-3 space-y-2">
                    <p class="text-sm text-gray-600">
                      <span class="font-semibold">Автор:</span>
                      {{ selectedCollection.authorName || selectedCollection.author }}
                    </p>
                    <p class="text-sm text-gray-600">
                      <span class="font-semibold">Дата создания:</span> {{ formatDate(selectedCollection.createdAt) }}
                    </p>
                    <p
                      v-if="selectedCollection.description"
                      class="text-sm text-gray-700 bg-gray-50 p-3 rounded-lg mt-2"
                    >
                      <span class="font-semibold">Описание:</span><br />
                      {{ selectedCollection.description }}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <h3 class="font-semibold text-black mb-3">Книги в подборке ({{ booksInCollection.length }})</h3>
              <div class="space-y-3 max-h-[400px] overflow-auto pr-1">
                <div
                  v-for="book in booksInCollection"
                  :key="book.id"
                  class="p-3 rounded-xl border border-gray-200 bg-gray-50"
                >
                  <div class="flex items-start gap-3">
                    <img
                      class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                      :src="book.coverUrl || book.imageUrl || ''"
                      :alt="book.title"
                    />
                    <div>
                      <div class="font-semibold text-black">{{ book.title }}</div>
                      <div class="text-xs text-gray-600 mt-1 line-clamp-2">{{ book.description }}</div>
                    </div>
                  </div>
                </div>
                <div v-if="booksInCollection.length === 0" class="text-sm text-gray-500 text-center py-4">
                  В подборке нет книг
                </div>
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

      <UModal v-model:open="showRejectModal" class="z-100">
        <template #body>
          <div v-if="rejectingCollection" class="space-y-3">
            <h3 class="text-xl font-bold text-black">Отклонить подборку</h3>
            <p class="text-sm text-gray-600">«{{ rejectingCollection.title }}» — укажите причину отклонения, чтобы автор мог исправить:</p>
            <UTextarea
              v-model="rejectReason"
              :rows="4"
              placeholder="Например: недостаточно книг, описание не раскрывает идею подборки..."
              class="w-full"
            />
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
