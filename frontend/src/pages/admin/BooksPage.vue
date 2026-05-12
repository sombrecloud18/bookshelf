<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { api } from '../../api.js';
import FileUpload from '../../components/FileUpload.vue';
import { useConfirm } from '../../composables/useConfirm.js';

const askConfirm = useConfirm();

const books = ref([]);
const loading = ref(true);
const query = ref('');
const includeArchived = ref(true);

const editOpen = ref(false);
const editingId = ref(null);
const saving = ref(false);
const formError = ref(null);

const form = reactive({
  title: '',
  author: '',
  genre: '',
  year: null,
  description: '',
  fullDescription: '',
  coverUrl: '',
  imageUrl: '',
  pages: null,
  publisher: '',
  publishYear: null,
  isbn: '',
});

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase();
  return books.value.filter(b => {
    if (!includeArchived.value && b.status === 'ARCHIVED') return false;
    if (!q) return true;
    return (b.title || '').toLowerCase().includes(q) ||
      (b.author || '').toLowerCase().includes(q) ||
      (b.isbn || '').toLowerCase().includes(q);
  });
});

async function loadBooks() {
  loading.value = true;
  try {
    const data = await api.get(`/books?size=200&includeArchived=true`);
    books.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки книг:', e);
  } finally {
    loading.value = false;
  }
}

onMounted(loadBooks);

function resetForm() {
  Object.assign(form, {
    title: '', author: '', genre: '', year: null, description: '', fullDescription: '',
    coverUrl: '', imageUrl: '', pages: null, publisher: '', publishYear: null, isbn: '',
  });
  formError.value = null;
}

function openCreate() {
  editingId.value = null;
  resetForm();
  editOpen.value = true;
}

function openEdit(book) {
  editingId.value = book.id;
  Object.assign(form, {
    title: book.title || '',
    author: book.author || '',
    genre: book.genre || '',
    year: book.year ?? null,
    description: book.description || '',
    fullDescription: book.fullDescription || '',
    coverUrl: book.coverUrl || '',
    imageUrl: book.imageUrl || '',
    pages: book.pages ?? null,
    publisher: book.publisher || '',
    publishYear: book.publishYear ?? null,
    isbn: book.isbn || '',
  });
  formError.value = null;
  editOpen.value = true;
}

function onCoverUploaded(url) {
  form.coverUrl = url;
  if (!form.imageUrl) form.imageUrl = url;
}

async function save() {
  formError.value = null;
  if (!form.title.trim() || !form.author.trim()) {
    formError.value = 'Название и автор обязательны';
    return;
  }
  saving.value = true;
  try {
    const payload = {
      title: form.title.trim(),
      author: form.author.trim(),
      genre: form.genre.trim() || null,
      year: form.year || null,
      description: form.description.trim() || null,
      fullDescription: form.fullDescription.trim() || null,
      coverUrl: form.coverUrl.trim() || null,
      imageUrl: form.imageUrl.trim() || form.coverUrl.trim() || null,
      pages: form.pages || null,
      publisher: form.publisher.trim() || null,
      publishYear: form.publishYear || null,
      isbn: form.isbn.trim() || null,
    };
    if (editingId.value) {
      const updated = await api.put(`/books/${editingId.value}`, payload);
      books.value = books.value.map(b => b.id === updated.id ? updated : b);
    } else {
      const created = await api.post('/books', payload);
      books.value = [created, ...books.value];
    }
    editOpen.value = false;
  } catch (e) {
    formError.value = e.message || 'Не удалось сохранить';
  } finally {
    saving.value = false;
  }
}

async function archive(book) {
  const ok = await askConfirm(`Архивировать «${book.title}»? Книга перестанет быть видна пользователям.`, {
    title: 'Архивация книги',
    confirmLabel: 'Архивировать',
    variant: 'primary',
  });
  if (!ok) return;
  try {
    const updated = await api.post(`/books/${book.id}/archive`);
    books.value = books.value.map(b => b.id === updated.id ? updated : b);
  } catch (e) {
    alert(e.message || 'Не удалось архивировать');
  }
}

async function restore(book) {
  try {
    const updated = await api.post(`/books/${book.id}/restore`);
    books.value = books.value.map(b => b.id === updated.id ? updated : b);
  } catch (e) {
    alert(e.message || 'Не удалось восстановить');
  }
}

