<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { api } from '../../api.js';
import { useFaculties } from '../../composables/useFaculties.js';
import { useConfirm } from '../../composables/useConfirm.js';

const askConfirm = useConfirm();

// ── Faculties + specialties (loaded via shared composable) ──────────────────
const { faculties, allSpecialties, refresh: refreshFaculties } = useFaculties();

// ── Subjects ────────────────────────────────────────────────────────────────
const subjects = ref([]);
const subjectsLoading = ref(true);

async function loadSubjects() {
  subjectsLoading.value = true;
  try {
    subjects.value = await api.get('/subjects');
  } catch (e) {
    console.error('Ошибка загрузки предметов:', e);
  } finally {
    subjectsLoading.value = false;
  }
}

onMounted(async () => {
  await Promise.all([refreshFaculties(), loadSubjects()]);
});

// ── Faculty modal ───────────────────────────────────────────────────────────
const facultyOpen = ref(false);
const facultyEditingId = ref(null);
const facultyName = ref('');
const facultySaving = ref(false);
const facultyError = ref(null);

function openFacultyCreate() {
  facultyEditingId.value = null;
  facultyName.value = '';
  facultyError.value = null;
  facultyOpen.value = true;
}

function openFacultyEdit(f) {
  facultyEditingId.value = f.id;
  facultyName.value = f.name;
  facultyError.value = null;
  facultyOpen.value = true;
}

async function saveFaculty() {
  facultyError.value = null;
  if (!facultyName.value.trim()) {
    facultyError.value = 'Введите название факультета';
    return;
  }
  facultySaving.value = true;
  try {
    const payload = { name: facultyName.value.trim() };
    if (facultyEditingId.value) {
      await api.put(`/admin/faculties/${facultyEditingId.value}`, payload);
    } else {
      await api.post('/admin/faculties', payload);
    }
    await refreshFaculties();
    facultyOpen.value = false;
  } catch (e) {
    facultyError.value = e.message || 'Не удалось сохранить';
  } finally {
    facultySaving.value = false;
  }
}

async function deleteFaculty(f) {
  const ok = await askConfirm(`Удалить факультет «${f.name}»? Все его специальности тоже будут удалены.`, {
    title: 'Удаление факультета',
    confirmLabel: 'Удалить',
  });
  if (!ok) return;
  try {
    await api.delete(`/admin/faculties/${f.id}`);
    await refreshFaculties();
  } catch (e) {
    alert(e.message || 'Не удалось удалить');
  }
}

// ── Specialty modal ─────────────────────────────────────────────────────────
const specialtyOpen = ref(false);
const specialtyEditingId = ref(null);
const specialtyForm = reactive({ facultyId: '', name: '' });
const specialtySaving = ref(false);
const specialtyError = ref(null);

function openSpecialtyCreate(faculty = null) {
  specialtyEditingId.value = null;
  specialtyForm.facultyId = faculty ? faculty.id : '';
  specialtyForm.name = '';
  specialtyError.value = null;
  specialtyOpen.value = true;
}

function openSpecialtyEdit(faculty, spec) {
  specialtyEditingId.value = spec.id;
  specialtyForm.facultyId = faculty.id;
  specialtyForm.name = spec.name;
  specialtyError.value = null;
  specialtyOpen.value = true;
}

async function saveSpecialty() {
  specialtyError.value = null;
  if (!specialtyForm.facultyId) {
    specialtyError.value = 'Выберите факультет';
    return;
  }
  if (!specialtyForm.name.trim()) {
    specialtyError.value = 'Введите название специальности';
    return;
  }
  specialtySaving.value = true;
  try {
    const payload = { facultyId: specialtyForm.facultyId, name: specialtyForm.name.trim() };
    if (specialtyEditingId.value) {
      await api.put(`/admin/specialties/${specialtyEditingId.value}`, payload);
    } else {
      await api.post('/admin/specialties', payload);
    }
    await refreshFaculties();
    specialtyOpen.value = false;
  } catch (e) {
    specialtyError.value = e.message || 'Не удалось сохранить';
  } finally {
    specialtySaving.value = false;
  }
}

