<script setup>
import { ref, reactive, computed } from 'vue';
import Select from './components/Select.vue';
import { faculties, courses, specialtiesData } from '../../constants/studyData.js';

// Функция для разбора ФИО на составляющие
const parseFullName = fullName => {
  const parts = fullName.trim().split(/\s+/);
  return {
    lastName: parts[0] || '',
    firstName: parts[1] || '',
    patronymic: parts[2] || '',
  };
};

// Функция для объединения в ФИО
const combineFullName = (lastName, firstName, patronymic) => {
  return [lastName, firstName, patronymic].filter(part => part.trim()).join(' ');
};

// Функция для разбора учебной информации
const parseStudyInfo = studyInfo => {
  // Ожидаемый формат: "Факультет, Специальность, Курс"
  const parts = studyInfo.split(',').map(part => part.trim());
  return {
    faculty: parts[0] || '',
    specialty: parts[1] || '',
    course: parts[2] || '',
  };
};

// Функция для объединения учебной информации
const combineStudyInfo = (faculty, specialty, course) => {
  return [faculty, specialty, course].filter(part => part).join(', ');
};

// Вычисляемое свойство для фильтрации специальностей
const filteredSpecialties = computed(() => {
  if (!formData.faculty) return [];
  return specialtiesData[formData.faculty] || [];
});

// Исходные данные пользователя
const userData = reactive({
  fullName: 'Викторов Глеб Остапович',
  studyInfo: 'ФКП, ПОИТ, 4 курс',
  role: 'Студент',
  phoneNumber: '+375(33)33-33-333',
  email: 'gleb@mail.ru',
  password: '8888',
  avatarUrl: 'https://i.pinimg.com/originals/ca/e3/20/cae32068d860aebece9c8c01b40b1d77.jpg',
});

// Разбираем начальные данные
const initialNameParts = parseFullName(userData.fullName);
const initialStudyParts = parseStudyInfo(userData.studyInfo);

// Данные формы
const formData = reactive({
  lastName: initialNameParts.lastName,
  firstName: initialNameParts.firstName,
  patronymic: initialNameParts.patronymic,
  faculty: initialStudyParts.faculty,
  specialty: initialStudyParts.specialty,
  course: initialStudyParts.course,
  role: userData.role,
  phoneNumber: userData.phoneNumber,
  email: userData.email,
  password: userData.password,
  avatarUrl: userData.avatarUrl,
});

const showSuccess = ref(false);
let successTimeout = null;
const show = ref(false);

// Обработчики изменений
const onFacultyChange = () => {
  formData.specialty = '';
};

// Сохранение данных
const saveUserData = () => {
  // Собираем ФИО из трёх полей
  userData.fullName = combineFullName(formData.lastName, formData.firstName, formData.patronymic);
  // Собираем учебную информацию из трёх полей
  userData.studyInfo = combineStudyInfo(formData.faculty, formData.specialty, formData.course);
  userData.role = formData.role;
  userData.avatarUrl = formData.avatarUrl;
  userData.phoneNumber = formData.phoneNumber;
  userData.email = formData.email;
  userData.password = formData.password;

  showSuccess.value = true;

  if (successTimeout) clearTimeout(successTimeout);
  successTimeout = setTimeout(() => {
    showSuccess.value = false;
  }, 3000);
};

// Сброс формы
const resetForm = () => {
  const currentNameParts = parseFullName(userData.fullName);
  const currentStudyParts = parseStudyInfo(userData.studyInfo);

  formData.lastName = currentNameParts.lastName;
  formData.firstName = currentNameParts.firstName;
  formData.patronymic = currentNameParts.patronymic;
  formData.faculty = currentStudyParts.faculty;
  formData.specialty = currentStudyParts.specialty;
  formData.course = currentStudyParts.course;
  formData.role = userData.role;
  formData.avatarUrl = userData.avatarUrl;
  formData.phoneNumber = userData.phoneNumber;
  formData.email = userData.email;
  formData.password = userData.password;

  showSuccess.value = false;
};
</script>

<template>
  <div class="p-8">
    <div class="flex gap-4 mb-8">
      <UAvatar :src="userData.avatarUrl" style="width: 125px; height: 125px" />
      <div class="flex flex-col gap-2">
        <h2 class="text-black font-semibold text-xl">{{ userData.fullName }}</h2>
        <p class="text-black font-semibold">{{ userData.studyInfo }}</p>
        <p color="gray">{{ userData.role }}</p>
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

          <Select
            v-model="formData.specialty"
            label="Специальность"
            name="specialty"
            :items="filteredSpecialties"
            placeholder="Выберите специальность"
            :disabled="!formData.faculty"
          />

          <Select v-model="formData.course" label="Курс" name="course" :items="courses" placeholder="Выберите курс" />
        </div>

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

        <UFormField label="Пароль" name="password">
          <UInput
            v-model="formData.password"
            class="w-full max-w-xl"
            placeholder="Password"
            :type="show ? 'text' : 'password'"
            highlight
            size="xl"
            variant="outline"
          >
            <template #trailing>
              <UButton
                color="neutral"
                variant="link"
                size="sm"
                :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                :aria-label="show ? 'Hide password' : 'Show password'"
                :aria-pressed="show"
                aria-controls="password"
                @click="show = !show"
              />
            </template>
          </UInput>
        </UFormField>

        <div class="flex gap-3">
          <UButton type="submit" class="bg-green-300 text-black rounded-xl" size="xl"> Сохранить </UButton>
          <UButton type="button" color="neutral" variant="outline" class="rounded-xl" @click="resetForm">
            Отмена
          </UButton>
        </div>

        <UAlert v-if="showSuccess" color="success" variant="soft" description="Данные успешно сохранены" class="mt-4" />
      </UForm>
    </div>
  </div>
</template>
