<script setup>
import { computed, ref } from 'vue';
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

const collections = ref([
  {
    id: 'col-1',
    title: 'Любимые романы',
    genre: 'Роман',
    description: 'Подборка для вечеров, когда хочется глубины и атмосферы.',
    bookIds: ['book-master', 'book-satan', 'book-kwebe'],
  },
  {
    id: 'col-2',
    title: 'Фантастика: технологии и выбор',
    genre: 'Фантастика',
    description: 'Книги, которые заставляют думать о будущем и людях.',
    bookIds: ['book-ave-maria', 'book-institute', 'book-bookship'],
  },
]);

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

// Draft state: edit
const editTitle = ref('');
const editGenre = ref('');
const editDescription = ref('');
const editSelectedIds = ref([]);
const editCatalogQuery = ref('');

const selectedBooks = ids => ids.map(id => booksById[id]).filter(Boolean);
const availableBooks = selectedIds => allBooks.filter(b => !selectedIds.includes(b.id));

const filteredCatalogForCreate = computed(() => {
  const q = createCatalogQuery.value.trim().toLowerCase();
  const base = availableBooks(createSelectedIds.value);
  if (!q) return base;
  return base.filter(b => b.title.toLowerCase().includes(q) || b.shortDescription.toLowerCase().includes(q));
});

const filteredCatalogForEdit = computed(() => {
  const q = editCatalogQuery.value.trim().toLowerCase();
  const base = availableBooks(editSelectedIds.value);
  if (!q) return base;
  return base.filter(b => b.title.toLowerCase().includes(q) || b.shortDescription.toLowerCase().includes(q));
});

const makeId = () => Math.random().toString(36).slice(2, 9);

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

  // From selected: reorder
  const fromIndex = payload.fromIndex ?? current.indexOf(bookId);
  if (fromIndex === -1) return;
  if (fromIndex === clampedTo) return;

  current.splice(fromIndex, 1);
  // After removing, indexes shift when the dragged item comes before the target.
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
  if (!payload) return;

  if (payload.from !== 'selected') return;
  removeFromSelected(selectedIdsRef, payload.bookId);
}

// Wrappers for template
function addToCreateSelected(bookId, toIndex = null) {
  addToSelected(createSelectedIds, bookId, toIndex);
}
function removeFromCreateSelected(bookId) {
  removeFromSelected(createSelectedIds, bookId);
}
function handleCreateDropToAvailable(e) {
  handleDropToAvailable(createSelectedIds, e);
}
function handleCreateDropToSelectedEnd(e) {
  handleDropToSelectedEnd(createSelectedIds, e);
}
function handleCreateDropToSelectedIndex(toIndex, e) {
  handleDropToSelectedIndex(createSelectedIds, toIndex, e);
}

function addToEditSelected(bookId, toIndex = null) {
  addToSelected(editSelectedIds, bookId, toIndex);
}
function removeFromEditSelected(bookId) {
  removeFromSelected(editSelectedIds, bookId);
}
function handleEditDropToAvailable(e) {
  handleDropToAvailable(editSelectedIds, e);
}
function handleEditDropToSelectedEnd(e) {
  handleDropToSelectedEnd(editSelectedIds, e);
}
function handleEditDropToSelectedIndex(toIndex, e) {
  handleDropToSelectedIndex(editSelectedIds, toIndex, e);
}

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
  editGenre.value = c.genre;
  editDescription.value = c.description;
  editSelectedIds.value = [...c.bookIds];
  editCatalogQuery.value = '';
  editOpen.value = true;
}

function openView(collectionId) {
  viewingCollectionId.value = collectionId;
  viewOpen.value = true;
}

function saveEdit() {
  if (!editingCollectionId.value) return;

  const nextTitle = editTitle.value.trim();
  if (!nextTitle) return;

  collections.value = collections.value.map(c => {
    if (c.id !== editingCollectionId.value) return c;
    return {
      ...c,
      title: nextTitle,
      genre: editGenre.value.trim(),
      description: editDescription.value.trim(),
      bookIds: [...editSelectedIds.value],
    };
  });

  editOpen.value = false;
}

function deleteCollectionById(collectionId) {
  const target = collections.value.find(c => c.id === collectionId);
  const name = target?.title ? `«${target.title}»` : 'эту подборку';

  const ok = window.confirm(`Удалить ${name}? Это действие нельзя отменить.`);
  if (!ok) return;

  collections.value = collections.value.filter(c => c.id !== collectionId);
  if (editingCollectionId.value === collectionId) {
    editOpen.value = false;
    editingCollectionId.value = null;
  }
  if (viewingCollectionId.value === collectionId) {
    viewOpen.value = false;
    viewingCollectionId.value = null;
  }
}

