<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import Select from '../AccountPage/components/Select.vue';
import { courses } from '../../constants/studyData.js';
import { useFaculties } from '../../composables/useFaculties.js';
import AppHeader from '../../layouts/components/AppHeader.vue';
import FileUpload from '../../components/FileUpload.vue';
import { api } from '../../api.js';

const { facultyOptions, specialtiesByFaculty } = useFaculties();
const faculties = facultyOptions;

const AUTH_TOKEN_KEY = 'bookshelf_auth_token';
const AUTH_ROLE_KEY = 'bookshelf_auth_role';

const router = useRouter();
const loading = ref(false);
const error = ref(null);

const form = reactive({
  userType: 'STUDENT',
  login: '',
  lastName: '',
  firstName: '',
  patronymic: '',
  faculty: '',
  specialty: '',
  course: '',
  department: '',
  position: '',
  phoneNumber: '',
  email: '',
  password: '',
  avatarUrl: '',
});

const filteredSpecialties = computed(() => {
  if (!form.faculty) return [];
  return specialtiesByFaculty.value[form.faculty] || [];
});

function onFacultyChange() {
  form.specialty = '';
}

function setUserType(type) {
  form.userType = type;
  // Reset role-specific fields when switching
  if (type === 'STUDENT') {
    form.department = '';
    form.position = '';
  } else {
    form.specialty = '';
    form.course = '';
  }
}

async function submit() {
  error.value = null;
  if (!form.login.trim()) {
    error.value = 'Введите логин.';
    return;
  }
  if (!form.lastName.trim() || !form.firstName.trim()) {
    error.value = 'Заполните ФИО.';
    return;
  }
  if (form.userType === 'STUDENT') {
    if (!form.faculty || !form.specialty || !form.course) {
      error.value = 'Заполните факультет, специальность и курс.';
      return;
    }
  } else if (form.userType === 'TEACHER') {
    if (!form.faculty || !form.department.trim() || !form.position.trim()) {
      error.value = 'Заполните факультет, кафедру и должность.';
      return;
    }
  }
  if (!form.email.trim() || !form.password) {
    error.value = 'Укажите почту и пароль.';
    return;
  }
  if (form.password.length < 6) {
    error.value = 'Пароль должен содержать не менее 6 символов.';
    return;
  }

  loading.value = true;
  try {
    const data = await api.post('/auth/register', {
      userType: form.userType,
      login: form.login.trim(),
      email: form.email.trim(),
      password: form.password,
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      patronymic: form.patronymic.trim() || null,
      faculty: form.faculty || null,
      specialty: form.userType === 'STUDENT' ? form.specialty || null : null,
      course: form.userType === 'STUDENT' ? form.course || null : null,
      department: form.userType === 'TEACHER' ? form.department.trim() || null : null,
      position: form.userType === 'TEACHER' ? form.position.trim() || null : null,
      phoneNumber: form.phoneNumber.trim() || null,
      avatarUrl: form.avatarUrl || null,
    });
    localStorage.setItem(AUTH_TOKEN_KEY, data.token);
    localStorage.setItem(AUTH_ROLE_KEY, data.role);
    router.push('/');
  } catch (e) {
    error.value = e.message || 'Ошибка регистрации';
  } finally {
    loading.value = false;
  }
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

            <!-- Шаг 1: тип пользователя -->
            <div class="mt-4 mb-6">
              <h2 class="text-lg font-semibold text-black mb-3">Кто вы?</h2>
              <div class="flex gap-3">
                <button
                  type="button"
                  class="px-6 py-3 rounded-xl border-2 transition-all"
                  :class="form.userType === 'STUDENT' ? 'border-blue-600 bg-blue-50 text-black font-semibold' : 'border-gray-300 text-gray-600 hover:border-gray-400'"
                  @click="setUserType('STUDENT')"
                >
                  Студент
                </button>
                <button
                  type="button"
                  class="px-6 py-3 rounded-xl border-2 transition-all"
                  :class="form.userType === 'TEACHER' ? 'border-blue-600 bg-blue-50 text-black font-semibold' : 'border-gray-300 text-gray-600 hover:border-gray-400'"
                  @click="setUserType('TEACHER')"
                >
                  Преподаватель
                </button>
              </div>
            </div>

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

                <UFormField label="Аватар">
                  <FileUpload v-model="form.avatarUrl" scope="avatars" label="Загрузить аватар" />
                </UFormField>
              </div>

              <div class="space-y-4">
                <h2 class="text-lg font-semibold text-black">
                  {{ form.userType === 'TEACHER' ? 'Преподавательские данные' : 'Учебные данные' }}
                </h2>

                <Select
                  v-model="form.faculty"
                  label="Факультет"
                  name="faculty"
                  :items="faculties"
                  placeholder="Выберите факультет"
                  @change="onFacultyChange"
                />

                <template v-if="form.userType === 'STUDENT'">
                  <div class="flex gap-4 w-full">
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
                </template>

                <template v-else>
                  <UFormField label="Кафедра" name="department">
                    <UInput
                      v-model="form.department"
                      highlight
                      variant="outline"
                      class="w-full"
                      placeholder="Например: Кафедра ПОИТ"
                      size="xl"
                    />
                  </UFormField>
                  <UFormField label="Должность" name="position">
                    <UInput
                      v-model="form.position"
                      highlight
                      variant="outline"
                      class="w-full"
                      placeholder="Например: Доцент"
                      size="xl"
                    />
                  </UFormField>
                </template>

                <h2 class="text-lg font-semibold text-black pt-2">Вход</h2>

                <UFormField label="Логин" name="login">
                  <UInput
                    v-model="form.login"
                    highlight
                    variant="outline"
                    class="w-full"
                    placeholder="Например: ivanov_ivan"
                    size="xl"
                  />
                </UFormField>

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
                    placeholder="Минимум 6 символов"
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
