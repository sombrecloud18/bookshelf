<script setup>
import { computed, ref, watch } from 'vue';
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
    const data = await api.get(`/subject-collections?specialty=${encodeURIComponent(specialty)}&size=100`);
    const cols = data.content || [];
    const seen = new Set();
    const subjects = [];
    for (const c of cols) {
      if (c.subject && !seen.has(c.subject)) {
        seen.add(c.subject);
        subjects.push(c.subject);
      }
    }
    subjectsList.value = subjects;
  } catch (e) {
    console.error('Ошибка загрузки предметов:', e);
  } finally {
    subjectsLoading.value = false;
  }
});

watch(selectedSubject, async subject => {
  subjectCollections.value = [];
  booksForCollections.value = [];
  if (!subject || !selectedSpecialty.value) return;
  collectionsLoading.value = true;
  try {
    const data = await api.get(
      `/subject-collections?subject=${encodeURIComponent(subject)}&specialty=${encodeURIComponent(selectedSpecialty.value)}&size=50`,
    );
    subjectCollections.value = data.content || [];
    const allIds = [...new Set(subjectCollections.value.flatMap(c => c.bookIds || []))];
    if (allIds.length > 0) {
      booksForCollections.value = await api.post('/books/by-ids', { ids: allIds });
    }
  } catch (e) {
    console.error('Ошибка загрузки подборок:', e);
  } finally {
    collectionsLoading.value = false;
  }
});
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
      Нет предметов для этой специальности
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
    <div class="text-sm text-gray-700">
      Подборки по предмету: <span class="font-semibold text-black">{{ selectedSubject }}</span>
    </div>
    <div v-if="collectionsLoading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>
    <CollectionsTab v-else :collections="subjectCollections" :books="booksForCollections" />
  </div>
</template>
