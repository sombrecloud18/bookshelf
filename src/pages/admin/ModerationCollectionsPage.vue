<script setup>
import { ref, computed } from 'vue';
import { getGenreColor } from '../../constants/genreColors';

const allBooks = [
  {
    id: 'book-satan',
    title: 'Скорбь сатаны',
    coverUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
    shortDescription: 'Мрачный роман с сильной атмосферой и интригой.',
  },
  {
    id: 'book-ave-maria',
    title: 'Проект "Ave Maria"',
    coverUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
    shortDescription: 'Научная фантастика о технологиях, морали и последствиях.',
  },
  {
    id: 'book-hunger-games',
    title: 'Голодные игры',
    coverUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
    shortDescription: 'Антиутопия о выживании, выборе и цене власти.',
  },
  {
    id: 'book-kwebe',
    title: 'Правда о деле Гарри Квеберта',
    coverUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
    shortDescription: 'Детектив с флешбэками и неожиданными поворотами.',
  },
  {
    id: 'book-master',
    title: 'Мастер и Маргарита',
    coverUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
    shortDescription: 'Классика: любовь, драма и философские мотивы.',
  },
  {
    id: 'book-bookship',
    title: 'Bookship',
    coverUrl: 'https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg',
    shortDescription: 'Фантастическая история о кораблях, времени и дружбе.',
  },
  {
    id: 'book-institute',
    title: 'Институт',
    coverUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
    shortDescription: 'Напряжённый сюжет и вопросы о свободе воли.',
  },
  {
    id: 'book-crows',
    title: 'Шестерка воронов',
    coverUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
    shortDescription: 'Фэнтези о командах, хитрости и больших ставках.',
  },
];

const booksById = Object.fromEntries(allBooks.map(b => [b.id, b]));

const collectionsQueue = ref([
  {
    id: 'col-pending-1',
    title: 'Любимые романы',
    genre: 'Роман',
    description: 'Подборка для вечеров, когда хочется глубины и атмосферы.',
    bookIds: ['book-master', 'book-satan', 'book-kwebe'],
    author: 'student1',
    authorName: 'Анна Смирнова',
    createdAt: '2026-04-10',
    status: 'pending',
  },
  {
    id: 'col-pending-2',
    title: 'Фантастика: технологии и выбор',
    genre: 'Фантастика',
    description: 'Книги, которые заставляют думать о будущем и людях.',
    bookIds: ['book-ave-maria', 'book-institute', 'book-bookship'],
    author: 'student2',
    authorName: 'Михаил Петров',
    createdAt: '2026-04-11',
    status: 'pending',
  },
]);

const selectedCollection = ref(null);
const showDetailsModal = ref(false);

const booksInCollection = computed(() => {
  if (!selectedCollection.value) return [];
  return selectedCollection.value.bookIds.map(id => booksById[id]).filter(Boolean);
});

function viewDetails(collection) {
  selectedCollection.value = collection;
  showDetailsModal.value = true;
}

function approveCollection(id) {
  const index = collectionsQueue.value.findIndex(c => c.id === id);
  if (index !== -1) {
    // В реальном приложении здесь был бы API запрос
    console.log('Одобрена подборка:', collectionsQueue.value[index].title);
    collectionsQueue.value.splice(index, 1);
  }
  showDetailsModal.value = false;
}

function rejectCollection(id) {
  const collection = collectionsQueue.value.find(c => c.id === id);
  if (confirm(`Отклонить подборку «${collection?.title}»?`)) {
    const index = collectionsQueue.value.findIndex(c => c.id === id);
    if (index !== -1) {
      console.log('Отклонена подборка:', collectionsQueue.value[index].title);
      collectionsQueue.value.splice(index, 1);
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
          <h1 class="text-4xl font-bold text-black">Модерация подборок</h1>
          <p class="text-gray-700 mt-2">На проверке: {{ collectionsQueue.length }} подборок</p>
        </div>
        <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
      </div>

      <UCard variant="soft" class="bg-white rounded-2xl p-5">
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
                <span class="text-xs text-gray-400">{{ collection.createdAt }}</span>
              </div>

              <div class="mt-3">
                <h2 class="text-xl font-bold text-black line-clamp-2">{{ collection.title }}</h2>
                <p class="text-sm text-gray-600 mt-1">Автор: {{ collection.authorName || collection.author }}</p>
                <p v-if="collection.description" class="mt-2 text-sm text-gray-700 line-clamp-2">
                  {{ collection.description }}
                </p>
                <p class="mt-2 text-xs text-gray-500">Книг: {{ collection.bookIds.length }}</p>
              </div>

              <div
                class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center"
              >
                <div class="relative h-[80%] w-[78%]">
                  <div v-if="collection.bookIds.length === 0" class="text-xs text-gray-500 text-center">Нет книг</div>
                  <template v-else>
                    <img
                      v-for="(b, idx) in collection.bookIds
                        .slice(-2)
                        .map(id => booksById[id])
                        .filter(Boolean)
                        .reverse()"
                      :key="b.id"
                      class="absolute h-full rounded-2xl shadow-lg border border-white bg-white/90 p-1 object-cover"
                      :style="{
                        right: `${idx * 22}px`,
                        top: `calc(50% + ${idx * 10}px)`,
                        transform: 'translateY(-50%)',
                        zIndex: 10 + idx,
                      }"
                      :src="b.coverUrl"
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
                  @click="rejectCollection(collection.id)"
                >
                  Отклонить
                </UButton>
              </div>
            </div>
          </UCard>
        </div>
      </UCard>

      <!-- Модальное окно с подробностями подборки -->
      <UModal v-model="showDetailsModal" class="z-100">
        <template #body>
          <div v-if="selectedCollection" class="space-y-4">
            <div class="bg-white rounded-2xl border border-gray-200 p-5">
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center gap-3 flex-wrap">
                    <h2 class="text-2xl font-bold text-black">{{ selectedCollection.title }}</h2>
                    <span
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
                      <span class="font-semibold">Дата создания:</span> {{ selectedCollection.createdAt }}
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
                    <img class="w-12 h-16 object-cover rounded-md flex-none" :src="book.coverUrl" :alt="book.title" />
                    <div>
                      <div class="font-semibold text-black">{{ book.title }}</div>
                      <div class="text-xs text-gray-600 mt-1 line-clamp-2">{{ book.shortDescription }}</div>
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
    </div>
  </div>
</template>
