<template>
  <UCard
    variant="soft"
    class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white flex flex-col h-full relative"
  >
    <!-- Звёздочка «в избранное / заказы» -->
    <button
      class="absolute top-3 right-3 p-2 rounded-full bg-white/80 hover:bg-white transition-all duration-200 shadow-md"
      :class="isOrdered ? 'text-yellow-500' : 'text-gray-400'"
      :aria-label="isOrdered ? 'Убрать из заказов' : 'Добавить в заказы'"
      :title="isOrdered ? 'В заказах' : 'Добавить в заказы'"
      @click.stop="$emit('toggle-order')"
    >
      <svg
        class="w-5 h-5"
        :fill="isOrdered ? 'currentColor' : 'none'"
        stroke="currentColor"
        stroke-width="1.8"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.32.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .32-.988l5.518-.442a.562.562 0 0 0 .475-.345l2.125-5.11Z"
        />
      </svg>
    </button>

    <div>
      <div>
        <span class="inline-flex px-4 py-1 text-sm font-medium rounded-md" :class="getGenreColor(genre)">
          {{ genre || 'Без жанра' }}
        </span>
      </div>
      <div class="flex min-h-16 items-center justify-center">
        <h2 class="text-2xl font-bold text-center text-black line-clamp-2">{{ title }}</h2>
      </div>
    </div>

    <div class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center">
      <img
        v-if="imageUrl"
        class="max-h-full max-w-full object-contain hover:scale-105 transition-transform duration-300"
        :src="imageUrl"
        :alt="title"
      />
      <span v-else class="text-xs text-gray-400">Нет обложки</span>
    </div>

    <div class="flex gap-2 justify-center mt-4 pt-2">
      <UButton size="md" color="primary" class="flex-1 justify-center" @click="reserve"> Забронировать </UButton>
      <UButton size="md" variant="outline" class="flex-1 justify-center bg-white hover:!text-white" @click="goToBookPage"> Подробнее </UButton>
    </div>
  </UCard>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { getGenreColor } from '../constants/genreColors';

const props = defineProps({
  id: { type: String, required: true },
  title: { type: String, required: true },
  imageUrl: { type: String, default: '' },
  genre: { type: String, default: '' },
  author: { type: String, default: '' },
  description: { type: String, default: '' },
  year: { type: [String, Number], default: '' },
  isOrdered: { type: Boolean, default: false },
});

defineEmits(['toggle-order']);
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
