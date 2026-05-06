<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import Select from './components/Select.vue';
import FileUpload from '../../components/FileUpload.vue';
import { courses } from '../../constants/studyData.js';
import { useFaculties } from '../../composables/useFaculties.js';
import { api } from '../../api.js';

const { facultyOptions, specialtiesByFaculty } = useFaculties();
const faculties = facultyOptions;

const loading = ref(true);
const showSuccess = ref(false);
const showPasswordSuccess = ref(false);
const showLoginSuccess = ref(false);
const saveError = ref(null);
const passwordError = ref(null);
const loginError = ref(null);
let successTimeout = null;

const filteredSpecialties = computed(() => {
  if (!formData.faculty) return [];
  return specialtiesByFaculty.value[formData.faculty] || [];
});

const profile = ref(null);
const formData = reactive({
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
  avatarUrl: '',
});

const passwordData = reactive({
  currentPassword: '',
  newPassword: '',
});

const loginData = reactive({
  currentPassword: '',
  newLogin: '',
});

const show = ref(false);
const showNew = ref(false);
const showLoginPwd = ref(false);

const isTeacher = computed(() => profile.value?.userType === 'TEACHER');

const roleLabel = computed(() => {
  if (profile.value?.role === 'admin') return 'Модератор';
  if (profile.value?.userType === 'TEACHER') return 'Преподаватель';
  return 'Студент';
});

function onFacultyChange() {
  formData.specialty = '';
}

function fillForm(p) {
  formData.lastName = p.lastName || '';
  formData.firstName = p.firstName || '';
  formData.patronymic = p.patronymic || '';
  formData.faculty = p.faculty || '';
  formData.specialty = p.specialty || '';
  formData.course = p.course || '';
  formData.department = p.department || '';
  formData.position = p.position || '';
  formData.phoneNumber = p.phoneNumber || '';
  formData.email = p.email || '';
  formData.avatarUrl = p.avatarUrl || '';
  loginData.newLogin = p.login || '';
}

onMounted(async () => {
  try {
    const p = await api.get('/users/me');
    profile.value = p;
    fillForm(p);
  } catch (e) {
    console.error('Ошибка загрузки профиля:', e);
  } finally {
    loading.value = false;
  }
});

async function saveUserData() {
  saveError.value = null;
  try {
    const payload = {
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      patronymic: formData.patronymic.trim() || null,
      faculty: formData.faculty || null,
      phoneNumber: formData.phoneNumber.trim() || null,
      email: formData.email.trim(),
      avatarUrl: formData.avatarUrl || null,
    };
    if (isTeacher.value) {
      payload.department = formData.department.trim() || null;
      payload.position = formData.position.trim() || null;
    } else {
      payload.specialty = formData.specialty || null;
      payload.course = formData.course || null;
    }
    const updated = await api.put('/users/me', payload);
    profile.value = updated;
    showSuccess.value = true;
    if (successTimeout) clearTimeout(successTimeout);
    successTimeout = setTimeout(() => { showSuccess.value = false; }, 3000);
  } catch (e) {
    saveError.value = e.message || 'Ошибка сохранения';
  }
}

function resetForm() {
  if (profile.value) fillForm(profile.value);
  showSuccess.value = false;
  saveError.value = null;
}

async function changePassword() {
  passwordError.value = null;
  if (!passwordData.currentPassword || !passwordData.newPassword) {
    passwordError.value = 'Заполните оба поля';
    return;
  }
  if (passwordData.newPassword.length < 6) {
    passwordError.value = 'Новый пароль должен содержать не менее 6 символов';
    return;
  }
  try {
    await api.put('/users/me/password', {
      currentPassword: passwordData.currentPassword,
      newPassword: passwordData.newPassword,
    });
    passwordData.currentPassword = '';
    passwordData.newPassword = '';
    showPasswordSuccess.value = true;
    setTimeout(() => { showPasswordSuccess.value = false; }, 3000);
  } catch (e) {
    passwordError.value = e.message || 'Ошибка смены пароля';
  }
}

