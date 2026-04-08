<script setup>
import { computed, reactive, ref } from 'vue';
import { getGenreColor } from '../../constants/genreColors';

const booksCatalog = [
  {
    id: 'book-satan',
    title: 'Скорбь сатаны',
    author: 'Мария Корелли',
    genre: 'Роман',
    coverUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
  },
  {
    id: 'book-ave-maria',
    title: 'Проект "Ave Maria"',
    author: 'Энди Вейер',
    genre: 'Научная фантастика',
    coverUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
  },
  {
    id: 'book-hunger-games',
    title: 'Голодные игры',
    author: 'Сьюзен Коллинз',
    genre: 'Фэнтези',
    coverUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
  },
  {
    id: 'book-kwebe',
    title: 'Правда о деле Гарри Квеберта',
    author: 'Жоэль Диккер',
    genre: 'Детектив',
    coverUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
  },
];

const reviews = reactive([
  {
    id: 'rev-1',
    bookId: 'book-satan',
    bookTitle: 'Скорбь сатаны',
    author: 'Мария Корелли',
    genre: 'Роман',
    coverUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
    rating: 5,
    text: 'В центре повествования — талантливый, но бедный и гордый писатель. Роман читается легко, при этом оставляет сильное послевкусие и заставляет задуматься о выборе и ответственности.\n\nОсобенно понравилась атмосфера и то, как автор раскрывает характеры. Некоторые моменты кажутся наивными, но в целом это не мешает цельности истории.',
  },
]);

const editingId = ref(null);
const isEditing = ref(false);
const saving = ref(false);
const savedToast = ref(false);
let savedTimer = null;

const current = computed(() => reviews.find(r => r.id === editingId.value) || null);

function setRating(value) {
  const r = current.value;
  if (!r) return;
  if (!isEditing.value) return;
  r.rating = value;
}

async function save() {
  if (!isEditing.value) return;
  saving.value = true;
  await new Promise(r => setTimeout(r, 400));
  saving.value = false;
  savedToast.value = true;
  isEditing.value = false;
  if (savedTimer) window.clearTimeout(savedTimer);
  savedTimer = window.setTimeout(() => {
    savedToast.value = false;
  }, 2500);
}

function startEdit(reviewId) {
  if (
    isEditing.value &&
    editingId.value !== reviewId &&
    !confirm('Есть несохранённые изменения. Продолжить без сохранения?')
  ) {
    return;
  }
  editingId.value = reviewId;
  isEditing.value = true;
}

// Add review modal
const addOpen = ref(false);
const addQuery = ref('');
const addSelectedBookId = ref(null);
const addRating = ref(0);
const addText = ref('');

const filteredBooks = computed(() => {
  const q = addQuery.value.trim().toLowerCase();
  if (!q) return booksCatalog;
  return booksCatalog.filter(
    b => b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q) || b.genre.toLowerCase().includes(q),
  );
});

const selectedBook = computed(() => booksCatalog.find(b => b.id === addSelectedBookId.value) || null);

function openAdd() {
  addQuery.value = '';
  addSelectedBookId.value = null;
  addRating.value = 0;
  addText.value = '';
  addOpen.value = true;
}

function setAddRating(value) {
  addRating.value = value;
}

function addReview() {
  if (!selectedBook.value) return;
  if (addRating.value <= 0) return;
  const text = addText.value.trim();
  if (!text) return;

  const b = selectedBook.value;
  const id = `rev-${Math.random().toString(36).slice(2, 9)}`;
  reviews.unshift({
    id,
    bookId: b.id,
    bookTitle: b.title,
    author: b.author,
    genre: b.genre,
    coverUrl: b.coverUrl,
    rating: addRating.value,
    text,
  });
  addOpen.value = false;
}