async function deleteSpecialty(spec) {
  const ok = await askConfirm(`Удалить специальность «${spec.name}»? Подборки/связи с предметами для неё также пропадут.`, {
    title: 'Удаление специальности',
    confirmLabel: 'Удалить',
  });
  if (!ok) return;
  try {
    await api.delete(`/admin/specialties/${spec.id}`);
    await refreshFaculties();
    await loadSubjects();
  } catch (e) {
    alert(e.message || 'Не удалось удалить');
  }
}

// ── Subject modal ───────────────────────────────────────────────────────────
const subjectOpen = ref(false);
const subjectEditingId = ref(null);
const subjectForm = reactive({
  name: '',
  common: false,
  specialtyIds: [],
});
const subjectSaving = ref(false);
const subjectError = ref(null);

const subjectSpecialtiesGrouped = computed(() => {
  const out = [];
  for (const f of faculties.value) {
    out.push({
      id: f.id,
      name: f.name,
      specialties: (f.specialties || []).map(s => ({ id: s.id, name: s.name })),
    });
  }
  return out;
});

function openSubjectCreate() {
  subjectEditingId.value = null;
  subjectForm.name = '';
  subjectForm.common = false;
  subjectForm.specialtyIds = [];
  subjectError.value = null;
  subjectOpen.value = true;
}

function openSubjectEdit(s) {
  subjectEditingId.value = s.id;
  subjectForm.name = s.name;
  subjectForm.common = s.common;
  subjectForm.specialtyIds = [...(s.specialtyIds || [])];
  subjectError.value = null;
  subjectOpen.value = true;
}

function toggleSpecialtyInSubject(id) {
  const idx = subjectForm.specialtyIds.indexOf(id);
  if (idx === -1) subjectForm.specialtyIds = [...subjectForm.specialtyIds, id];
  else subjectForm.specialtyIds = subjectForm.specialtyIds.filter(x => x !== id);
}

function selectAllForFaculty(faculty) {
  const ids = faculty.specialties.map(s => s.id);
  const allIn = ids.every(id => subjectForm.specialtyIds.includes(id));
  if (allIn) {
    subjectForm.specialtyIds = subjectForm.specialtyIds.filter(id => !ids.includes(id));
  } else {
    subjectForm.specialtyIds = [...new Set([...subjectForm.specialtyIds, ...ids])];
  }
}

async function saveSubject() {
  subjectError.value = null;
  if (!subjectForm.name.trim()) {
    subjectError.value = 'Введите название предмета';
    return;
  }
  if (!subjectForm.common && subjectForm.specialtyIds.length === 0) {
    subjectError.value = 'Выберите хотя бы одну специальность или отметьте предмет общим';
    return;
  }
  subjectSaving.value = true;
  try {
    const payload = {
      name: subjectForm.name.trim(),
      common: subjectForm.common,
      specialtyIds: subjectForm.common ? [] : subjectForm.specialtyIds,
    };
    if (subjectEditingId.value) {
      await api.put(`/subjects/${subjectEditingId.value}`, payload);
    } else {
      await api.post('/subjects', payload);
    }
    await loadSubjects();
    subjectOpen.value = false;
  } catch (e) {
    subjectError.value = e.message || 'Не удалось сохранить';
  } finally {
    subjectSaving.value = false;
  }
}