async function deleteBook(book) {
  const ok = await askConfirm(`Удалить книгу «${book.title}»? Каскадно удалятся все рецензии, заказы и упоминания в подборках. Действие необратимо.`, {
    title: 'Удаление книги',
    confirmLabel: 'Удалить',
  });
  if (!ok) return;
  try {
    await api.delete(`/books/${book.id}`);
    books.value = books.value.filter(b => b.id !== book.id);
  } catch (e) {
    alert(e.message || 'Не удалось удалить');
  }
}
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-8 flex-wrap gap-3">
        <div>
          <h1 class="text-4xl font-bold text-black">Управление книгами</h1>
          <p class="text-gray-700 mt-2">Всего книг: {{ books.length }}</p>
        </div>
        <div class="flex items-center gap-3">
          <UButton color="primary" class="rounded-xl" @click="openCreate">+ Добавить книгу</UButton>
          <UButton to="/admin" variant="ghost" class="rounded-xl bg-white hover:!bg-black hover:!text-white">← Назад</UButton>
        </div>
      </div>

      <UCard variant="soft" class="bg-white rounded-2xl p-5 mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <UInput v-model="query" icon="i-lucide-search" placeholder="Поиск по названию, автору, ISBN..." class="flex-1 min-w-[260px]" />
          <label class="flex items-center gap-2 text-sm text-black">
            <input type="checkbox" v-model="includeArchived" />
            Показывать архивные
          </label>
        </div>
      </UCard>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <UCard v-else variant="soft" class="bg-white rounded-2xl p-5">
        <div v-if="filtered.length === 0" class="text-center py-8 text-gray-500">
          Книг не найдено
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="b in filtered"
            :key="b.id"
            class="flex items-start gap-4 p-3 rounded-xl border border-gray-200 hover:bg-gray-50"
            :class="b.status === 'ARCHIVED' ? 'opacity-60' : ''"
          >
            <img
              class="w-16 h-24 object-cover rounded-md flex-none bg-gray-100"
              :src="b.coverUrl || b.imageUrl || ''"
              :alt="b.title"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="font-semibold text-black">{{ b.title }}</h3>
                <span v-if="b.genre" class="text-xs px-2 py-0.5 rounded-full bg-gray-100">{{ b.genre }}</span>
                <span v-if="b.status === 'ARCHIVED'" class="text-xs px-2 py-0.5 rounded-full bg-orange-100 text-orange-800">архив</span>
              </div>
              <p class="text-sm text-gray-600">{{ b.author }} • {{ b.year || '—' }}</p>
              <p class="text-xs text-gray-400">ISBN: {{ b.isbn || '—' }}</p>
            </div>
            <div class="flex flex-col gap-2">
              <UButton size="xs" color="primary" variant="soft" @click="openEdit(b)">Изменить</UButton>
              <UButton v-if="b.status === 'ACTIVE'" size="xs" color="error" variant="soft" @click="archive(b)">Скрыть</UButton>
              <UButton v-else size="xs" color="success" variant="soft" @click="restore(b)">Вернуть</UButton>
              <UButton size="xs" color="error" variant="soft" @click="deleteBook(b)">Удалить</UButton>
            </div>
          </div>
        </div>
      </UCard>

      <UModal v-model:open="editOpen" class="z-100">
        <template #body>
          <div class="space-y-4 max-h-[70vh] overflow-auto">
            <h2 class="text-2xl font-bold text-black">{{ editingId ? 'Редактировать книгу' : 'Добавить книгу' }}</h2>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <UFormField label="Название">
                <UInput v-model="form.title" placeholder="Например: Мастер и Маргарита" class="w-full" />
              </UFormField>
              <UFormField label="Автор">
                <UInput v-model="form.author" placeholder="Например: Михаил Булгаков" class="w-full" />
              </UFormField>
              <UFormField label="Жанр">
                <UInput v-model="form.genre" placeholder="Например: Роман" class="w-full" />
              </UFormField>
              <UFormField label="Год">
                <UInput v-model.number="form.year" type="number" placeholder="Год написания" class="w-full" />
              </UFormField>
              <UFormField label="Издательство">
                <UInput v-model="form.publisher" placeholder="Например: АСТ" class="w-full" />
              </UFormField>
              <UFormField label="Год издания">
                <UInput v-model.number="form.publishYear" type="number" placeholder="Год публикации" class="w-full" />
              </UFormField>
              <UFormField label="Страниц">
                <UInput v-model.number="form.pages" type="number" placeholder="Количество страниц" class="w-full" />
              </UFormField>
              <UFormField label="ISBN">
                <UInput v-model="form.isbn" placeholder="Например: 978-5-17-038942-4" class="w-full" />
              </UFormField>
            </div>

            <UFormField label="Краткое описание">
              <UTextarea v-model="form.description" :rows="2" placeholder="Одно-два предложения для карточки книги в каталоге" class="w-full" />
            </UFormField>
            <UFormField label="Полное описание">
              <UTextarea v-model="form.fullDescription" :rows="4" placeholder="Развёрнутое описание для страницы книги" class="w-full" />
            </UFormField>

            <UFormField label="Обложка">
              <FileUpload v-model="form.coverUrl" scope="covers" label="Загрузить обложку" @uploaded="onCoverUploaded" />
            </UFormField>

            <UAlert v-if="formError" color="error" variant="soft" :description="formError" />
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-3 w-full">
            <UButton variant="outline" class="bg-white hover:!text-white" @click="editOpen = false">Отмена</UButton>
            <UButton :loading="saving" color="primary" @click="save">Сохранить</UButton>
          </div>
        </template>
      </UModal>
    </div>
  </div>
</template>
