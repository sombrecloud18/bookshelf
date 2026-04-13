<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import Select from '../AccountPage/components/Select.vue';
import { faculties, courses, specialtiesData } from '../../constants/studyData.js';
import AppHeader from '../../layouts/components/AppHeader.vue';

const AUTH_TOKEN_KEY = 'bookshelf_auth_token';
const AUTH_ROLE_KEY = 'bookshelf_auth_role';

const router = useRouter();
const loading = ref(false);
const error = ref(null);

const form = reactive({
  lastName: '',
  firstName: '',
  patronymic: '',
  faculty: '',
  specialty: '',
  course: '',
  phoneNumber: '',
  email: '',
  password: '',
  avatarUrl: '',
});

const filteredSpecialties = computed(() => {
  if (!form.faculty) return [];
  return specialtiesData[form.faculty] || [];
});

function onFacultyChange() {
  form.specialty = '';
}

async function submit() {
  error.value = null;
  if (!form.lastName.trim() || !form.firstName.trim() || !form.faculty || !form.specialty || !form.course) {
    error.value = 'Заполните обязательные поля (ФИО и учебные данные).';
    return;
  }
  if (!form.email.trim() || !form.password) {
    error.value = 'Укажите почту и пароль.';
    return;
  }

  loading.value = true;
  await new Promise(r => setTimeout(r, 350));
  loading.value = false;

  // Пока без API: сохраняем профиль локально + ставим токен
  localStorage.setItem(AUTH_TOKEN_KEY, `token_${Date.now()}`);
  localStorage.setItem(AUTH_ROLE_KEY, 'user');
  localStorage.setItem(
    'bookshelf_profile',
    JSON.stringify({
      fullName: [form.lastName, form.firstName, form.patronymic].filter(Boolean).join(' '),
      studyInfo: [form.faculty, form.specialty, form.course].filter(Boolean).join(', '),
      phoneNumber: form.phoneNumber,
      email: form.email,
      avatarUrl: form.avatarUrl,
    }),
  );

  router.push('/');
}
</script>

<template>
  <div class="min-h-screen bg-[#a3b5ff]">
    <AppHeader />
    <main class="pt-20">
      <div class="max-w-7xl mx-auto px-8">
        <div class="flex justify-center">
          <UCard variant="soft" class="mt-6 bg-white rounded-2xl p-6 w-full max-w-7xl">
            <h1 class="text-black font-extrabold tracking-wide text-3xl uppercase">Регистрация</h1>

            <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
              <div class="space-y-4">
                <h2 class="text-lg font-semibold text-black">Личные данные</h2>

              <div class="flex gap-4 w-full">
                <UFormField label="Фамилия" name="lastName" class="flex-1">
                  <UInput v-model="form.lastName" placeholder="Введите фамилию" highlight variant="outline" size="xl" />
                </UFormField>
                <UFormField label="Имя" name="firstName" class="flex-1">
                  <UInput v-model="form.firstName" placeholder="Введите имя" highlight variant="outline" size="xl" />
                </UFormField>
                <UFormField label="Отчество" name="patronymic" class="flex-1">
                  <UInput
                    v-model="form.patronymic"
                    placeholder="Введите отчество"
                    highlight
                    variant="outline"
                    size="xl"
                  />
                </UFormField>
              </div>

              <UFormField label="Номер телефона" name="phoneNumber">
                <UInput
                  v-model="form.phoneNumber"
                  highlight
                  variant="outline"
                  class="w-full"
                  placeholder="Например: +375(33)33-33-333"
                  size="xl"
                />
              </UFormField>

              <UFormField label="Аватар (URL)" name="avatarUrl">
                <UInput
                  v-model="form.avatarUrl"
                  highlight
                  variant="outline"
                  class="w-full"
                  placeholder="https://..."
                  size="xl"
                />
              </UFormField>
              </div>

              <div class="space-y-4">
                <h2 class="text-lg font-semibold text-black">Учёба и вход</h2>

              <div class="flex gap-4 w-full">
                <Select
                  v-model="form.faculty"
                  label="Факультет"
                  name="faculty"
                  :items="faculties"
                  placeholder="Выберите факультет"
                  @change="onFacultyChange"
                />

                <Select
                  v-model="form.specialty"
                  label="Специальность"
                  name="specialty"
                  :items="filteredSpecialties"
                  placeholder="Выберите специальность"
                  :disabled="!form.faculty"
                />

                <Select v-model="form.course" label="Курс" name="course" :items="courses" placeholder="Выберите курс" />
              </div>

              <UFormField label="Почта" name="email">
                <UInput
                  v-model="form.email"
                  highlight
                  variant="outline"
                  class="w-full"
                  placeholder="student@gmail.com"
                  size="xl"
                />
              </UFormField>

              <UFormField label="Пароль" name="password">
                <UInput
                  v-model="form.password"
                  type="password"
                  highlight
                  variant="outline"
                  class="w-full"
                  placeholder="Password"
                  size="xl"
                />
              </UFormField>

              <UAlert v-if="error" color="error" variant="soft" :description="error" />

              <div class="flex gap-3 pt-2">
                <UButton :loading="loading" class="bg-[#e9b6f0] text-black rounded-md px-10" size="lg" @click="submit">
                  Зарегистрироваться
                </UButton>
                <UButton variant="outline" color="neutral" class="rounded-md" size="lg" to="/auth"> Войти </UButton>
              </div>
              </div>
            </div>
          </UCard>
        </div>
      </div>
    </main>
  </div>
</template>
