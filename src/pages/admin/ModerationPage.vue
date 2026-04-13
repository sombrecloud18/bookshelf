<script setup>
const queue = [
  { id: 'r-1', type: 'Рецензия', title: 'Скорбь сатаны', author: 'student1', status: 'Ожидает' },
  { id: 'c-1', type: 'Подборка', title: 'Русская классика', author: 'student2', status: 'Ожидает' },
  { id: 'r-2', type: 'Рецензия', title: 'Голодные игры', author: 'student3', status: 'Ожидает' },
];
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-4xl font-bold text-black mb-8">Модерация</h1>

      <UCard variant="soft" class="bg-white rounded-2xl p-5">
        <div class="overflow-auto">
          <table class="min-w-full text-sm">
            <thead>
              <tr class="text-left text-gray-600">
                <th class="py-2 pr-4">Тип</th>
                <th class="py-2 pr-4">Название</th>
                <th class="py-2 pr-4">Автор</th>
                <th class="py-2 pr-4">Статус</th>
                <th class="py-2 pr-4"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in queue" :key="i.id" class="border-t border-gray-100">
                <td class="py-3 pr-4 font-semibold text-black">{{ i.type }}</td>
                <td class="py-3 pr-4">{{ i.title }}</td>
                <td class="py-3 pr-4">{{ i.author }}</td>
                <td class="py-3 pr-4 text-gray-700">{{ i.status }}</td>
                <td class="py-3 pr-4">
                  <div class="flex gap-2 justify-end">
                    <UButton size="xs" color="primary" class="rounded-xl">Одобрить</UButton>
                    <UButton size="xs" color="red" variant="soft" class="rounded-xl">Отклонить</UButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </UCard>
    </div>
  </div>
</template>

