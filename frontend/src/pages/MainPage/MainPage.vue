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

const BOOKS_PAGE_SIZE = 20;
const COLLECTIONS_PAGE_SIZE = 12;
const EVENTS_PAGE_SIZE = 9;

const books = ref([]);
const collections = ref([]);
const clubItems = ref([]);

const booksLoading = ref(false);
const booksLoadingMore = ref(false);
const booksPage = ref(0);
const booksHasMore = ref(true);
const booksQuery = ref('');

const collectionsLoading = ref(false);
const collectionsLoadingMore = ref(false);
const collectionsPage = ref(0);
const collectionsHasMore = ref(true);
const collectionsQuery = ref('');

const eventsLoading = ref(false);
const eventsLoadingMore = ref(false);
const eventsPage = ref(0);
const eventsHasMore = ref(true);

let searchTimer = null;

function mapEvent(e) {
  return {
    id: e.id,
    title: e.title,
    text: e.description,
    date: e.date,
    time: e.time,
    location: e.location,
    organizer: e.organizer,
    maxParticipants: e.maxParticipants,
    participantsCount: e.currentParticipants,
  };
}

// Общая утилита: первый или следующий запрос пагинированного эндпоинта.
async function fetchPage(path, page, size, query) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (query) params.set('query', query);
  return api.get(`${path}?${params.toString()}`);
}

async function loadBooks(q = '') {
  booksLoading.value = true;
  booksPage.value = 0;
  booksHasMore.value = true;
  booksQuery.value = q;
  try {
    const data = await fetchPage('/books', 0, BOOKS_PAGE_SIZE, q);
    books.value = data.content || [];
    booksHasMore.value = !data.last && (data.content || []).length > 0;
  } catch (e) {
    console.error('Ошибка загрузки книг:', e);
    booksHasMore.value = false;
  } finally {
    booksLoading.value = false;
  }
}

async function loadMoreBooks() {
  if (booksLoadingMore.value || booksLoading.value || !booksHasMore.value) return;
  booksLoadingMore.value = true;
  try {
    const nextPage = booksPage.value + 1;
    const data = await fetchPage('/books', nextPage, BOOKS_PAGE_SIZE, booksQuery.value);
    const newItems = data.content || [];
    if (newItems.length > 0) {
      books.value = [...books.value, ...newItems];
      booksPage.value = nextPage;
    }
    booksHasMore.value = !data.last && newItems.length > 0;
  } catch (e) {
    console.error('Ошибка дозагрузки книг:', e);
    booksHasMore.value = false;
  } finally {
    booksLoadingMore.value = false;
  }
}

async function loadCollections(q = '') {
  collectionsLoading.value = true;
  collectionsPage.value = 0;
  collectionsHasMore.value = true;
  collectionsQuery.value = q;
  try {
    const data = await fetchPage('/collections', 0, COLLECTIONS_PAGE_SIZE, q);
    collections.value = data.content || [];
    collectionsHasMore.value = !data.last && (data.content || []).length > 0;
  } catch (e) {
    console.error('Ошибка загрузки подборок:', e);
    collectionsHasMore.value = false;
  } finally {
    collectionsLoading.value = false;
  }
}

async function loadMoreCollections() {
  if (collectionsLoadingMore.value || collectionsLoading.value || !collectionsHasMore.value) return;
  collectionsLoadingMore.value = true;
  try {
    const nextPage = collectionsPage.value + 1;
    const data = await fetchPage('/collections', nextPage, COLLECTIONS_PAGE_SIZE, collectionsQuery.value);
    const newItems = data.content || [];
    if (newItems.length > 0) {
      collections.value = [...collections.value, ...newItems];
      collectionsPage.value = nextPage;
    }
    collectionsHasMore.value = !data.last && newItems.length > 0;
  } catch (e) {
    console.error('Ошибка дозагрузки подборок:', e);
    collectionsHasMore.value = false;
  } finally {
    collectionsLoadingMore.value = false;
  }
}

async function loadEvents() {
  eventsLoading.value = true;
  eventsPage.value = 0;
  eventsHasMore.value = true;
  try {
    const data = await fetchPage('/events', 0, EVENTS_PAGE_SIZE);
    clubItems.value = (data.content || []).map(mapEvent);
    eventsHasMore.value = !data.last && (data.content || []).length > 0;
  } catch (e) {
    console.error('Ошибка загрузки событий:', e);
    eventsHasMore.value = false;
  } finally {
    eventsLoading.value = false;
  }
}

async function loadMoreEvents() {
  if (eventsLoadingMore.value || eventsLoading.value || !eventsHasMore.value) return;
  eventsLoadingMore.value = true;
  try {
    const nextPage = eventsPage.value + 1;
    const data = await fetchPage('/events', nextPage, EVENTS_PAGE_SIZE);
    const newItems = (data.content || []).map(mapEvent);
    if (newItems.length > 0) {
      clubItems.value = [...clubItems.value, ...newItems];
      eventsPage.value = nextPage;
    }
    eventsHasMore.value = !data.last && newItems.length > 0;
  } catch (e) {
    console.error('Ошибка дозагрузки событий:', e);
    eventsHasMore.value = false;
  } finally {
    eventsLoadingMore.value = false;
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
        <CatalogTab
          v-else
          :books="books"
          :has-more="booksHasMore"
          :loading-more="booksLoadingMore"
          @load-more="loadMoreBooks"
        />
      </div>

      <!-- Подборки -->
      <div v-else-if="activeTab === 'collections'" class="mt-6">
        <div v-if="collectionsLoading" class="flex justify-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        </div>
        <CollectionsTab
          v-else
          :collections="collections"
          :books="books"
          :has-more="collectionsHasMore"
          :loading-more="collectionsLoadingMore"
          @load-more="loadMoreCollections"
        />
      </div>

      <!-- Предметы -->
      <div v-else-if="activeTab === 'subjects'" class="mt-6">
        <SubjectsTab :query="query" />
      </div>

      <!-- Книжный клуб -->
      <div v-else class="mt-6">
        <div v-if="eventsLoading" class="flex justify-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        </div>
        <ClubTab
          v-else
          :items="clubItems"
          :query="query"
          :has-more="eventsHasMore"
          :loading-more="eventsLoadingMore"
          @load-more="loadMoreEvents"
        />
      </div>
    </div>
  </div>
</template>
