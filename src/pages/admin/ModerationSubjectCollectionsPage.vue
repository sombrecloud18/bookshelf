<!-- pages/admin/ModerationSubjectCollectionsPage.vue -->
<script setup>
import { ref, computed } from 'vue';
import { useSubjectCollectionsStore } from '../../stores/subjectCollectionsStore';

const store = useSubjectCollectionsStore();

const selectedCollection = ref(null);
const showDetailsModal = ref(false);
const rejectReason = ref('');
const showRejectModal = ref(false);

const allBooks = {
  'book-satan': {
    id: 'book-satan',
    title: 'Скорбь сатаны',
    coverUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
  },
  'book-ave-maria': {
    id: 'book-ave-maria',
    title: 'Проект "Ave Maria"',
    coverUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
  },
  'book-hunger-games': {
    id: 'book-hunger-games',
    title: 'Голодные игры',
    coverUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
  },
  'book-kwebe': {
    id: 'book-kwebe',
    title: 'Правда о деле Гарри Квеберта',
    coverUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
  },
  'book-institute': {
    id: 'book-institute',
    title: 'Институт',
    coverUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
  },
  'book-crows': {
    id: 'book-crows',
    title: 'Шестерка воронов',
    coverUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
  },
  'book-master': {
    id: 'book-master',
    title: 'Мастер и Маргарита',
    coverUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
  },
  'book-bookship': {
    id: 'book-bookship',
    title: 'Bookship',
    coverUrl: 'https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg',
  },
};

const booksInCollection = computed(() => {
  if (!selectedCollection.value) return [];
  return selectedCollection.value.bookIds.map(id => allBooks[id]).filter(Boolean);
});

function viewDetails(collection) {
  selectedCollection.value = collection;
  showDetailsModal.value = true;
}

function approve(collection) {
  if (confirm(`Одобрить подборку "${collection.title}"?`)) {
    store.approveCollection(collection.id);
  }
}

function openRejectModal(collection) {
  selectedCollection.value = collection;
  rejectReason.value = '';
  showRejectModal.value = true;
}

function confirmReject() {
  if (rejectReason.value.trim()) {
    store.rejectCollection(selectedCollection.value.id, rejectReason.value);
    showRejectModal.value = false;
    selectedCollection.value = null;
  } else {
    alert('Пожалуйста, укажите причину отклонения');
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-4xl font-bold text-black">Модерация подборок по предметам</h1>
          <p class="text-gray-700 mt-2">На проверке: {{ store.pendingCollections.length }} подборок</p>
        </div>
        <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
      </div>

      <UCard variant="soft" class="bg-white rounded-2xl p-5">
        <div v-if="store.pendingCollections.length === 0" class="text-center py-12 text-gray-500">
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
            v-for="collection in store.pendingCollections"
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
                      <span class="text-sm text-gray-600">{{ collection.specialtyName }}</span>
                    </div>
                  </div>
                  <span class="inline-flex px-3 py-1 text-xs font-medium rounded-full bg-yellow-100 text-yellow-800">
                    На модерации
                  </span>
                </div>

                <p class="mt-2 text-gray-700">{{ collection.description }}</p>

                <div class="mt-3 flex flex-wrap gap-4 text-sm text-gray-500">
                  <span>📚 Книг: {{ collection.bookIds.length }}</span>
                  <span
                    >👤 Автор: {{ collection.author }} ({{
                      collection.authorRole === 'teacher' ? 'Преподаватель' : 'Студент'
                    }})</span
                  >
                  <span>📅 {{ collection.createdAt }}</span>
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

      <!-- Модальное окно с подробностями - ИСПРАВЛЕНО: v-model:open -->
      <UModal v-model:open="showDetailsModal" class="z-100">
        <div v-if="selectedCollection" class="bg-white rounded-2xl max-w-2xl mx-auto">
          <div class="p-6">
            <h2 class="text-2xl font-bold text-black mb-4">{{ selectedCollection.title }}</h2>
            <div class="space-y-3">
              <p><span class="font-semibold">Предмет:</span> {{ selectedCollection.subject }}</p>
              <p><span class="font-semibold">Специальность:</span> {{ selectedCollection.specialtyName }}</p>
              <p>
                <span class="font-semibold">Автор:</span> {{ selectedCollection.author }} ({{
                  selectedCollection.authorRole === 'teacher' ? 'Преподаватель' : 'Студент'
                }})
              </p>
              <p><span class="font-semibold">Дата создания:</span> {{ selectedCollection.createdAt }}</p>
              <div>
                <p class="font-semibold">Описание:</p>
                <p class="text-gray-700 bg-gray-50 p-3 rounded-lg mt-1">{{ selectedCollection.description }}</p>
              </div>
              <div>
                <p class="font-semibold mb-2">Книги в подборке ({{ booksInCollection.length }})</p>
                <div class="space-y-2 max-h-64 overflow-auto">
                  <div
                    v-for="book in booksInCollection"
                    :key="book.id"
                    class="flex items-center gap-3 p-2 bg-gray-50 rounded-lg"
                  >
                    <img class="w-10 h-14 object-cover rounded" :src="book.coverUrl" :alt="book.title" />
                    <span class="font-medium">{{ book.title }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-6 border-t bg-gray-50 rounded-b-2xl">
            <UButton variant="outline" @click="showDetailsModal = false">Закрыть</UButton>
          </div>
        </div>
      </UModal>

      <!-- Модальное окно для причины отклонения - ИСПРАВЛЕНО: v-model:open -->
      <UModal v-model:open="showRejectModal" class="z-100">
        <div class="bg-white rounded-2xl max-w-md mx-auto">
          <div class="p-6">
            <h3 class="text-xl font-bold text-black mb-2">Отклонить подборку</h3>
            <p class="text-gray-600 mb-4">Укажите причину отклонения:</p>
            <UTextarea
              v-model="rejectReason"
              :rows="4"
              placeholder="Например: недостаточно книг, неподходящее содержание, ошибки в описании..."
              class="w-full"
            />
          </div>
          <div class="flex justify-end gap-3 p-6 border-t bg-gray-50 rounded-b-2xl">
            <UButton variant="outline" @click="showRejectModal = false">Отмена</UButton>
            <UButton color="red" @click="confirmReject">Отклонить</UButton>
          </div>
        </div>
      </UModal>
    </div>
  </div>
</template>
