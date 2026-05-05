<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getGenreColor } from '../../constants/genreColors';
import { api } from '../../api.js';

const router = useRouter();

const reviews = ref([]);
const loading = ref(true);

const editingId = ref(null);
const isEditing = ref(false);
const saving = ref(false);
const savedToast = ref(false);
const saveError = ref(null);
let savedTimer = null;

// Draft for inline editing
const editDraft = reactive({ rating: 0, text: '' });

function formatDate(dateStr) {
  if (!dateStr) return '';
  return String(dateStr).split('T')[0];
}

onMounted(async () => {
  try {
    const data = await api.get('/reviews/my?size=50');
    reviews.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки рецензий:', e);
  } finally {
    loading.value = false;
  }
});

function startEdit(reviewId) {
  if (isEditing.value && editingId.value !== reviewId) {
    if (!confirm('Есть несохранённые изменения. Продолжить без сохранения?')) return;
  }
  const review = reviews.value.find(r => r.id === reviewId);
  if (!review) return;
  editingId.value = reviewId;
  editDraft.rating = review.rating;
  editDraft.text = review.text;
  isEditing.value = true;
  saveError.value = null;
}

function setRating(value) {
  if (!isEditing.value) return;
  editDraft.rating = value;
}

async function save() {
  if (!isEditing.value) return;
  if (editDraft.text.trim().length < 200) {
    saveError.value = 'Рецензия должна содержать не менее 200 символов';
    return;
  }
  saving.value = true;
  saveError.value = null;
  try {
    const updated = await api.put(`/reviews/${editingId.value}`, {
      rating: editDraft.rating,
      text: editDraft.text.trim(),
    });
    const idx = reviews.value.findIndex(r => r.id === editingId.value);
    if (idx !== -1) reviews.value[idx] = updated;
    savedToast.value = true;
    isEditing.value = false;
    if (savedTimer) window.clearTimeout(savedTimer);
    savedTimer = window.setTimeout(() => { savedToast.value = false; }, 2500);
  } catch (e) {
    saveError.value = e.message || 'Ошибка сохранения';
  } finally {
    saving.value = false;
  }
}

async function deleteReview(id) {
  if (!confirm('Удалить эту рецензию?')) return;
  try {
    await api.delete(`/reviews/${id}`);
    reviews.value = reviews.value.filter(r => r.id !== id);
    if (editingId.value === id) {
      editingId.value = null;
      isEditing.value = false;
    }
  } catch (e) {
    console.error('Ошибка удаления:', e);
  }
}

// Add review modal
const addOpen = ref(false);
const addQuery = ref('');
const addSelectedBookId = ref(null);
const addRating = ref(0);
const addText = ref('');
const addError = ref(null);
const addSubmitting = ref(false);
const addSubmitted = ref(false);

const booksCatalog = ref([]);

async function loadCatalog() {
  if (booksCatalog.value.length > 0) return;
  try {
    const data = await api.get('/books?size=100');
    booksCatalog.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки каталога:', e);
  }
}

const filteredBooks = computed(() => {
  const q = addQuery.value.trim().toLowerCase();
  const base = booksCatalog.value;
  if (!q) return base;
  return base.filter(
    b =>
      (b.title || '').toLowerCase().includes(q) ||
      (b.author || '').toLowerCase().includes(q) ||
      (b.genre || '').toLowerCase().includes(q),
  );
});

const selectedBook = computed(() => booksCatalog.value.find(b => b.id === addSelectedBookId.value) || null);

async function openAdd() {
  addQuery.value = '';
  addSelectedBookId.value = null;
  addRating.value = 0;
  addText.value = '';
  addError.value = null;
  addSubmitted.value = false;
  addOpen.value = true;
  await loadCatalog();
}

function setAddRating(value) {
  addRating.value = value;
}

async function addReview() {
  addError.value = null;
  if (!selectedBook.value) { addError.value = 'Выберите книгу'; return; }
  if (addRating.value <= 0) { addError.value = 'Поставьте оценку'; return; }
  const text = addText.value.trim();
  if (!text) { addError.value = 'Напишите текст рецензии'; return; }
  if (text.length < 200) { addError.value = 'Рецензия должна содержать не менее 200 символов'; return; }

  addSubmitting.value = true;
  try {
    await api.post('/reviews', {
      bookId: addSelectedBookId.value,
      rating: addRating.value,
      text,
    });
    addSubmitted.value = true;
    addSelectedBookId.value = null;
    addRating.value = 0;
    addText.value = '';
    // Reload reviews to pick up the new pending one
    const data = await api.get('/reviews/my?size=50');
    reviews.value = data.content || [];
    setTimeout(() => { addOpen.value = false; }, 1500);
  } catch (e) {
    addError.value = e.message || 'Ошибка при отправке';
  } finally {
    addSubmitting.value = false;
  }
}

