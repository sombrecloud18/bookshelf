<script setup>
import { computed, ref, onMounted } from 'vue';
import { getGenreColor } from '../../constants/genreColors';
import { api } from '../../api.js';

// ── Data ────────────────────────────────────────────────────────────────────
const collections = ref([]);
const subjectCollections = ref([]);
const allBooks = ref([]);
const loading = ref(true);

async function loadAll() {
  loading.value = true;
  try {
    const [myCollections, mySubjectCollections, booksPage] = await Promise.all([
      api.get('/collections/my'),
      api.get('/subject-collections/my'),
      api.get('/books?size=200'),
    ]);
    collections.value = myCollections || [];
    subjectCollections.value = mySubjectCollections || [];
    allBooks.value = booksPage.content || [];
  } catch (e) {
    console.error('Ошибка загрузки:', e);
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);

async function deleteSubjectCollection(sc) {
  if (!window.confirm(`Удалить учебную подборку «${sc.title}»? Действие необратимо.`)) return;
  try {
    await api.delete(`/subject-collections/${sc.id}`);
    subjectCollections.value = subjectCollections.value.filter(x => x.id !== sc.id);
  } catch (e) {
    alert(e.message || 'Не удалось удалить');
  }
}

const booksById = computed(() => Object.fromEntries(allBooks.value.map(b => [b.id, b])));

// ── Modal state ──────────────────────────────────────────────────────────────
const createOpen = ref(false);
const editOpen = ref(false);
const viewOpen = ref(false);

const editingCollectionId = ref(null);
const viewingCollectionId = ref(null);

// Draft state: create
const createTitle = ref('');
const createGenre = ref('');
const createDescription = ref('');
const createSelectedIds = ref([]);
const createCatalogQuery = ref('');
const createSaving = ref(false);

// Draft state: edit
const editTitle = ref('');
const editGenre = ref('');
const editDescription = ref('');
const editSelectedIds = ref([]);
const editCatalogQuery = ref('');
const editSaving = ref(false);

// ── Helpers ──────────────────────────────────────────────────────────────────
const selectedBooks = ids => (ids || []).map(id => booksById.value[id]).filter(Boolean);

const availableBooks = selectedIds => allBooks.value.filter(b => !selectedIds.includes(b.id));

const filteredCatalogForCreate = computed(() => {
  const q = createCatalogQuery.value.trim().toLowerCase();
  const base = availableBooks(createSelectedIds.value);
  if (!q) return base;
  return base.filter(
    b =>
      (b.title || '').toLowerCase().includes(q) ||
      (b.description || '').toLowerCase().includes(q) ||
      (b.author || '').toLowerCase().includes(q),
  );
});

const filteredCatalogForEdit = computed(() => {
  const q = editCatalogQuery.value.trim().toLowerCase();
  const base = availableBooks(editSelectedIds.value);
  if (!q) return base;
  return base.filter(
    b =>
      (b.title || '').toLowerCase().includes(q) ||
      (b.description || '').toLowerCase().includes(q) ||
      (b.author || '').toLowerCase().includes(q),
  );
});

// ── Drag & Drop ──────────────────────────────────────────────────────────────
function onDragStart(e, payload) {
  if (!e.dataTransfer) return;
  e.dataTransfer.effectAllowed = 'move';
  e.dataTransfer.setData('application/json', JSON.stringify(payload));
}

function readDragPayload(e) {
  try {
    const raw = e.dataTransfer?.getData('application/json');
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function clampIndex(index, length) {
  return Math.max(0, Math.min(index, length));
}

function addToSelected(selectedIdsRef, bookId, toIndex = null) {
  if (selectedIdsRef.value.includes(bookId)) return;
  const next = [...selectedIdsRef.value];
  const idx = toIndex === null ? next.length : clampIndex(toIndex, next.length);
  next.splice(idx, 0, bookId);
  selectedIdsRef.value = next;
}

function removeFromSelected(selectedIdsRef, bookId) {
  selectedIdsRef.value = selectedIdsRef.value.filter(id => id !== bookId);
}

function handleDropToSelectedIndex(selectedIdsRef, toIndex, e) {
  const payload = readDragPayload(e);
  if (!payload) return;
  const bookId = payload.bookId;
  const current = [...selectedIdsRef.value];
  const clampedTo = clampIndex(toIndex, current.length);

  if (payload.from === 'available') {
    if (current.includes(bookId)) return;
    current.splice(clampedTo, 0, bookId);
    selectedIdsRef.value = current;
    return;
  }

  const fromIndex = payload.fromIndex ?? current.indexOf(bookId);
  if (fromIndex === -1 || fromIndex === clampedTo) return;

  current.splice(fromIndex, 1);
  const insertionIndex = fromIndex < clampedTo ? clampedTo - 1 : clampedTo;
  const safeInsertionIndex = clampIndex(insertionIndex, current.length);
  current.splice(safeInsertionIndex, 0, bookId);
  selectedIdsRef.value = current;
}

function handleDropToSelectedEnd(selectedIdsRef, e) {
  handleDropToSelectedIndex(selectedIdsRef, selectedIdsRef.value.length, e);
}

function handleDropToAvailable(selectedIdsRef, e) {
  const payload = readDragPayload(e);
  if (!payload || payload.from !== 'selected') return;
  removeFromSelected(selectedIdsRef, payload.bookId);
}

// Create wrappers
const addToCreateSelected = (bookId, toIndex = null) => addToSelected(createSelectedIds, bookId, toIndex);
const removeFromCreateSelected = bookId => removeFromSelected(createSelectedIds, bookId);
const handleCreateDropToAvailable = e => handleDropToAvailable(createSelectedIds, e);
const handleCreateDropToSelectedEnd = e => handleDropToSelectedEnd(createSelectedIds, e);
const handleCreateDropToSelectedIndex = (toIndex, e) => handleDropToSelectedIndex(createSelectedIds, toIndex, e);

// Edit wrappers
const addToEditSelected = (bookId, toIndex = null) => addToSelected(editSelectedIds, bookId, toIndex);
const removeFromEditSelected = bookId => removeFromSelected(editSelectedIds, bookId);
const handleEditDropToAvailable = e => handleDropToAvailable(editSelectedIds, e);
const handleEditDropToSelectedEnd = e => handleDropToSelectedEnd(editSelectedIds, e);
const handleEditDropToSelectedIndex = (toIndex, e) => handleDropToSelectedIndex(editSelectedIds, toIndex, e);

// ── CRUD ─────────────────────────────────────────────────────────────────────
const viewingCollection = computed(() => collections.value.find(c => c.id === viewingCollectionId.value));

function resetCreateDraft() {
  createTitle.value = '';
  createGenre.value = '';
  createDescription.value = '';
  createSelectedIds.value = [];
  createCatalogQuery.value = '';
}

function openCreate() {
  resetCreateDraft();
  createOpen.value = true;
}

function openEdit(collectionId) {
  const c = collections.value.find(x => x.id === collectionId);
  if (!c) return;
  editingCollectionId.value = collectionId;
  editTitle.value = c.title;
  editGenre.value = c.genre || '';
  editDescription.value = c.description || '';
  editSelectedIds.value = [...(c.bookIds || [])];
  editCatalogQuery.value = '';
  editOpen.value = true;
}

function openView(collectionId) {
  viewingCollectionId.value = collectionId;
  viewOpen.value = true;
}

async function saveEdit() {
  if (!editingCollectionId.value) return;
  const nextTitle = editTitle.value.trim();
  if (!nextTitle) return;
  editSaving.value = true;
  try {
    const updated = await api.put(`/collections/${editingCollectionId.value}`, {
      title: nextTitle,
      genre: editGenre.value.trim() || null,
      description: editDescription.value.trim() || null,
      bookIds: editSelectedIds.value,
    });
    collections.value = collections.value.map(c => c.id === editingCollectionId.value ? updated : c);
    editOpen.value = false;
  } catch (e) {
    console.error('Ошибка сохранения:', e);
    alert(e.message || 'Не удалось сохранить');
  } finally {
    editSaving.value = false;
  }
}

// ── Subject collection edit ─────────────────────────────────────────────────
const subjectEditOpen = ref(false);
const subjectEditTarget = ref(null);
const subjectEditTitle = ref('');
const subjectEditDescription = ref('');
const subjectEditQuery = ref('');
const subjectEditSelectedIds = ref([]);
const subjectEditSaving = ref(false);
const subjectEditError = ref(null);

const filteredCatalogForSubjectEdit = computed(() => {
  const q = subjectEditQuery.value.trim().toLowerCase();
  const base = availableBooks(subjectEditSelectedIds.value);
  if (!q) return base;
  return base.filter(
    b =>
      (b.title || '').toLowerCase().includes(q) ||
      (b.author || '').toLowerCase().includes(q) ||
      (b.description || '').toLowerCase().includes(q),
  );
});

function openSubjectEdit(sc) {
  subjectEditTarget.value = sc;
  subjectEditTitle.value = sc.title;
  subjectEditDescription.value = sc.description || '';
  subjectEditSelectedIds.value = [...(sc.bookIds || [])];
  subjectEditQuery.value = '';
  subjectEditError.value = null;
  subjectEditOpen.value = true;
}

async function saveSubjectEdit() {
  const target = subjectEditTarget.value;
  if (!target) return;
  const nextTitle = subjectEditTitle.value.trim();
  if (!nextTitle) {
    subjectEditError.value = 'Введите название подборки';
    return;
  }
  if (subjectEditSelectedIds.value.length === 0) {
    subjectEditError.value = 'Добавьте хотя бы одну книгу';
    return;
  }
  subjectEditSaving.value = true;
  subjectEditError.value = null;
  try {
    const updated = await api.put(`/subject-collections/${target.id}`, {
      subject: target.subject,
      specialty: target.specialty,
      specialtyName: target.specialtyName || target.specialty,
      title: nextTitle,
      description: subjectEditDescription.value.trim() || null,
      bookIds: subjectEditSelectedIds.value,
    });
    subjectCollections.value = subjectCollections.value.map(x => x.id === target.id ? updated : x);
    subjectEditOpen.value = false;
  } catch (e) {
    subjectEditError.value = e.message || 'Не удалось сохранить';
  } finally {
    subjectEditSaving.value = false;
  }
}

async function deleteCollectionById(collectionId) {
  const target = collections.value.find(c => c.id === collectionId);
  const name = target?.title ? `«${target.title}»` : 'эту подборку';
  if (!window.confirm(`Удалить ${name}? Это действие нельзя отменить.`)) return;
  try {
    await api.delete(`/collections/${collectionId}`);
    collections.value = collections.value.filter(c => c.id !== collectionId);
    if (editingCollectionId.value === collectionId) editOpen.value = false;
    if (viewingCollectionId.value === collectionId) viewOpen.value = false;
  } catch (e) {
    console.error('Ошибка удаления:', e);
  }
}

const publishingId = ref(null);
const publishError = ref(null);

async function publishCollection(collectionId) {
  const collection = collections.value.find(c => c.id === collectionId);
  if (!collection) return;
  if ((collection.bookIds || []).length === 0) {
    publishError.value = 'Сначала добавьте книги в подборку';
    return;
  }
  if (!window.confirm(`Отправить «${collection.title}» на модерацию?`)) return;
  publishingId.value = collectionId;
  publishError.value = null;
  try {
    const updated = await api.post(`/collections/${collectionId}/publish`);
    collections.value = collections.value.map(c => c.id === collectionId ? updated : c);
  } catch (e) {
    publishError.value = e.message || 'Не удалось отправить на модерацию';
  } finally {
    publishingId.value = null;
  }
}

function statusLabel(status) {
  switch (status) {
    case 'DRAFT': return 'Черновик';
    case 'PENDING': return 'На модерации';
    case 'APPROVED': return 'Опубликовано';
    case 'REJECTED': return 'Отклонено';
    default: return status || 'Черновик';
  }
}

function statusClass(status) {
  switch (status) {
    case 'DRAFT': return 'bg-gray-100 text-gray-700';
    case 'PENDING': return 'bg-yellow-100 text-yellow-700';
    case 'APPROVED': return 'bg-green-100 text-green-700';
    case 'REJECTED': return 'bg-red-100 text-red-700';
    default: return 'bg-gray-100 text-gray-700';
  }
}

function isEditable(status) {
  // PENDING is locked while moderator reviews; everything else is editable.
  // Editing an APPROVED collection sends it back through moderation on the backend.
  return status !== 'PENDING';
}

function isPublishable(status) {
  // Only DRAFT and REJECTED need a manual "publish" action; APPROVED already lives in moderation flow.
  return status === 'DRAFT' || status === 'REJECTED';
}

async function createCollection() {
  const nextTitle = createTitle.value.trim();
  if (!nextTitle) return;
  createSaving.value = true;
  try {
    const created = await api.post('/collections', {
      title: nextTitle,
      genre: createGenre.value.trim() || null,
      description: createDescription.value.trim() || null,
      bookIds: createSelectedIds.value,
    });
    collections.value = [...collections.value, created];
    createOpen.value = false;
  } catch (e) {
    console.error('Ошибка создания:', e);
  } finally {
    createSaving.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between gap-6 mb-8">
        <h1 class="text-4xl font-bold text-black">Мои подборки</h1>
        <UButton class="bg-white text-black rounded-xl" size="lg" @click="openCreate"> Создать подборку </UButton>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <div
        v-else-if="collections.length"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr"
      >
        <UCard
          v-for="c in collections"
          :key="c.id"
          variant="soft"
          class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white flex flex-col h-full"
        >
          <div class="flex flex-col">
            <div class="flex items-center justify-between gap-3 h-8">
              <span
                v-if="c.genre"
                class="inline-flex px-4 py-1 text-sm font-medium rounded-md"
                :class="getGenreColor(c.genre)"
              >
                {{ c.genre }}
              </span>
              <span class="inline-flex px-3 py-0.5 text-xs font-medium rounded-full" :class="statusClass(c.status)">
                {{ statusLabel(c.status) }}
              </span>
            </div>

            <div class="flex min-h-16 items-center justify-center">
              <div class="w-full">
                <h2 class="text-lg font-extrabold text-center line-clamp-2 text-black min-h-12">
                  {{ c.title }}
                </h2>
                <p v-if="c.description" class="mt-2 text-sm text-gray-700 text-center line-clamp-2 min-h-10">
                  {{ c.description }}
                </p>
                <p class="mt-2 text-xs text-gray-500 text-center">Книг: {{ (c.bookIds || []).length }}</p>
                <p v-if="c.status === 'REJECTED' && c.moderatorComment" class="mt-2 text-xs text-red-700 bg-red-50 p-2 rounded">
                  <span class="font-semibold">Причина отказа:</span> {{ c.moderatorComment }}
                </p>
              </div>
            </div>
          </div>

          <div class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center">
            <div class="relative h-[80%] w-[78%]">
              <div v-if="(c.bookIds || []).length === 0" class="text-xs text-gray-500 text-center">Нет книг</div>

              <template v-else>
                <img
                  v-for="(b, idx) in selectedBooks((c.bookIds || []).slice(-2)).reverse()"
                  :key="b.id"
                  class="absolute h-full rounded-2xl shadow-lg border border-white bg-white/90 p-1"
                  :class="idx === 0 ? 'object-contain' : 'object-cover'"
                  :style="{
                    right: `${idx * 22}px`,
                    top: `calc(50% + ${idx * 10}px)`,
                    transform: 'translateY(-50%)',
                    zIndex: 10 + idx,
                  }"
                  :src="b.coverUrl || b.imageUrl"
                  :alt="b.title"
                />
              </template>
            </div>
          </div>

          <div class="flex flex-wrap gap-2 justify-center mt-auto pt-4">
            <UButton
              v-if="isEditable(c.status)"
              size="sm"
              color="primary"
              class="flex-1 justify-center"
              @click="openEdit(c.id)"
            >
              Редактировать
            </UButton>
            <UButton size="sm" variant="outline" class="flex-1 justify-center" @click="openView(c.id)">
              Подробнее
            </UButton>
            <UButton
              v-if="isPublishable(c.status)"
              size="sm"
              color="green"
              :loading="publishingId === c.id"
              :disabled="(c.bookIds || []).length === 0"
              class="w-full justify-center"
              @click="publishCollection(c.id)"
            >
              Отправить на модерацию
            </UButton>
            <UButton
              v-if="c.status !== 'DRAFT'"
              size="sm"
              color="error"
              variant="soft"
              class="w-full justify-center"
              @click="deleteCollectionById(c.id)"
            >
              Удалить
            </UButton>
          </div>
        </UCard>
      </div>

      <UAlert v-if="publishError" color="error" variant="soft" :description="publishError" class="mt-4" />

      <div v-if="!loading && collections.length === 0" class="bg-white rounded-2xl p-6 text-black">Подборок пока нет. Создайте первую.</div>

      <!-- Мои учебные подборки (subject collections) -->
      <div class="mt-12">
        <div class="flex items-center justify-between gap-4 mb-6">
          <h2 class="text-3xl font-bold text-black">Мои учебные подборки</h2>
          <UButton variant="outline" class="rounded-xl" to="/?tab=subjects">Создать на вкладке «Предметы»</UButton>
        </div>

        <div
          v-if="!loading && subjectCollections.length"
          class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr"
        >
          <UCard
            v-for="sc in subjectCollections"
            :key="sc.id"
            variant="soft"
            class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white flex flex-col h-full"
          >
            <div class="flex flex-col">
              <div class="flex items-center justify-between gap-2">
                <span class="inline-flex px-3 py-0.5 text-xs font-medium rounded-full bg-blue-100 text-blue-700">
                  {{ sc.specialty }}
                </span>
                <span class="inline-flex px-3 py-0.5 text-xs font-medium rounded-full" :class="statusClass(sc.status)">
                  {{ statusLabel(sc.status) }}
                </span>
              </div>

              <div class="mt-3">
                <p class="text-xs text-gray-500">Предмет:</p>
                <p class="font-semibold text-black break-words" style="overflow-wrap: anywhere">{{ sc.subject }}</p>
              </div>

              <h3 class="mt-3 text-lg font-extrabold text-black line-clamp-2 break-words" style="overflow-wrap: anywhere">
                {{ sc.title }}
              </h3>

              <p
                v-if="sc.description"
                class="mt-2 text-sm text-gray-700 line-clamp-3 break-words"
                style="overflow-wrap: anywhere"
              >
                {{ sc.description }}
              </p>

              <p class="mt-2 text-xs text-gray-500">Книг: {{ (sc.bookIds || []).length }}</p>

              <p
                v-if="sc.status === 'REJECTED' && sc.moderatorComment"
                class="mt-2 text-xs text-red-700 bg-red-50 p-2 rounded break-words"
                style="overflow-wrap: anywhere"
              >
                <span class="font-semibold">Причина отказа:</span> {{ sc.moderatorComment }}
              </p>
            </div>

            <div class="flex flex-wrap gap-2 justify-center mt-auto pt-4">
              <UButton
                v-if="sc.status !== 'PENDING'"
                size="sm"
                color="primary"
                class="flex-1 justify-center"
                @click="openSubjectEdit(sc)"
              >
                Редактировать
              </UButton>
              <UButton size="sm" color="error" variant="soft" class="flex-1 justify-center" @click="deleteSubjectCollection(sc)">
                Удалить
              </UButton>
              <p
                v-if="sc.status === 'APPROVED'"
                class="w-full text-xs text-gray-500 text-center mt-1"
              >
                Изменения отправят подборку на повторную модерацию.
              </p>
            </div>
          </UCard>
        </div>

        <div
          v-else-if="!loading"
          class="bg-white rounded-2xl p-6 text-black text-center"
        >
          У вас пока нет учебных подборок. Создать новую можно во вкладке
          <router-link to="/?tab=subjects" class="underline">«Предметы»</router-link>.
        </div>
      </div>
    </div>
  </div>

  <!-- Create modal -->
  <UModal v-model:open="createOpen" title="Создать подборку" class="z-100">
    <template #body>
      <div class="space-y-5">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <UFormField label="Название" class="w-full">
            <UInput v-model="createTitle" placeholder="Например: Фэнтези для холодных вечеров" class="w-full" />
          </UFormField>
          <UFormField label="Жанр подборки" class="w-full">
            <UInput v-model="createGenre" placeholder="Например: Фантастика" class="w-full" />
          </UFormField>
          <UFormField label="Описание" class="w-full sm:col-span-2">
            <UTextarea
              v-model="createDescription"
              placeholder="Кратко о том, что объединяет книги в подборке"
              class="w-full"
            />
          </UFormField>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="rounded-2xl border border-gray-200 bg-white p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-black">Каталог</h3>
              <p class="text-xs text-gray-500">Ищите и добавляйте</p>
            </div>

            <UInput v-model="createCatalogQuery" placeholder="Поиск по названию или автору..." class="w-full mb-3" />

            <div
              class="space-y-3 max-h-[360px] overflow-auto pr-1"
              @dragover.prevent
              @drop.prevent="e => handleCreateDropToAvailable(e)"
            >
              <div
                v-for="b in filteredCatalogForCreate"
                :key="b.id"
                class="p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50"
                draggable="true"
                @dragstart="e => onDragStart(e, { bookId: b.id, from: 'available' })"
              >
                <div class="flex items-start gap-3">
                  <img
                    class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                    :src="b.coverUrl || b.imageUrl || ''"
                    :alt="b.title"
                  />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.description }}</div>
                    <div class="mt-2">
                      <UButton size="xs" variant="outline" @click="addToCreateSelected(b.id)"> Добавить </UButton>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="filteredCatalogForCreate.length === 0" class="text-sm text-gray-500">Ничего не найдено.</div>
            </div>
          </div>

          <div
            class="rounded-2xl border border-gray-200 bg-white p-4"
            @dragover.prevent
            @drop.prevent="e => handleCreateDropToSelectedEnd(e)"
          >
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-black">В подборке</h3>
              <p class="text-xs text-gray-500">Перетащите для порядка</p>
            </div>

            <div v-if="createSelectedIds.length" class="space-y-3 max-h-[360px] overflow-auto pr-1">
              <div
                v-for="(b, idx) in selectedBooks(createSelectedIds)"
                :key="b.id"
                class="p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50"
                draggable="true"
                @dragstart="e => onDragStart(e, { bookId: b.id, from: 'selected', fromIndex: idx })"
                @dragover.prevent
                @drop.prevent="e => handleCreateDropToSelectedIndex(idx, e)"
              >
                <div class="flex items-start gap-3">
                  <img
                    class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                    :src="b.coverUrl || b.imageUrl || ''"
                    :alt="b.title"
                  />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.description }}</div>
                    <div class="mt-2 flex gap-2">
                      <UButton size="xs" color="red" variant="soft" @click="removeFromCreateSelected(b.id)">
                        Удалить
                      </UButton>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="text-sm text-gray-500">Добавьте книги, чтобы создать подборку.</div>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="createOpen = false">Отмена</UButton>
        <UButton :loading="createSaving" class="bg-green-300 text-black rounded-xl" @click="createCollection"> Создать </UButton>
      </div>
    </template>
  </UModal>

  <!-- Edit modal -->
  <UModal v-model:open="editOpen" title="Редактировать подборку" class="z-100">
    <template #body>
      <div class="space-y-5">
        <UAlert
          v-if="(collections.find(c => c.id === editingCollectionId) || {}).status === 'APPROVED'"
          color="warning"
          variant="soft"
          description="После сохранения подборка снова отправится на модерацию администратора."
        />
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <UFormField label="Название" class="w-full">
            <UInput v-model="editTitle" placeholder="Название подборки" class="w-full" />
          </UFormField>
          <UFormField label="Жанр подборки" class="w-full">
            <UInput v-model="editGenre" placeholder="Жанр" class="w-full" />
          </UFormField>
          <UFormField label="Описание" class="w-full sm:col-span-2">
            <UTextarea v-model="editDescription" placeholder="Описание подборки" class="w-full" />
          </UFormField>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="rounded-2xl border border-gray-200 bg-white p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-black">Каталог</h3>
              <p class="text-xs text-gray-500">Ищите и добавляйте</p>
            </div>

            <UInput v-model="editCatalogQuery" placeholder="Поиск по названию или автору..." class="w-full mb-3" />

            <div
              class="space-y-3 max-h-[360px] overflow-auto pr-1"
              @dragover.prevent
              @drop.prevent="e => handleEditDropToAvailable(e)"
            >
              <div
                v-for="b in filteredCatalogForEdit"
                :key="b.id"
                class="p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50"
                draggable="true"
                @dragstart="e => onDragStart(e, { bookId: b.id, from: 'available' })"
              >
                <div class="flex items-start gap-3">
                  <img
                    class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                    :src="b.coverUrl || b.imageUrl || ''"
                    :alt="b.title"
                  />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.description }}</div>
                    <div class="mt-2">
                      <UButton size="xs" variant="outline" @click="addToEditSelected(b.id)"> Добавить </UButton>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="filteredCatalogForEdit.length === 0" class="text-sm text-gray-500">Ничего не найдено.</div>
            </div>
          </div>

          <div
            class="rounded-2xl border border-gray-200 bg-white p-4"
            @dragover.prevent
            @drop.prevent="e => handleEditDropToSelectedEnd(e)"
          >
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-black">В подборке</h3>
              <p class="text-xs text-gray-500">Удаляйте или меняйте порядок</p>
            </div>

            <div v-if="editSelectedIds.length" class="space-y-3 max-h-[360px] overflow-auto pr-1">
              <div
                v-for="(b, idx) in selectedBooks(editSelectedIds)"
                :key="b.id"
                class="p-3 rounded-xl border border-gray-200 bg-white hover:bg-gray-50"
                draggable="true"
                @dragstart="e => onDragStart(e, { bookId: b.id, from: 'selected', fromIndex: idx })"
                @dragover.prevent
                @drop.prevent="e => handleEditDropToSelectedIndex(idx, e)"
              >
                <div class="flex items-start gap-3">
                  <img
                    class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                    :src="b.coverUrl || b.imageUrl || ''"
                    :alt="b.title"
                  />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.description }}</div>
                    <div class="mt-2 flex gap-2">
                      <UButton size="xs" color="red" variant="soft" @click="removeFromEditSelected(b.id)">
                        Удалить
                      </UButton>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="text-sm text-gray-500">В этой подборке пока нет книг.</div>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex items-center justify-between gap-3 w-full">
        <UButton
          color="error"
          variant="soft"
          class="rounded-xl"
          @click="editingCollectionId && deleteCollectionById(editingCollectionId)"
        >
          Удалить подборку
        </UButton>

        <div class="flex justify-end gap-3">
          <UButton variant="outline" @click="editOpen = false">Отмена</UButton>
          <UButton :loading="editSaving" class="bg-green-300 text-black rounded-xl" @click="saveEdit"> Сохранить </UButton>
        </div>
      </div>
    </template>
  </UModal>

  <!-- Subject collection edit modal -->
  <UModal v-model:open="subjectEditOpen" title="Редактировать учебную подборку" class="z-100">
    <template #body>
      <div v-if="subjectEditTarget" class="space-y-5">
        <UAlert
          v-if="subjectEditTarget.status === 'APPROVED'"
          color="warning"
          variant="soft"
          description="После сохранения подборка снова отправится на модерацию администратора."
        />
        <p class="text-sm text-gray-600">
          Предмет: <span class="font-semibold text-black">{{ subjectEditTarget.subject }}</span>,
          специальность: <span class="font-semibold text-black">{{ subjectEditTarget.specialty }}</span>
        </p>

        <UFormField label="Название подборки">
          <UInput v-model="subjectEditTitle" placeholder="Название подборки" />
        </UFormField>
        <UFormField label="Описание">
          <UTextarea v-model="subjectEditDescription" :rows="3" placeholder="Что объединяет книги в подборке" />
        </UFormField>

        <div class="rounded-2xl border border-gray-200 bg-white p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-semibold text-black">Книги</h3>
            <p class="text-xs text-gray-500">Выбрано: {{ subjectEditSelectedIds.length }}</p>
          </div>
          <UInput v-model="subjectEditQuery" placeholder="Поиск по названию или автору..." class="w-full mb-3" />

          <div v-if="subjectEditSelectedIds.length" class="mb-3 flex flex-wrap gap-2">
            <button
              v-for="b in selectedBooks(subjectEditSelectedIds)"
              :key="b.id"
              type="button"
              class="px-2 py-1 rounded-full bg-blue-100 text-blue-700 text-xs hover:bg-blue-200"
              @click="subjectEditSelectedIds = subjectEditSelectedIds.filter(id => id !== b.id)"
            >
              {{ b.title }} ✕
            </button>
          </div>

          <div class="space-y-2 max-h-[280px] overflow-auto pr-1">
            <button
              v-for="b in filteredCatalogForSubjectEdit"
              :key="b.id"
              type="button"
              class="w-full text-left p-2 rounded-xl border border-gray-200 bg-white hover:bg-gray-50"
              @click="subjectEditSelectedIds = [...subjectEditSelectedIds, b.id]"
            >
              <div class="flex items-start gap-3">
                <img class="w-10 h-14 object-cover rounded bg-gray-100 flex-none" :src="b.coverUrl || b.imageUrl || ''" :alt="b.title" />
                <div>
                  <div class="font-semibold text-black text-sm line-clamp-1">{{ b.title }}</div>
                  <div class="text-xs text-gray-600 mt-0.5">{{ b.author }}</div>
                </div>
              </div>
            </button>
            <div v-if="filteredCatalogForSubjectEdit.length === 0" class="text-sm text-gray-500 text-center py-2">
              Все книги добавлены
            </div>
          </div>
        </div>

        <UAlert v-if="subjectEditError" color="error" variant="soft" :description="subjectEditError" />
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="subjectEditOpen = false">Отмена</UButton>
        <UButton :loading="subjectEditSaving" class="bg-green-300 text-black rounded-xl" @click="saveSubjectEdit">
          Сохранить
        </UButton>
      </div>
    </template>
  </UModal>

  <!-- View modal -->
  <UModal v-model:open="viewOpen" title="Подборка" class="z-100">
    <template #body>
      <div v-if="viewingCollection" class="space-y-4">
        <div class="bg-white rounded-2xl border border-gray-200 p-4">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h2 class="text-2xl font-bold text-black">{{ viewingCollection.title }}</h2>
              <p class="mt-2 text-sm text-gray-700">{{ viewingCollection.description }}</p>
            </div>
            <div class="text-xs text-gray-500 mt-1">Книг: {{ (viewingCollection.bookIds || []).length }}</div>
          </div>
        </div>

        <div class="space-y-3 max-h-[420px] overflow-auto pr-1">
          <div
            v-for="b in selectedBooks(viewingCollection.bookIds)"
            :key="b.id"
            class="p-3 rounded-xl border border-gray-200 bg-white"
          >
            <div class="flex items-start gap-3">
              <img
                class="w-12 h-16 object-cover rounded-md flex-none bg-gray-100"
                :src="b.coverUrl || b.imageUrl || ''"
                :alt="b.title"
              />
              <div>
                <div class="font-semibold text-black">{{ b.title }}</div>
                <div class="text-xs text-gray-600 mt-1">{{ b.description }}</div>
              </div>
            </div>
          </div>

          <div v-if="(viewingCollection.bookIds || []).length === 0" class="text-sm text-gray-500">
            В подборке пока нет книг.
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="viewOpen = false">Закрыть</UButton>
      </div>
    </template>
  </UModal>
</template>