function createCollection() {
  const nextTitle = createTitle.value.trim();
  if (!nextTitle) return;

  const nextCollection = {
    id: `col-${makeId()}`,
    title: nextTitle,
    genre: createGenre.value.trim(),
    description: createDescription.value.trim(),
    bookIds: [...createSelectedIds.value],
  };

  collections.value = [...collections.value, nextCollection];
  createOpen.value = false;
}
</script>

<template>
  <!-- Template remains exactly the same -->
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between gap-6 mb-8">
        <h1 class="text-4xl font-bold text-black">Мои подборки</h1>
        <UButton class="bg-white text-black rounded-xl" size="lg" @click="openCreate"> Создать подборку </UButton>
      </div>

      <div
        v-if="collections.length"
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
            </div>

            <div class="flex min-h-16 items-center justify-center">
              <div class="w-full">
                <h2 class="text-lg font-extrabold text-center line-clamp-2 text-black min-h-12">
                  {{ c.title }}
                </h2>
                <p v-if="c.description" class="mt-2 text-sm text-gray-700 text-center line-clamp-2 min-h-10">
                  {{ c.description }}
                </p>
                <p class="mt-2 text-xs text-gray-500 text-center">Книг: {{ c.bookIds.length }}</p>
              </div>
            </div>
          </div>

          <div class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center">
            <div class="relative h-[80%] w-[78%]">
              <div v-if="c.bookIds.length === 0" class="text-xs text-gray-500 text-center">Нет книг</div>

              <template v-else>
                <img
                  v-for="(b, idx) in selectedBooks(c.bookIds.slice(-2)).reverse()"
                  :key="b.id"
                  class="absolute h-full rounded-2xl shadow-lg border border-white bg-white/90 p-1"
                  :class="idx === 0 ? 'object-contain' : 'object-cover'"
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

          <div class="flex gap-2 justify-center mt-auto pt-4">
            <UButton size="md" color="primary" class="flex-1 justify-center" @click="openEdit(c.id)">
              Редактировать
            </UButton>
            <UButton size="md" variant="outline" class="flex-1 justify-center" @click="openView(c.id)">
              Узнать подробнее
            </UButton>
          </div>
        </UCard>
      </div>

      <div v-else class="bg-white rounded-2xl p-6 text-black">Подборок пока нет. Создайте первую.</div>
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

            <UInput v-model="createCatalogQuery" placeholder="Поиск по названию или описанию..." class="w-full mb-3" />

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
                  <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl" :alt="b.title" />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.shortDescription }}</div>
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
                  <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl" :alt="b.title" />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.shortDescription }}</div>

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
        <UButton class="bg-green-300 text-black rounded-xl" @click="createCollection"> Создать </UButton>
      </div>
    </template>
  </UModal>

  <!-- Edit modal -->
  <UModal v-model:open="editOpen" title="Редактировать подборку" class="z-100">
    <template #body>
      <div class="space-y-5">
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

            <UInput v-model="editCatalogQuery" placeholder="Поиск по названию или описанию..." class="w-full mb-3" />

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
                  <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl" :alt="b.title" />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.shortDescription }}</div>
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
                  <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl" :alt="b.title" />
                  <div class="flex-1">
                    <div class="font-semibold text-black line-clamp-1">{{ b.title }}</div>
                    <div class="text-xs text-gray-600 line-clamp-2 mt-1">{{ b.shortDescription }}</div>

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
          <UButton class="bg-green-300 text-black rounded-xl" @click="saveEdit"> Сохранить </UButton>
        </div>
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
            <div class="text-xs text-gray-500 mt-1">Книг: {{ viewingCollection.bookIds.length }}</div>
          </div>
        </div>

        <div class="space-y-3 max-h-[420px] overflow-auto pr-1">
          <div
            v-for="b in selectedBooks(viewingCollection.bookIds)"
            :key="b.id"
            class="p-3 rounded-xl border border-gray-200 bg-white"
          >
            <div class="flex items-start gap-3">
              <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl" :alt="b.title" />
              <div>
                <div class="font-semibold text-black">{{ b.title }}</div>
                <div class="text-xs text-gray-600 mt-1">{{ b.shortDescription }}</div>
              </div>
            </div>
          </div>

          <div v-if="viewingCollection.bookIds.length === 0" class="text-sm text-gray-500">
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
