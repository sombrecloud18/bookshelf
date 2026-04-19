<script setup>
import { computed, ref, watch } from 'vue';
import { specialtiesData } from '../../../constants/studyData.js';
import CollectionsTab from './CollectionsTab.vue';

const props = defineProps({
  collections: { type: Array, default: () => [] },
  books: { type: Array, default: () => [] },
});

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

const subjectsBySpecialty = computed(() => {
  const base = [
    { id: 'sub-opi', title: 'ОПИ', teacher: 'Иванов И.И.', semester: '4 семестр' },
    { id: 'sub-db', title: 'БД', teacher: 'Петров П.П.', semester: '4 семестр' },
    { id: 'sub-net', title: 'Сети', teacher: 'Сидоров С.С.', semester: '5 семестр' },
    { id: 'sub-web', title: 'Web', teacher: 'Кузнецов К.К.', semester: '5 семестр' },
    { id: 'sub-math', title: 'Дискретная математика', teacher: 'Орлова О.О.', semester: '3 семестр' },
  ];
  const map = {};
  for (const s of specialties.value) map[s.value] = base;
  return map;
});

const currentSubjects = computed(() => {
  if (!selectedSpecialty.value) return [];
  return subjectsBySpecialty.value[selectedSpecialty.value] || [];
});

const subjectCollections = computed(() => {
  if (!selectedSubject.value) return [];
  return props.collections;
});

const breadcrumbs = computed(() => {
  const items = [{ key: 'root', label: 'Специальности' }];
  if (selectedSpecialty.value) items.push({ key: 'spec', label: selectedSpecialty.value });
  if (selectedSubject.value) items.push({ key: 'sub', label: selectedSubject.value });
  return items;
});

function resetSubjects() {
  selectedSpecialty.value = null;
  selectedSubject.value = null;
}

function goBreadcrumb(key) {
  if (key === 'root') return resetSubjects();
  if (key === 'spec') selectedSubject.value = null;
}

watch(
  () => props.collections,
  () => {
    // no-op placeholder; keeps reactivity explicit if later collections come from API
  },
);
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

    <div class="overflow-auto">
      <table class="min-w-full text-sm">
        <tbody>
          <tr
            v-for="s in currentSubjects"
            :key="s.id"
            class="border-t border-gray-100 hover:bg-gray-50 cursor-pointer"
            @click="selectedSubject = s.title"
          >
            <td class="py-3 pr-4 font-semibold text-black">{{ s.title }}</td>
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
    <CollectionsTab :collections="subjectCollections" :books="books" />
  </div>
</template>
