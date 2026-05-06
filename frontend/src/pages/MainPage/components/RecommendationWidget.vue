<script setup>
import { computed, ref, onMounted } from 'vue';
import Card from '../../../components/Card.vue';
import { api } from '../../../api.js';

const props = defineProps({
  query: { type: String, default: '' },
});

const personalRecommendations = ref([]);
const popularBooks = ref([]);
const newBooks = ref([]);
const loading = ref(true);
const orderedBooks = ref(new Set());

function matches(rec, q) {
  if (!q) return true;
  const ql = q.toLowerCase();
  return (
    (rec.title || '').toLowerCase().includes(ql) ||
    (rec.author || '').toLowerCase().includes(ql) ||
    (rec.genre || '').toLowerCase().includes(ql) ||
    (rec.description || '').toLowerCase().includes(ql)
  );
}

const filteredPersonal = computed(() => personalRecommendations.value.filter(r => matches(r, props.query)));
const filteredPopular = computed(() => popularBooks.value.filter(r => matches(r, props.query)));
const filteredNew = computed(() => newBooks.value.filter(r => matches(r, props.query)));
const hasAnyResults = computed(() =>
  filteredPersonal.value.length + filteredPopular.value.length + filteredNew.value.length > 0,
);

async function loadOrders() {
  try {
    const orders = await api.get('/orders');
    orderedBooks.value = new Set((orders || []).map(b => b.id));
  } catch {
    // anonymous viewer is OK
  }
}

onMounted(async () => {
  try {
    const [data, _] = await Promise.all([api.get('/recommendations'), loadOrders()]);
    personalRecommendations.value = data.personal || [];
    popularBooks.value = data.popular || [];
    newBooks.value = data.newBooks || [];
  } catch (error) {
    console.error('Ошибка загрузки рекомендаций:', error);
  } finally {
    loading.value = false;
  }
});

async function toggleOrder(bookId) {
  if (!bookId) return;
  if (orderedBooks.value.has(bookId)) {
    try {
      await api.delete(`/orders/${bookId}`);
      orderedBooks.value = new Set([...orderedBooks.value].filter(id => id !== bookId));
    } catch (e) {
      console.error('Ошибка удаления из заказов:', e);
    }
  } else {
    try {
      await api.post('/orders', { bookId });
      orderedBooks.value = new Set([...orderedBooks.value, bookId]);
    } catch (e) {
      console.error('Ошибка добавления в заказы:', e);
    }
  }
}

function isOrdered(bookId) {
  return orderedBooks.value.has(bookId);
}
</script>

<template>
  <div class="space-y-10">
    <div v-if="loading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>

    <template v-else>
      <!-- Персональные рекомендации -->
      <div v-if="filteredPersonal.length > 0">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="text-2xl font-bold text-black">Рекомендации для вас</h2>
            <p class="text-sm text-gray-600 mt-1">Подобрано на основе ваших предпочтений</p>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <div v-for="rec in filteredPersonal" :key="rec.id" class="relative">
            <Card
              :id="String(rec.bookId)"
              :title="rec.title"
              :genre="rec.genre"
              :author="rec.author"
              :description="rec.description"
              :image-url="rec.imageUrl"
              :is-ordered="isOrdered(rec.bookId)"
              @toggle-order="toggleOrder(rec.bookId)"
            />
          </div>
        </div>
      </div>

      <!-- Популярные книги -->
      <div v-if="filteredPopular.length > 0">
        <h2 class="text-2xl font-bold text-black mb-4">Популярное</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <Card
            v-for="rec in filteredPopular"
            :id="String(rec.bookId)"
            :key="rec.id"
            :title="rec.title"
            :genre="rec.genre"
            :author="rec.author"
            :description="rec.description"
            :image-url="rec.imageUrl"
            :is-ordered="isOrdered(rec.bookId)"
            @toggle-order="toggleOrder(rec.bookId)"
          />
        </div>
      </div>

      <!-- Новинки -->
      <div v-if="filteredNew.length > 0">
        <h2 class="text-2xl font-bold text-black mb-4">Новинки</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <Card
            v-for="rec in filteredNew"
            :id="String(rec.bookId)"
            :key="rec.id"
            :title="rec.title"
            :genre="rec.genre"
            :author="rec.author"
            :description="rec.description"
            :image-url="rec.imageUrl"
            :is-ordered="isOrdered(rec.bookId)"
            @toggle-order="toggleOrder(rec.bookId)"
          />
        </div>
      </div>

      <!-- Empty / no-match states -->
      <div
        v-if="!hasAnyResults && props.query"
        class="text-center py-12 bg-white rounded-3xl"
      >
        <h3 class="text-xl font-semibold text-gray-600 mb-2">Ничего не найдено</h3>
        <p class="text-gray-500">По запросу «{{ props.query }}» среди рекомендаций ничего не подошло.</p>
      </div>

      <div
        v-else-if="personalRecommendations.length === 0 && popularBooks.length === 0 && newBooks.length === 0"
        class="text-center py-12 bg-white rounded-3xl"
      >
        <h3 class="text-xl font-semibold text-gray-600 mb-2">Нет рекомендаций</h3>
        <p class="text-gray-500">Оцените больше книг, чтобы получать персонализированные рекомендации</p>
        <UButton to="/" color="primary" class="rounded-xl mt-4">Перейти в каталог</UButton>
      </div>
    </template>
  </div>
</template>
