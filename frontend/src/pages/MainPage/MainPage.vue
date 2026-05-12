<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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

const validTabIds = tabs.map(t => t.id);
const route = useRoute();
const router = useRouter();

function readTabFromRoute() {
  const t = route.query.tab;
  return validTabIds.includes(t) ? t : 'catalog';
}

const activeTab = ref(readTabFromRoute());
const query = ref('');

const books = ref([]);
const collections = ref([]);
const clubItems = ref([]);
const booksLoading = ref(false);
const collectionsLoading = ref(false);

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

async function loadCollections(q = '') {
  collectionsLoading.value = true;
  try {
    const params = q ? `?query=${encodeURIComponent(q)}&size=20` : '?size=20';
    const data = await api.get(`/collections${params}`);
    collections.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки подборок:', e);
  } finally {
    collectionsLoading.value = false;
  }
}

async function loadEvents() {
  try {
    const data = await api.get('/events?size=10');
    clubItems.value = (data.content || []).map(e => ({
      id: e.id,
      title: e.title,
      text: e.description,
      date: e.date,
      time: e.time,
      location: e.location,
      organizer: e.organizer,
      maxParticipants: e.maxParticipants,
      participantsCount: e.currentParticipants,
    }));
  } catch (e) {
    console.error('Ошибка загрузки событий:', e);
  }
}

function setTab(id) {
  activeTab.value = id;
  router.replace({ query: { ...route.query, tab: id } });
}

function searchPlaceholder() {
  switch (activeTab.value) {
    case 'collections': return 'Искать подборки';
    case 'subjects': return 'Поиск по специальности / предмету / подборке';
    case 'club': return 'Искать мероприятия';
    case 'recommendations': return 'Искать в рекомендациях';
    default: return 'Искать книги';
  }
}

function applySearch(val) {
  // Server-side search only for catalogs (heavy data, paginated). The other tabs
  // filter client-side using `query` as a prop.
  if (activeTab.value === 'catalog') {
    loadBooks(val);
  } else if (activeTab.value === 'collections') {
    loadCollections(val);
  }
}

watch(query, val => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => applySearch(val), 400);
});

watch(activeTab, () => {
  // Refresh the active tab's data when switching to it.
  if (activeTab.value === 'catalog') loadBooks(query.value);
  else if (activeTab.value === 'collections') loadCollections(query.value);
  else if (activeTab.value === 'club') loadEvents();
});

watch(() => route.query.tab, (val) => {
  if (validTabIds.includes(val) && val !== activeTab.value) {
    activeTab.value = val;
  }
});

function loadForTab(tab) {
  if (tab === 'catalog') loadBooks(query.value);
  else if (tab === 'collections') loadCollections(query.value);
  else if (tab === 'club') loadEvents();
  // Recommendations and Subjects manage their own data inside the child component.
}

onMounted(() => {
  // Always populate the catalog so collections that reference book covers can
  // resolve them, and load whichever tab the URL points at.
  loadBooks();
  if (activeTab.value !== 'catalog') loadForTab(activeTab.value);
});
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="space-y-4">
        <div class="flex items-center gap-4">
          <UInput
            v-model="query"
            icon="i-lucide-search"
            :placeholder="searchPlaceholder()"
            class="w-full"
          />
        </div>

        <div class="flex gap-3 flex-wrap">
          <UButton
            v-for="t in tabs"
            :key="t.id"
            size="lg"
            :variant="activeTab === t.id ? 'solid' : 'outline'"
            :color="activeTab === t.id ? 'primary' : 'neutral'"
            class="rounded-xl"
            :class="activeTab === t.id ? '' : 'bg-white hover:!text-white active:!text-white'"
            @click="setTab(t.id)"
          >
            {{ t.label }}
          </UButton>
        </div>
      </div>

      <!-- Рекомендации -->
      <div v-if="activeTab === 'recommendations'" class="mt-6">
        <RecommendationWidget :query="query" />
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
        <div v-if="collectionsLoading" class="flex justify-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        </div>
        <CollectionsTab v-else :collections="collections" :books="books" />
      </div>

      <!-- Предметы -->
      <div v-else-if="activeTab === 'subjects'" class="mt-6">
        <SubjectsTab :query="query" />
      </div>

      <!-- Книжный клуб -->
      <div v-else class="mt-6">
        <ClubTab :items="clubItems" :query="query" />
      </div>
    </div>
  </div>
</template>