// Удаление рецензии
function deleteReview(id) {
  if (!confirm('Удалить эту рецензию?')) return;

  const index = reviews.findIndex(r => r.id === id);
  if (index !== -1) {
    reviews.splice(index, 1);
  }

  if (editingId.value === id) {
    editingId.value = null;
    isEditing.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between gap-6 mb-8">
        <h1 class="text-4xl font-bold text-black">Мои рецензии</h1>
        <UButton class="bg-white text-black rounded-xl" size="lg" @click="openAdd"> Добавить рецензию </UButton>
      </div>

      <!-- Список рецензий -->
      <div v-if="reviews.length" class="space-y-6">
        <UCard
          v-for="review in reviews"
          :key="review.id"
          variant="soft"
          class="bg-white rounded-[36px] shadow-lg border border-white/60 overflow-hidden transition-all hover:shadow-xl"
        >
          <div class="grid grid-cols-1 lg:grid-cols-[240px_1fr] gap-6 p-6">
            <div class="flex items-center justify-center">
              <div class="w-full max-w-[200px] aspect-2/3 rounded-2xl bg-gray-100 overflow-hidden shadow-md">
                <img class="w-full h-full object-cover" :src="review.coverUrl" :alt="review.bookTitle" />
              </div>
            </div>

            <div class="flex flex-col">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <div class="flex items-center gap-3 flex-wrap">
                    <h2 class="text-2xl font-bold text-black leading-tight">
                      {{ review.bookTitle }}
                    </h2>
                    <span
                      class="inline-flex px-4 py-1 text-sm font-medium rounded-full"
                      :class="getGenreColor(review.genre)"
                    >
                      {{ review.genre }}
                    </span>
                  </div>
                  <p class="text-md text-gray-800 mt-1">{{ review.author }}</p>
                </div>

                <div class="flex gap-2">
                  <UButton
                    size="sm"
                    variant="ghost"
                    color="red"
                    class="rounded-xl"
                    aria-label="Удалить"
                    @click="deleteReview(review.id)"
                  >
                    <svg viewBox="0 0 24 24" class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 6h18" />
                      <path d="M8 6V4h8v2" />
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
                    </svg>
                  </UButton>
                  <UButton
                    size="sm"
                    variant="ghost"
                    color="neutral"
                    class="rounded-xl"
                    aria-label="Редактировать"
                    @click="startEdit(review.id)"
                  >
                    <svg viewBox="0 0 24 24" class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z" />
                    </svg>
                  </UButton>
                </div>
              </div>

              <div class="mt-4 flex items-center gap-2">
                <button
                  v-for="i in 5"
                  :key="i"
                  type="button"
                  class="transition-transform"
                  :class="editingId === review.id && isEditing ? 'hover:scale-105 cursor-pointer' : 'cursor-default'"
                  :aria-label="`Оценка ${i}`"
                  @click="editingId === review.id && isEditing && setRating(i)"
                >
                  <svg
                    viewBox="0 0 24 24"
                    class="w-8 h-8"
                    :class="i <= review.rating ? 'text-yellow-300' : 'text-gray-200'"
                    fill="currentColor"
                  >
                    <path
                      d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"
                    />
                  </svg>
                </button>
              </div>

              <div class="mt-4 flex-1">
                <div v-if="editingId === review.id && isEditing" class="space-y-4">
                  <UTextarea
                    :model-value="review.text"
                    :rows="6"
                    class="w-full"
                    placeholder="Напишите вашу рецензию..."
                    @update:model-value="val => (review.text = val)"
                  />
                  <div class="flex items-center justify-end gap-4">
                    <UAlert
                      v-if="savedToast && editingId === review.id"
                      color="success"
                      variant="soft"
                      description="Сохранено"
                      class="flex-1"
                    />
                    <UButton
                      :loading="saving"
                      class="bg-green-300 text-black rounded-2xl px-6 disabled:opacity-50"
                      size="md"
                      @click="save"
                    >
                      Сохранить
                    </UButton>
                  </div>
                </div>
                <div v-else class="prose prose-sm max-w-none">
                  <p class="text-gray-700 whitespace-pre-wrap">{{ review.text }}</p>
                </div>
              </div>
            </div>
          </div>
        </UCard>
      </div>

      <div v-else class="bg-white rounded-2xl p-6 text-black">Рецензий пока нет.</div>
    </div>
  </div>

  <UModal v-model:open="addOpen" title="Добавить рецензию">
    <template #body>
      <div class="space-y-5">
        <div class="rounded-2xl border border-gray-200 bg-white p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-semibold text-black">Выберите книгу</h3>
            <p class="text-xs text-gray-500">Поиск по каталогу</p>
          </div>
          <UInput v-model="addQuery" placeholder="Название / автор / жанр..." class="w-full mb-3" />

          <div class="space-y-3 max-h-[320px] overflow-auto pr-1">
            <button
              v-for="b in filteredBooks"
              :key="b.id"
              type="button"
              class="w-full text-left p-3 rounded-xl border transition-colors"
              :class="
                addSelectedBookId === b.id ? 'border-black bg-gray-50' : 'border-gray-200 bg-white hover:bg-gray-50'
              "
              @click="addSelectedBookId = b.id"
            >
              <div class="flex items-start gap-3">
                <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl" :alt="b.title" />
                <div class="flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <span
                      class="inline-flex px-3 py-0.5 text-xs font-medium rounded-full"
                      :class="getGenreColor(b.genre)"
                    >
                      {{ b.genre }}
                    </span>
                  </div>
                  <div class="text-xs text-gray-600 mt-1">{{ b.author }}</div>
                </div>
              </div>
            </button>
            <div v-if="filteredBooks.length === 0" class="text-sm text-gray-500">Ничего не найдено.</div>
          </div>
        </div>

        <div class="rounded-2xl border border-gray-200 bg-white p-4 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-semibold text-black">Рецензия</h3>
            <p v-if="selectedBook" class="text-xs text-gray-500">
              {{ selectedBook.title }} — {{ selectedBook.author }}
            </p>
          </div>

          <div class="flex items-center gap-2">
            <button
              v-for="i in 5"
              :key="i"
              type="button"
              class="transition-transform hover:scale-105"
              :disabled="!selectedBook"
              :aria-label="`Оценка ${i}`"
              @click="setAddRating(i)"
            >
              <svg
                viewBox="0 0 24 24"
                class="w-9 h-9"
                :class="i <= addRating ? 'text-yellow-300' : 'text-gray-200'"
                fill="currentColor"
              >
                <path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" />
              </svg>
            </button>
          </div>

          <UTextarea
            v-model="addText"
            :rows="8"
            class="w-full"
            placeholder="Напишите вашу рецензию..."
            :disabled="!selectedBook"
          />
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="addOpen = false">Отмена</UButton>
        <UButton class="bg-green-300 text-black rounded-xl" @click="addReview"> Добавить </UButton>
      </div>
    </template>
  </UModal>
</template>
