<template>
  <UCard variant="soft" class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white flex flex-col h-full">
    <div>
      <div>
        <span class="inline-flex px-4 py-1 text-sm font-medium rounded-md" :class="getGenreColor(genre)">
          {{ genre }}
        </span>
      </div>
      <div class="flex min-h-16 items-center justify-center">
        <h2 class="text-2xl font-bold text-center line-clamp-2">{{ title }}</h2>
      </div>
    </div>

    <div class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center">
      <img
        class="max-h-full max-w-full object-contain hover:scale-105 transition-transform duration-300"
        :src="imageUrl"
        :alt="title"
      />
    </div>

    <div class="flex gap-2 justify-center mt-4 pt-2">
      <UButton size="md" color="primary" class="flex-1 justify-center">Забронировать</UButton>
      <UButton size="md" variant="outline" class="flex-1 justify-center">Подробнее</UButton>
    </div>
  </UCard>
</template>

<script setup>
import { getGenreColor } from '../constants/genreColors';

defineProps({
  title: {
    type: String,
    default: 'Скорбь сатаны',
  },
  imageUrl: {
    type: String,
    default: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
  },
  genre: {
    type: String,
    default: 'Роман',
  },
});
</script>