function goToBook(bookId) {
  router.push(`/book/${bookId}`);
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between gap-6 mb-8">
        <h1 class="text-4xl font-bold text-black">Мои рецензии</h1>
        <UButton class="bg-white text-black rounded-xl" size="lg" @click="openAdd"> Добавить рецензию </UButton>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <!-- Список рецензий -->
      <div v-else-if="reviews.length" class="space-y-6">
        <UCard
          v-for="review in reviews"
          :key="review.id"
          variant="soft"
          class="bg-white rounded-[36px] shadow-lg border border-white/60 overflow-hidden transition-all hover:shadow-xl"
        >
          <div class="grid grid-cols-1 lg:grid-cols-[240px_1fr] gap-6 p-6">
            <div class="flex items-center justify-center">
              <div
                class="w-full max-w-[200px] aspect-2/3 rounded-2xl bg-gray-100 overflow-hidden shadow-md cursor-pointer hover:opacity-90 transition-opacity"
                @click="goToBook(review.bookId)"
              >
                <img
                  class="w-full h-full object-cover"
                  :src="review.coverUrl || ''"
                  :alt="review.bookTitle"
                />
              </div>
            </div>

            <div class="flex flex-col">
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center gap-3 flex-wrap">
                    <h2
                      class="text-2xl font-bold text-black leading-tight cursor-pointer hover:text-blue-600 transition-colors"
                      @click="goToBook(review.bookId)"
                    >
                      {{ review.bookTitle }}
                    </h2>
                    <span
                      class="inline-flex px-4 py-1 text-sm font-medium rounded-full"
                      :class="getGenreColor(review.genre)"
                    >
                      {{ review.genre }}
                    </span>
                    <span
                      v-if="review.status && review.status !== 'APPROVED'"
                      class="inline-flex px-3 py-0.5 text-xs font-medium rounded-full"
                      :class="review.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'"
                    >
                      {{ review.status === 'PENDING' ? 'На модерации' : 'Отклонено' }}
                    </span>
                  </div>
                  <p class="text-md text-gray-800 mt-1">{{ review.author }}</p>
                  <p class="text-xs text-gray-400 mt-1">{{ formatDate(review.createdAt) }}</p>
                </div>

                <div class="flex gap-2">
                  <UButton
                    size="sm"
                    variant="ghost"
                    color="primary"
                    class="rounded-xl"
                    aria-label="Подробнее о книге"
                    @click="goToBook(review.bookId)"
                  >
                    <svg viewBox="0 0 24 24" class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2">
                      <path
                        d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                      />
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
                    :class="(editingId === review.id && isEditing ? i <= editDraft.rating : i <= review.rating) ? 'text-yellow-300' : 'text-gray-200'"
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
                    v-model="editDraft.text"
                    :rows="6"
                    class="w-full"
                    placeholder="Напишите вашу рецензию..."
                  />
                  <p class="text-xs text-gray-400">{{ editDraft.text.length }} / 200</p>
                  <UAlert v-if="saveError" color="error" variant="soft" :description="saveError" />
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

              <div class="mt-4 pt-2 flex justify-end">
                <UButton size="sm" variant="outline" class="rounded-xl" @click="goToBook(review.bookId)">
                  Подробнее о книге →
                </UButton>
              </div>
            </div>
          </div>
        </UCard>
      </div>

      <div v-else class="bg-white rounded-2xl p-6 text-black text-center">
        <p class="text-lg">У вас пока нет рецензий</p>
        <UButton class="mt-4 bg-white text-black rounded-xl" variant="outline" @click="openAdd">
          Написать первую рецензию
        </UButton>
      </div>
    </div>
  </div>

  <UModal v-model:open="addOpen" title="Добавить рецензию">
    <template #body>
      <div class="space-y-5">
        <UAlert
          v-if="addSubmitted"
          color="success"
          variant="soft"
          description="Рецензия отправлена на модерацию!"
        />

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
                <img
                  class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                  :src="b.imageUrl || b.coverUrl || ''"
                  :alt="b.title"
                />
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
            placeholder="Напишите вашу рецензию (минимум 200 символов)..."
            :disabled="!selectedBook"
          />
          <p class="text-xs text-gray-400">{{ addText.length }} / 200</p>

          <UAlert v-if="addError" color="error" variant="soft" :description="addError" />
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="addOpen = false">Отмена</UButton>
        <UButton :loading="addSubmitting" class="bg-green-300 text-black rounded-xl" @click="addReview"> Добавить </UButton>
      </div>
    </template>
  </UModal>
</template>
