<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../../api.js';

const pendingCollections = ref([]);
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
    const data = await api.get('/subject-collections/pending?size=50');
    pendingCollections.value = data.content || [];
    const allIds = [...new Set(pendingCollections.value.flatMap(c => c.bookIds || []))];
    if (allIds.length > 0) await loadBookCovers(allIds);
  } catch (e) {
    console.error('Ошибка загрузки:', e);
  } finally {
    loading.value = false;
  }
});

const selectedCollection = ref(null);
const showDetailsModal = ref(false);
const rejectReason = ref('');
const showRejectModal = ref(false);

const booksInCollection = computed(() => {
  if (!selectedCollection.value) return [];
  return (selectedCollection.value.bookIds || []).map(id => booksCache.value[id]).filter(Boolean);
});

function viewDetails(collection) {
  selectedCollection.value = collection;
  showDetailsModal.value = true;
}

async function approve(collection) {
  if (!confirm(`Одобрить подборку "${collection.title}"?`)) return;
  try {
    await api.post(`/subject-collections/${collection.id}/approve`);
    pendingCollections.value = pendingCollections.value.filter(c => c.id !== collection.id);
    showDetailsModal.value = false;
  } catch (e) {
    console.error('Ошибка одобрения:', e);
  }
}

function openRejectModal(collection) {
  selectedCollection.value = collection;
  rejectReason.value = '';
  showRejectModal.value = true;
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    alert('Пожалуйста, укажите причину отклонения');
    return;
  }
  try {
    await api.post(`/subject-collections/${selectedCollection.value.id}/reject`, {
      moderatorComment: rejectReason.value.trim(),
    });
    pendingCollections.value = pendingCollections.value.filter(c => c.id !== selectedCollection.value.id);
    showRejectModal.value = false;
    showDetailsModal.value = false;
    selectedCollection.value = null;
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
          <h1 class="text-4xl font-bold text-black">Модерация подборок по предметам</h1>
          <p class="text-gray-700 mt-2">На проверке: {{ pendingCollections.length }} подборок</p>
        </div>
        <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <UCard v-else variant="soft" class="bg-white rounded-2xl p-5">
        <div v-if="pendingCollections.length === 0" class="text-center py-12 text-gray-500">
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

        <div v-else class="space-y-4">
          <UCard
            v-for="collection in pendingCollections"
            :key="collection.id"
            variant="soft"
            class="bg-white rounded-xl border border-gray-200 hover:shadow-md transition-shadow"
          >
            <div class="flex flex-col md:flex-row justify-between gap-4">
              <div class="flex-1">
                <div class="flex items-start justify-between">
                  <div>
                    <h3 class="text-xl font-bold text-black">{{ collection.title }}</h3>
                    <div class="flex items-center gap-2 mt-1">
                      <span class="text-sm text-gray-600">{{ collection.subject }}</span>
                      <span class="text-xs text-gray-400">•</span>
                      <span class="text-sm text-gray-600">{{ collection.specialtyName || collection.specialty }}</span>
                    </div>
                  </div>
                  <span class="inline-flex px-3 py-1 text-xs font-medium rounded-full bg-yellow-100 text-yellow-800">
                    На модерации
                  </span>
                </div>

                <p class="mt-2 text-gray-700">{{ collection.description }}</p>

                <div class="mt-3 flex flex-wrap gap-4 text-sm text-gray-500">
                  <span>📚 Книг: {{ (collection.bookIds || []).length }}</span>
                  <span>👤 Автор: {{ collection.author }} ({{ collection.authorRole === 'TEACHER' ? 'Преподаватель' : 'Студент' }})</span>
                  <span>📅 {{ formatDate(collection.createdAt) }}</span>
                </div>
              </div>

              <div class="flex gap-2 items-start">
                <UButton size="sm" color="primary" variant="soft" class="rounded-xl" @click="viewDetails(collection)">
                  Подробнее
                </UButton>
                <UButton size="sm" color="green" class="rounded-xl" @click="approve(collection)"> Одобрить </UButton>
                <UButton size="sm" color="red" variant="soft" class="rounded-xl" @click="openRejectModal(collection)">
                  Отклонить
                </UButton>
              </div>
            </div>
          </UCard>
        </div>
      </UCard>

      <!-- Детали подборки -->
      <UModal v-model:open="showDetailsModal" class="z-100">
        <template #body>
          <div v-if="selectedCollection" class="space-y-3">
            <h2 class="text-2xl font-bold text-black">{{ selectedCollection.title }}</h2>
            <p><span class="font-semibold">Предмет:</span> {{ selectedCollection.subject }}</p>
            <p>
              <span class="font-semibold">Специальность:</span>
              {{ selectedCollection.specialtyName || selectedCollection.specialty }}
            </p>
            <p>
              <span class="font-semibold">Автор:</span> {{ selectedCollection.author }} ({{
                selectedCollection.authorRole === 'TEACHER' ? 'Преподаватель' : 'Студент'
              }})
            </p>
            <p><span class="font-semibold">Дата создания:</span> {{ formatDate(selectedCollection.createdAt) }}</p>
            <div>
              <p class="font-semibold">Описание:</p>
              <p class="text-gray-700 bg-gray-50 p-3 rounded-lg mt-1 break-words" style="overflow-wrap: anywhere">
                {{ selectedCollection.description }}
              </p>
            </div>
            <div>
              <p class="font-semibold mb-2">Книги в подборке ({{ booksInCollection.length }})</p>
              <div class="space-y-2 max-h-64 overflow-auto pr-1">
                <div
                  v-for="book in booksInCollection"
                  :key="book.id"
                  class="flex items-center gap-3 p-2 bg-gray-50 rounded-lg"
                >
                  <img
                    class="w-10 h-14 object-cover rounded bg-gray-100 flex-none"
                    :src="book.coverUrl || book.imageUrl || ''"
                    :alt="book.title"
                  />
                  <span class="font-medium">{{ book.title }}</span>
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

      <!-- Причина отклонения -->
      <UModal v-model:open="showRejectModal" class="z-100">
        <template #body>
          <div class="space-y-3">
            <h3 class="text-xl font-bold text-black">Отклонить подборку</h3>
            <p class="text-gray-600">Укажите причину отклонения, чтобы автор мог исправить:</p>
            <UTextarea
              v-model="rejectReason"
              :rows="4"
              placeholder="Например: недостаточно книг, неподходящее содержание, ошибки в описании..."
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
