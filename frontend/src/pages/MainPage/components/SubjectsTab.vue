<script setup>
import { computed, ref, watch, onMounted } from 'vue';
import { specialtiesData } from '../../../constants/studyData.js';
import { api } from '../../../api.js';
import CollectionsTab from './CollectionsTab.vue';

const selectedSpecialty = ref(null);
const selectedSubject = ref(null);

const specialties = computed(() => {
  const result = [];
  for (const [faculty, specs] of Object.entries(specialtiesData || {})) {
    for (const s of specs) {
      result.push({ faculty, value: s.value, label: s.label });
    }
  }
  return result;
});

const specialtiesGrid = computed(() => {
  const cols = 3;
  const out = [];
  for (let i = 0; i < specialties.value.length; i += cols) out.push(specialties.value.slice(i, i + cols));
  return out;
});

const subjectsList = ref([]);
const subjectsLoading = ref(false);
const subjectCollections = ref([]);
const booksForCollections = ref([]);
const collectionsLoading = ref(false);

const breadcrumbs = computed(() => {
  const items = [{ key: 'root', label: 'Специальности' }];
  if (selectedSpecialty.value) items.push({ key: 'spec', label: selectedSpecialty.value });
  if (selectedSubject.value) items.push({ key: 'sub', label: selectedSubject.value });
  return items;
});

function resetSubjects() {
  selectedSpecialty.value = null;
  selectedSubject.value = null;
  subjectsList.value = [];
  subjectCollections.value = [];
  booksForCollections.value = [];
}

function goBreadcrumb(key) {
  if (key === 'root') return resetSubjects();
  if (key === 'spec') {
    selectedSubject.value = null;
    subjectCollections.value = [];
    booksForCollections.value = [];
  }
}

watch(selectedSpecialty, async specialty => {
  selectedSubject.value = null;
  subjectCollections.value = [];
  booksForCollections.value = [];
  if (!specialty) return;
  subjectsLoading.value = true;
  subjectsList.value = [];
  try {
    const data = await api.get(`/subjects?specialty=${encodeURIComponent(specialty)}`);
    subjectsList.value = (data || []).map(s => s.name);
  } catch (e) {
    console.error('Ошибка загрузки предметов:', e);
  } finally {
    subjectsLoading.value = false;
  }
});

async function loadSubjectCollections() {
  if (!selectedSubject.value || !selectedSpecialty.value) return;
  collectionsLoading.value = true;
  try {
    const data = await api.get(
      `/subject-collections?subject=${encodeURIComponent(selectedSubject.value)}&specialty=${encodeURIComponent(selectedSpecialty.value)}&size=50`,
    );
    subjectCollections.value = data.content || [];
    const allIds = [...new Set(subjectCollections.value.flatMap(c => c.bookIds || []))];
    if (allIds.length > 0) {
      booksForCollections.value = await api.post('/books/by-ids', { ids: allIds });
    } else {
      booksForCollections.value = [];
    }
  } catch (e) {
    console.error('Ошибка загрузки подборок:', e);
  } finally {
    collectionsLoading.value = false;
  }
}

watch(selectedSubject, () => {
  subjectCollections.value = [];
  booksForCollections.value = [];
  if (!selectedSubject.value || !selectedSpecialty.value) return;
  loadSubjectCollections();
});

// ---- Add collection modal ----
const addOpen = ref(false);
const addTitle = ref('');
const addDescription = ref('');
const addQuery = ref('');
const addSelectedIds = ref([]);
const addCatalog = ref([]);
const addSubmitting = ref(false);
const addError = ref(null);
const addSuccess = ref(false);

async function loadCatalogForAdd() {
  if (addCatalog.value.length > 0) return;
  try {
    const data = await api.get('/books?size=200');
    addCatalog.value = data.content || [];
  } catch (e) {
    console.error('Ошибка загрузки каталога:', e);
  }
}

const filteredAddCatalog = computed(() => {
  const q = addQuery.value.trim().toLowerCase();
  const base = addCatalog.value.filter(b => !addSelectedIds.value.includes(b.id));
  if (!q) return base;
  return base.filter(b =>
    (b.title || '').toLowerCase().includes(q) ||
    (b.author || '').toLowerCase().includes(q),
  );
});

const selectedAddBooks = computed(() => addSelectedIds.value
  .map(id => addCatalog.value.find(b => b.id === id))
  .filter(Boolean));

function openAdd() {
  addTitle.value = '';
  addDescription.value = '';
  addQuery.value = '';
  addSelectedIds.value = [];
  addError.value = null;
  addSuccess.value = false;
  addOpen.value = true;
  loadCatalogForAdd();
}

async function submitAdd() {
  addError.value = null;
  if (!addTitle.value.trim()) { addError.value = 'Введите название подборки'; return; }
  if (addSelectedIds.value.length === 0) { addError.value = 'Добавьте хотя бы одну книгу'; return; }
  addSubmitting.value = true;
  try {
    await api.post('/subject-collections', {
      subject: selectedSubject.value,
      specialty: selectedSpecialty.value,
      specialtyName: selectedSpecialty.value,
      title: addTitle.value.trim(),
      description: addDescription.value.trim() || null,
      bookIds: addSelectedIds.value,
    });
    addSuccess.value = true;
    setTimeout(() => { addOpen.value = false; }, 1500);
  } catch (e) {
    addError.value = e.message || 'Не удалось отправить подборку';
  } finally {
    addSubmitting.value = false;
  }
}
</script>

