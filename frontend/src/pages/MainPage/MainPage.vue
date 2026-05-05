<script setup>
import { ref, watch } from 'vue';
import CatalogTab from './components/CatalogTab.vue';
import CollectionsTab from './components/CollectionsTab.vue';
import SubjectsTab from './components/SubjectsTab.vue';
import ClubTab from './components/ClubTab.vue';
import RecommendationWidget from './components/RecommendationWidget.vue';
import { api } from '../../api.js';

const tabs = [
  { id: 'catalog', label: 'Каталог' },
  { id: 'recommendations', label: 'Рекомендации' },
  { id: 'collections', label: 'Подборки' },
  { id: 'subjects', label: 'Предметы' },
  { id: 'club', label: 'Книжный клуб' },
];

const activeTab = ref('catalog');
const query = ref('');

const books = ref([]);
const collections = ref([]);
const clubItems = ref([]);
const booksLoading = ref(false);

let searchTimer = null;

async function loadBooks(q = '') {
  booksLoading.value = true;
  try {
    const params = q ? `?query=${encodeURIComponent(q)}&size=20` : '?size=20';
    const data = await api.get(`/books${params}`);
    books.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки книг:', e);
  } finally {
    booksLoading.value = false;
  }
}

async function loadCollections() {
  try {
    const data = await api.get('/collections?size=20');
    collections.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки подборок:', e);
  }
}

async function loadEvents() {
  try {
    const data = await api.get('/events?size=10');
    clubItems.value = (data.content || []).map(e => ({
      id: e.id,
      title: e.title,
      text: e.description,
      participantsCount: e.currentParticipants,
    }));
  } catch (e) {
    console.error('Ошибка загрузки событий:', e);
  }
}

loadBooks();
loadCollections();
loadEvents();

watch(query, val => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => loadBooks(val), 400);
});
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="space-y-4">
        <div class="flex items-center gap-4">
          <UInput v-model="query" icon="i-lucide-search" placeholder="Искать книги" class="w-full" />
        </div>

        <div class="flex gap-3 flex-wrap">
          <UButton
            v-for="t in tabs"
            :key="t.id"
            size="lg"
            :variant="activeTab === t.id ? 'solid' : 'outline'"
            :color="activeTab === t.id ? 'primary' : 'neutral'"
            class="rounded-xl"
            @click="activeTab = t.id"
          >
            {{ t.label }}
          </UButton>
        </div>
      </div>

      <!-- Рекомендации -->
      <div v-if="activeTab === 'recommendations'" class="mt-6">
        <RecommendationWidget />
      </div>

      <!-- Каталог -->
      <div v-else-if="activeTab === 'catalog'" class="mt-6">
        <div v-if="booksLoading" class="flex justify-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        </div>
        <CatalogTab v-else :books="books" />
      </div>

      <!-- Подборки -->
      <div v-else-if="activeTab === 'collections'" class="mt-6">
        <CollectionsTab :collections="collections" :books="books" />
      </div>

      <!-- Предметы -->
      <div v-else-if="activeTab === 'subjects'" class="mt-6">
        <SubjectsTab />
      </div>

      <!-- Книжный клуб -->
      <div v-else class="mt-6">
        <ClubTab :items="clubItems" />
      </div>
    </div>
  </div>
</template>
