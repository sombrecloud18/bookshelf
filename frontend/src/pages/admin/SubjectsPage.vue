<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import Select from '../AccountPage/components/Select.vue';
import { specialtiesData } from '../../constants/studyData.js';
import { api } from '../../api.js';

const subjects = ref([]);
const loading = ref(true);

const editOpen = ref(false);
const editingId = ref(null);
const formError = ref(null);
const saving = ref(false);

const form = reactive({
  specialty: '',
  name: '',
});

const specialtiesList = computed(() => {
  const out = [];
  for (const [, list] of Object.entries(specialtiesData || {})) {
    for (const s of list) {
      out.push({ value: s.value, label: s.label });
    }
  }
  return out;
});

const grouped = computed(() => {
  const map = new Map();
  for (const s of subjects.value) {
    if (!map.has(s.specialty)) map.set(s.specialty, []);
    map.get(s.specialty).push(s);
  }
  return Array.from(map.entries()).map(([specialty, items]) => ({ specialty, items }));
});

async function load() {
  loading.value = true;
  try {
    subjects.value = await api.get('/subjects');
  } catch (e) {
    console.error('Ошибка загрузки предметов:', e);
  } finally {
    loading.value = false;
  }
}

onMounted(load);

function openCreate() {
  editingId.value = null;
  form.specialty = '';
  form.name = '';
  formError.value = null;
  editOpen.value = true;
}

function openEdit(subject) {
  editingId.value = subject.id;
  form.specialty = subject.specialty;
  form.name = subject.name;
  formError.value = null;
  editOpen.value = true;
}

async function save() {
  formError.value = null;
  if (!form.specialty || !form.name.trim()) {
    formError.value = 'Заполните специальность и название';
    return;
  }
  saving.value = true;
  try {
    const payload = { specialty: form.specialty, name: form.name.trim() };
    if (editingId.value) {
      const updated = await api.put(`/subjects/${editingId.value}`, payload);
      subjects.value = subjects.value.map(s => s.id === updated.id ? updated : s);
    } else {
      const created = await api.post('/subjects', payload);
      subjects.value = [...subjects.value, created];
    }
    editOpen.value = false;
  } catch (e) {
    formError.value = e.message || 'Не удалось сохранить';
  } finally {
    saving.value = false;
  }
}

async function remove(subject) {
  if (!confirm(`Удалить предмет «${subject.name}» (${subject.specialty})?`)) return;
  try {
    await api.delete(`/subjects/${subject.id}`);
    subjects.value = subjects.value.filter(s => s.id !== subject.id);
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
          <h1 class="text-4xl font-bold text-black">Управление предметами</h1>
          <p class="text-gray-700 mt-2">Всего записей: {{ subjects.length }}</p>
        </div>
        <div class="flex items-center gap-3">
          <UButton color="primary" class="rounded-xl" @click="openCreate">+ Добавить предмет</UButton>
          <UButton to="/admin" variant="ghost" class="rounded-xl">← Назад</UButton>
        </div>
      </div>

      <div v-if="loading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
      </div>

      <UCard v-else variant="soft" class="bg-white rounded-2xl p-5">
        <div v-if="grouped.length === 0" class="text-center py-8 text-gray-500">Предметы не добавлены</div>
        <div v-else class="space-y-6">
          <div v-for="group in grouped" :key="group.specialty">
            <h3 class="text-lg font-semibold text-black mb-3">{{ group.specialty }}</h3>
            <div class="space-y-2">
              <div
                v-for="s in group.items"
                :key="s.id"
                class="flex items-center justify-between p-3 rounded-xl border border-gray-200"
              >
                <span class="text-sm">{{ s.name }}</span>
                <div class="flex gap-2">
                  <UButton size="xs" color="primary" variant="soft" @click="openEdit(s)">Изменить</UButton>
                  <UButton size="xs" color="red" variant="soft" @click="remove(s)">Удалить</UButton>
                </div>
              </div>
            </div>
          </div>
        </div>
      </UCard>

      <UModal v-model:open="editOpen" class="z-100">
        <template #body>
          <div class="space-y-4">
            <h2 class="text-xl font-bold text-black">{{ editingId ? 'Редактировать предмет' : 'Добавить предмет' }}</h2>

            <Select
              v-model="form.specialty"
              label="Специальность"
              name="specialty"
              :items="specialtiesList"
              placeholder="Выберите специальность"
            />

            <UFormField label="Название предмета">
              <UInput v-model="form.name" placeholder="Например: Машинное обучение" />
            </UFormField>

            <UAlert v-if="formError" color="error" variant="soft" :description="formError" />
          </div>
        </template>
        <template #footer>
          <div class="flex justify-end gap-3 w-full">
            <UButton variant="outline" @click="editOpen = false">Отмена</UButton>
            <UButton :loading="saving" color="primary" @click="save">Сохранить</UButton>
          </div>
        </template>
      </UModal>
    </div>
  </div>
</template>