<template>
  <div class="mb-4 flex items-center gap-2 text-sm text-black/70">
    <button
      v-for="(b, idx) in breadcrumbs"
      :key="b.key"
      type="button"
      class="hover:underline"
      :class="idx === breadcrumbs.length - 1 ? 'text-black font-semibold cursor-default no-underline' : ''"
      :disabled="idx === breadcrumbs.length - 1"
      @click="goBreadcrumb(b.key)"
    >
      {{ b.label }}<span v-if="idx !== breadcrumbs.length - 1" class="mx-2 text-black/40">/</span>
    </button>
  </div>

  <!-- 1) Специальности -->
  <UCard v-if="!selectedSpecialty" variant="soft" class="bg-white rounded-2xl p-5">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold text-black">Специальности</h2>
      <div class="text-xs text-gray-500">Выберите специальность</div>
    </div>

    <div class="overflow-auto">
      <table class="min-w-full text-sm">
        <tbody>
          <tr v-for="(row, rIdx) in specialtiesGrid" :key="rIdx" class="border-t border-gray-100">
            <td v-for="s in row" :key="s.value" class="py-3 pr-4 align-top">
              <button
                type="button"
                class="w-full text-left rounded-xl border border-gray-200 hover:bg-gray-50 px-3 py-2"
                @click="selectedSpecialty = s.value"
              >
                <div class="font-semibold text-black">{{ s.label }}</div>
                <div class="text-xs text-gray-500 mt-0.5">{{ s.faculty }}</div>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </UCard>

  <!-- 2) Предметы специальности -->
  <UCard v-else-if="selectedSpecialty && !selectedSubject" variant="soft" class="bg-white rounded-2xl p-5">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold text-black">Предметы</h2>
      <div class="text-xs text-gray-500">Нажмите на предмет</div>
    </div>

    <div v-if="subjectsLoading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>
    <div v-else-if="subjectsList.length === 0" class="text-sm text-gray-500 text-center py-4">
      Для этой специальности предметы ещё не добавлены администратором
    </div>
    <div v-else class="overflow-auto">
      <table class="min-w-full text-sm">
        <tbody>
          <tr
            v-for="subject in subjectsList"
            :key="subject"
            class="border-t border-gray-100 hover:bg-gray-50 cursor-pointer"
            @click="selectedSubject = subject"
          >
            <td class="py-3 pr-4 font-semibold text-black">{{ subject }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </UCard>

  <!-- 3) Подборки по предмету -->
  <div v-else class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="text-sm text-gray-700">
        Подборки по предмету: <span class="font-semibold text-black">{{ selectedSubject }}</span>
      </div>
      <UButton color="primary" class="rounded-xl" @click="openAdd">+ Добавить подборку</UButton>
    </div>

    <div v-if="collectionsLoading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>
    <CollectionsTab v-else :collections="subjectCollections" :books="booksForCollections" />
  </div>

  <UModal v-model:open="addOpen" title="Добавить подборку" class="z-100">
    <template #body>
      <div class="space-y-4">
        <UAlert
          v-if="addSuccess"
          color="success"
          variant="soft"
          description="Подборка отправлена на модерацию администратору."
        />
        <p class="text-sm text-gray-600">
          Предмет: <span class="font-semibold text-black">{{ selectedSubject }}</span>,
          специальность: <span class="font-semibold text-black">{{ selectedSpecialty }}</span>
        </p>

        <UFormField label="Название подборки">
          <UInput v-model="addTitle" placeholder="Например: Базовый курс по предмету" />
        </UFormField>

        <UFormField label="Описание">
          <UTextarea v-model="addDescription" :rows="3" placeholder="Что объединяет книги в подборке" />
        </UFormField>

        <div class="rounded-2xl border border-gray-200 bg-white p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-semibold text-black">Книги</h3>
            <p class="text-xs text-gray-500">Выбрано: {{ addSelectedIds.length }}</p>
          </div>
          <UInput v-model="addQuery" placeholder="Поиск по названию или автору..." class="w-full mb-3" />

          <div v-if="addSelectedIds.length" class="mb-3 flex flex-wrap gap-2">
            <button
              v-for="b in selectedAddBooks"
              :key="b.id"
              type="button"
              class="px-2 py-1 rounded-full bg-blue-100 text-blue-700 text-xs hover:bg-blue-200"
              @click="addSelectedIds = addSelectedIds.filter(id => id !== b.id)"
            >
              {{ b.title }} ✕
            </button>
          </div>

          <div class="space-y-2 max-h-[280px] overflow-auto pr-1">
            <button
              v-for="b in filteredAddCatalog"
              :key="b.id"
              type="button"
              class="w-full text-left p-2 rounded-xl border border-gray-200 bg-white hover:bg-gray-50"
              @click="addSelectedIds = [...addSelectedIds, b.id]"
            >
              <div class="flex items-start gap-3">
                <img class="w-10 h-14 object-cover rounded bg-gray-100 flex-none" :src="b.coverUrl || b.imageUrl || ''" :alt="b.title" />
                <div>
                  <div class="font-semibold text-black text-sm line-clamp-1">{{ b.title }}</div>
                  <div class="text-xs text-gray-600 mt-0.5">{{ b.author }}</div>
                </div>
              </div>
            </button>
            <div v-if="filteredAddCatalog.length === 0" class="text-sm text-gray-500 text-center py-2">
              Все книги добавлены
            </div>
          </div>
        </div>

        <UAlert v-if="addError" color="error" variant="soft" :description="addError" />
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="addOpen = false">Отмена</UButton>
        <UButton :loading="addSubmitting" class="bg-green-300 text-black rounded-xl" @click="submitAdd">
          Отправить на модерацию
        </UButton>
      </div>
    </template>
  </UModal>
</template>
