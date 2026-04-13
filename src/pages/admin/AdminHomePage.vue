<script setup>
const stats = [
  { label: 'Рецензий на модерации', value: 3 },
  { label: 'Подборок на модерации', value: 2 },
  { label: 'Активных мероприятий', value: 2 },
];
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-4xl font-bold text-black mb-8">Панель администратора</h1>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <UCard v-for="s in stats" :key="s.label" variant="soft" class="bg-white rounded-2xl p-5">
          <div class="text-sm text-gray-600">{{ s.label }}</div>
          <div class="mt-2 text-3xl font-extrabold text-black">{{ s.value }}</div>
        </UCard>
      </div>

      <div class="mt-8 grid grid-cols-1 md:grid-cols-3 gap-6">
        <UCard variant="soft" class="bg-white rounded-2xl p-5">
          <h2 class="text-lg font-semibold text-black">Рецензии</h2>
          <p class="text-sm text-gray-500 mt-1">Модерация пользовательских рецензий</p>
          <div class="mt-4">
            <UButton to="/admin/reviews" class="rounded-xl w-full" color="primary"> Перейти к рецензиям </UButton>
          </div>
        </UCard>

        <UCard variant="soft" class="bg-white rounded-2xl p-5">
          <h2 class="text-lg font-semibold text-black">Подборки</h2>
          <p class="text-sm text-gray-500 mt-1">Модерация пользовательских подборок</p>
          <div class="mt-4">
            <UButton to="/admin/collections" class="rounded-xl w-full" color="primary"> Перейти к подборкам </UButton>
          </div>
        </UCard>

        <UCard variant="soft" class="bg-white rounded-2xl p-5">
          <h2 class="text-lg font-semibold text-black">Подборки по предметам</h2>
          <p class="text-sm text-gray-500 mt-1">Модерация учебных подборок</p>
          <div class="mt-4">
            <UButton to="/admin/subject-collections" class="rounded-xl w-full" color="primary">
              Перейти к модерации
            </UButton>
          </div>
        </UCard>

        <UCard variant="soft" class="bg-white rounded-2xl p-5">
          <h2 class="text-lg font-semibold text-black">Мероприятия</h2>
          <p class="text-sm text-gray-500 mt-1">Управление мероприятиями</p>
          <div class="mt-4">
            <UButton to="/admin/events" class="rounded-xl w-full" variant="outline"> Управление мероприятиями </UButton>
          </div>
        </UCard>
      </div>
    </div>
  </div>
</template>