async function deleteSubject(s) {
  const ok = await askConfirm(`Удалить предмет «${s.name}»? Связанные учебные подборки потеряют ссылку на предмет.`, {
    title: 'Удаление предмета',
    confirmLabel: 'Удалить',
  });
  if (!ok) return;
  try {
    await api.delete(`/subjects/${s.id}`);
    await loadSubjects();
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
          <h1 class="text-4xl font-bold text-black">Учебная система</h1>
          <p class="text-gray-700 mt-2">Факультеты, специальности и предметы</p>
        </div>
        <UButton to="/admin" variant="ghost" class="rounded-xl bg-white hover:!bg-black hover:!text-white">← Назад</UButton>
      </div>

      <!-- Faculties / Specialties -->
      <UCard variant="soft" class="bg-white rounded-2xl p-5 mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-2xl font-bold text-black">Факультеты и специальности</h2>
          <div class="flex gap-2">
            <UButton size="sm" color="primary" class="rounded-xl" @click="openFacultyCreate">
              + Факультет
            </UButton>
            <UButton size="sm" variant="outline" class="rounded-xl bg-white hover:!text-white" @click="() => openSpecialtyCreate()">
              + Специальность
            </UButton>
          </div>
        </div>

        <div v-if="faculties.length === 0" class="text-center py-8 text-gray-500">
          Факультеты ещё не добавлены
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <div
            v-for="f in faculties"
            :key="f.id"
            class="rounded-2xl border border-gray-200 bg-gray-50 p-4"
          >
            <div class="flex items-start justify-between gap-2">
              <h3 class="text-lg font-bold text-black">{{ f.name }}</h3>
              <div class="flex gap-1">
                <UButton size="xs" color="primary" variant="soft" @click="openFacultyEdit(f)">
                  Изменить
                </UButton>
                <UButton size="xs" color="error" variant="soft" @click="deleteFaculty(f)">
                  Удалить
                </UButton>
              </div>
            </div>
            <div class="mt-3 space-y-2">
              <div
                v-for="s in f.specialties || []"
                :key="s.id"
                class="flex items-center justify-between p-2 rounded-xl bg-white border border-gray-200 text-sm"
              >
                <span class="font-medium text-black">{{ s.name }}</span>
                <div class="flex gap-1">
                  <UButton size="xs" color="primary" variant="ghost" class="bg-white hover:!bg-black hover:!text-white" @click="openSpecialtyEdit(f, s)">
                    Изменить
                  </UButton>
                  <UButton size="xs" color="error" variant="ghost" class="bg-white" @click="deleteSpecialty(s)">
                    Удалить
                  </UButton>
                </div>
              </div>
              <div v-if="(f.specialties || []).length === 0" class="text-xs text-gray-500 italic">
                Нет специальностей
              </div>
              <UButton size="xs" variant="outline" class="w-full mt-2 bg-white hover:!text-white" @click="openSpecialtyCreate(f)">
                + Добавить специальность
              </UButton>
            </div>
          </div>
        </div>
      </UCard>

      <!-- Subjects -->
      <UCard variant="soft" class="bg-white rounded-2xl p-5">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-2xl font-bold text-black">Предметы</h2>
          <UButton size="sm" color="primary" class="rounded-xl" @click="openSubjectCreate">
            + Предмет
          </UButton>
        </div>

        <div v-if="subjectsLoading" class="flex justify-center py-8">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
        </div>

        <div v-else-if="subjects.length === 0" class="text-center py-8 text-gray-500">
          Предметы ещё не добавлены
        </div>

        <div v-else class="space-y-2">
          <div
            v-for="s in subjects"
            :key="s.id"
            class="flex items-start justify-between p-3 rounded-xl border border-gray-200 bg-white gap-3"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold text-black">{{ s.name }}</span>
                <span
                  v-if="s.common"
                  class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-800"
                >
                  Общий для всех
                </span>
              </div>
              <p v-if="!s.common" class="mt-1 text-xs text-gray-600">
                <template v-if="(s.specialtyLabels || []).length">
                  {{ (s.specialtyLabels || []).join(' • ') }}
                </template>
                <template v-else>
                  <span class="text-gray-400">Не привязан ни к одной специальности</span>
                </template>
              </p>
            </div>
            <div class="flex gap-1 flex-none">
              <UButton size="xs" color="primary" variant="soft" @click="openSubjectEdit(s)">Изменить</UButton>
              <UButton size="xs" color="error" variant="soft" @click="deleteSubject(s)">Удалить</UButton>
            </div>
          </div>
        </div>
      </UCard>
    </div>

    <!-- Faculty modal -->
    <UModal v-model:open="facultyOpen" class="z-100">
      <template #body>
        <div class="space-y-4">
          <h2 class="text-xl font-bold text-black">
            {{ facultyEditingId ? 'Редактировать факультет' : 'Новый факультет' }}
          </h2>
          <UFormField label="Название">
            <UInput v-model="facultyName" placeholder="Например: ФКП" />
          </UFormField>
          <UAlert v-if="facultyError" color="error" variant="soft" :description="facultyError" />
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3 w-full">
          <UButton variant="outline" class="bg-white hover:!text-white" @click="facultyOpen = false">Отмена</UButton>
          <UButton :loading="facultySaving" color="primary" @click="saveFaculty">Сохранить</UButton>
        </div>
      </template>
    </UModal>

    <!-- Specialty modal -->
    <UModal v-model:open="specialtyOpen" class="z-100">
      <template #body>
        <div class="space-y-4">
          <h2 class="text-xl font-bold text-black">
            {{ specialtyEditingId ? 'Редактировать специальность' : 'Новая специальность' }}
          </h2>
          <UFormField label="Факультет">
            <select
              v-model="specialtyForm.facultyId"
              class="w-full px-3 py-2 rounded-xl border border-gray-200 bg-white"
            >
              <option value="" disabled>Выберите факультет</option>
              <option v-for="f in faculties" :key="f.id" :value="f.id">{{ f.name }}</option>
            </select>
          </UFormField>
          <UFormField label="Название специальности">
            <UInput v-model="specialtyForm.name" placeholder="Например: ПОИТ" />
          </UFormField>
          <UAlert v-if="specialtyError" color="error" variant="soft" :description="specialtyError" />
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3 w-full">
          <UButton variant="outline" class="bg-white hover:!text-white" @click="specialtyOpen = false">Отмена</UButton>
          <UButton :loading="specialtySaving" color="primary" @click="saveSpecialty">Сохранить</UButton>
        </div>
      </template>
    </UModal>

    <!-- Subject modal -->
    <UModal v-model:open="subjectOpen" class="z-100">
      <template #body>
        <div class="space-y-4">
          <h2 class="text-xl font-bold text-black">
            {{ subjectEditingId ? 'Редактировать предмет' : 'Новый предмет' }}
          </h2>

          <UFormField label="Название предмета">
            <UInput v-model="subjectForm.name" placeholder="Например: Машинное обучение" />
          </UFormField>

          <label class="flex items-start gap-2 p-3 rounded-xl border border-gray-200 bg-blue-50 cursor-pointer">
            <input
              type="checkbox"
              v-model="subjectForm.common"
              class="mt-0.5"
            />
            <div>
              <div class="font-semibold text-sm">Общий предмет — добавить ко всем специальностям</div>
              <div class="text-xs text-gray-600">
                Подходит для математики, философии и т. п. — предмет станет видимым на любой специальности.
              </div>
            </div>
          </label>

          <div v-if="!subjectForm.common" class="space-y-3">
            <div class="text-sm font-semibold text-black">
              Привязать к специальностям ({{ subjectForm.specialtyIds.length }} выбрано):
            </div>
            <div
              v-for="f in subjectSpecialtiesGrouped"
              :key="f.id"
              class="rounded-xl border border-gray-200 bg-gray-50 p-3"
            >
              <div class="flex items-center justify-between mb-2">
                <h4 class="font-bold text-black">{{ f.name }}</h4>
                <button
                  type="button"
                  class="text-xs text-blue-600 hover:underline"
                  @click="selectAllForFaculty(f)"
                >
                  Выбрать всё
                </button>
              </div>
              <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
                <label
                  v-for="s in f.specialties"
                  :key="s.id"
                  class="flex items-center gap-2 px-2 py-1 rounded-lg bg-white border border-gray-200 text-sm cursor-pointer hover:bg-gray-50"
                >
                  <input
                    type="checkbox"
                    :checked="subjectForm.specialtyIds.includes(s.id)"
                    @change="toggleSpecialtyInSubject(s.id)"
                  />
                  {{ s.name }}
                </label>
              </div>
            </div>
          </div>

          <UAlert v-if="subjectError" color="error" variant="soft" :description="subjectError" />

          <div class="text-xs text-gray-600 bg-yellow-100 border border-gray-200 rounded-lg p-3">
            Подсказка: если несколько специальностей (даже на разных факультетах) разделяют один и тот же предмет, подборки книг для них будут общими.
          </div>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-3 w-full">
          <UButton variant="outline" class="bg-white hover:!text-white" @click="subjectOpen = false">Отмена</UButton>
          <UButton :loading="subjectSaving" color="primary" @click="saveSubject">Сохранить</UButton>
        </div>
      </template>
    </UModal>
  </div>
</template>
