<template>
  <UCard
    variant="soft"
    class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white flex flex-col h-full relative"
  >
    <!-- Иконка сердечка -->
    <button
      class="absolute top-3 right-3 z-10 p-2 rounded-full bg-white/80 hover:bg-white transition-all duration-200 shadow-md"
      :class="isOrdered ? 'text-red-500' : 'text-gray-400'"
      aria-label="Добавить в заказы"
      @click="$emit('toggle-order')"
    >
      <svg
        class="w-5 h-5"
        :fill="isOrdered ? 'currentColor' : 'none'"
        :stroke="isOrdered ? 'currentColor' : 'currentColor'"
        stroke-width="1.5"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
        />
      </svg>
    </button>

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
      <UButton size="md" color="primary" class="flex-1 justify-center" @click="reserve"> Забронировать </UButton>
      <UButton size="md" variant="outline" class="flex-1 justify-center" @click="goToBookPage"> Подробнее </UButton>
    </div>
  </UCard>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { getGenreColor } from '../constants/genreColors';

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
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
  author: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  year: {
    type: [String, Number],
    default: '',
  },
  isOrdered: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['toggle-order']);
const router = useRouter();

function reserve() {
  window.open('https://library.bsuir.by/', '_blank', 'noopener,noreferrer');
}

function goToBookPage() {
  if (props.id) {
    router.push(`/book/${props.id}`);
  } else {
    console.error('ID книги не передан в Card компонент');
  }
}
</script>