async function changeLogin() {
  loginError.value = null;
  if (!loginData.currentPassword || !loginData.newLogin.trim()) {
    loginError.value = 'Заполните оба поля';
    return;
  }
  if (loginData.newLogin.trim().length < 3) {
    loginError.value = 'Логин должен содержать минимум 3 символа';
    return;
  }
  try {
    const updated = await api.put('/users/me/login', {
      currentPassword: loginData.currentPassword,
      newLogin: loginData.newLogin.trim(),
    });
    profile.value = updated;
    loginData.currentPassword = '';
    showLoginSuccess.value = true;
    setTimeout(() => { showLoginSuccess.value = false; }, 3000);
  } catch (e) {
    loginError.value = e.message || 'Ошибка смены логина';
  }
}
</script>

<template>
  <div class="p-8">
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
    </div>

    <template v-else>
      <div class="flex gap-4 mb-8">
        <UAvatar :src="profile?.avatarUrl" style="width: 125px; height: 125px" />
        <div class="flex flex-col gap-2">
          <h2 class="text-black font-semibold text-xl">
            {{ [profile?.lastName, profile?.firstName, profile?.patronymic].filter(Boolean).join(' ') }}
          </h2>
          <p class="text-black font-semibold">
            <template v-if="isTeacher">
              {{ [profile?.faculty, profile?.department, profile?.position].filter(Boolean).join(', ') }}
            </template>
            <template v-else>
              {{ [profile?.faculty, profile?.specialty, profile?.course].filter(Boolean).join(', ') }}
            </template>
          </p>
          <p class="text-gray-700">
            <span class="font-semibold">Логин:</span> {{ profile?.login }}
          </p>
          <p class="text-gray-600">{{ roleLabel }}</p>
        </div>
      </div>

      <div class="border-t pt-6">
        <h3 class="text-lg font-semibold mb-4">Редактировать данные</h3>

        <UForm :state="formData" class="space-y-4" @submit="saveUserData">
          <div class="flex gap-4 w-full max-w-xl">
            <UFormField label="Фамилия" name="lastName" class="flex-1">
              <UInput v-model="formData.lastName" placeholder="Введите фамилию" highlight variant="outline" size="xl" />
            </UFormField>

            <UFormField label="Имя" name="firstName" class="flex-1">
              <UInput v-model="formData.firstName" placeholder="Введите имя" highlight variant="outline" size="xl" />
            </UFormField>

            <UFormField label="Отчество" name="patronymic" class="flex-1">
              <UInput
                v-model="formData.patronymic"
                placeholder="Введите отчество"
                highlight
                variant="outline"
                size="xl"
              />
            </UFormField>
          </div>

          <div class="flex gap-4 w-full max-w-xl">
            <Select
              v-model="formData.faculty"
              label="Факультет"
              name="faculty"
              :items="faculties"
              placeholder="Выберите факультет"
              @change="onFacultyChange"
            />

            <template v-if="!isTeacher">
              <Select
                v-model="formData.specialty"
                label="Специальность"
                name="specialty"
                :items="filteredSpecialties"
                placeholder="Выберите специальность"
                :disabled="!formData.faculty"
              />

              <Select v-model="formData.course" label="Курс" name="course" :items="courses" placeholder="Выберите курс" />
            </template>
          </div>

          <template v-if="isTeacher">
            <div class="flex gap-4 w-full max-w-xl">
              <UFormField label="Кафедра" name="department" class="flex-1">
                <UInput v-model="formData.department" highlight variant="outline" size="xl" placeholder="Например: Кафедра ПОИТ" />
              </UFormField>
              <UFormField label="Должность" name="position" class="flex-1">
                <UInput v-model="formData.position" highlight variant="outline" size="xl" placeholder="Например: Доцент" />
              </UFormField>
            </div>
          </template>

          <UFormField label="Номер телефона" name="phoneNumber">
            <UInput
              v-model="formData.phoneNumber"
              highlight
              variant="outline"
              class="w-full max-w-xl"
              placeholder="Например: +375(33)33-33-333"
              size="xl"
            />
          </UFormField>

          <UFormField label="Почта" name="email">
            <UInput
              v-model="formData.email"
              highlight
              variant="outline"
              class="w-full max-w-xl"
              placeholder="Например: student@gmail.com"
              size="xl"
            />
          </UFormField>

          <UFormField label="Аватар">
            <FileUpload v-model="formData.avatarUrl" scope="avatars" label="Загрузить аватар" />
          </UFormField>

          <UAlert v-if="saveError" color="error" variant="soft" :description="saveError" class="max-w-xl" />

          <div class="flex gap-3">
            <UButton type="submit" class="bg-green-300 text-black rounded-xl" size="xl"> Сохранить </UButton>
            <UButton type="button" color="neutral" variant="outline" class="rounded-xl" @click="resetForm">
              Отмена
            </UButton>
          </div>

          <UAlert v-if="showSuccess" color="success" variant="soft" description="Данные успешно сохранены" class="mt-4" />
        </UForm>
      </div>

      <div class="border-t pt-6 mt-6">
        <h3 class="text-lg font-semibold mb-4">Сменить логин</h3>

        <div class="space-y-4 max-w-xl">
          <UFormField label="Новый логин" name="newLogin">
            <UInput
              v-model="loginData.newLogin"
              highlight
              variant="outline"
              class="w-full"
              size="xl"
              placeholder="Введите новый логин"
            />
          </UFormField>

          <UFormField label="Текущий пароль" name="loginPassword">
            <UInput
              v-model="loginData.currentPassword"
              :type="showLoginPwd ? 'text' : 'password'"
              highlight
              variant="outline"
              class="w-full"
              placeholder="Подтвердите паролем"
              size="xl"
            >
              <template #trailing>
                <UButton
                  color="neutral"
                  variant="link"
                  size="sm"
                  :icon="showLoginPwd ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                  @click="showLoginPwd = !showLoginPwd"
                />
              </template>
            </UInput>
          </UFormField>

          <UAlert v-if="loginError" color="error" variant="soft" :description="loginError" />
          <UAlert v-if="showLoginSuccess" color="success" variant="soft" description="Логин успешно изменён" />

          <UButton class="bg-green-300 text-black rounded-xl" size="xl" @click="changeLogin">
            Сменить логин
          </UButton>
        </div>
      </div>

      <div class="border-t pt-6 mt-6">
        <h3 class="text-lg font-semibold mb-4">Сменить пароль</h3>

        <div class="space-y-4 max-w-xl">
          <UFormField label="Текущий пароль" name="currentPassword">
            <UInput
              v-model="passwordData.currentPassword"
              :type="show ? 'text' : 'password'"
              highlight
              variant="outline"
              class="w-full"
              placeholder="Введите текущий пароль"
              size="xl"
            >
              <template #trailing>
                <UButton
                  color="neutral"
                  variant="link"
                  size="sm"
                  :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                  @click="show = !show"
                />
              </template>
            </UInput>
          </UFormField>

          <UFormField label="Новый пароль" name="newPassword">
            <UInput
              v-model="passwordData.newPassword"
              :type="showNew ? 'text' : 'password'"
              highlight
              variant="outline"
              class="w-full"
              placeholder="Минимум 6 символов"
              size="xl"
            >
              <template #trailing>
                <UButton
                  color="neutral"
                  variant="link"
                  size="sm"
                  :icon="showNew ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                  @click="showNew = !showNew"
                />
              </template>
            </UInput>
          </UFormField>

          <UAlert v-if="passwordError" color="error" variant="soft" :description="passwordError" />
          <UAlert v-if="showPasswordSuccess" color="success" variant="soft" description="Пароль успешно изменён" />

          <UButton class="bg-green-300 text-black rounded-xl" size="xl" @click="changePassword">
            Сменить пароль
          </UButton>
        </div>
      </div>
    </template>
  </div>
</template>
