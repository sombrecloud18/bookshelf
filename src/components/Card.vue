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
      <UButton size="md" color="primary" class="flex-1 justify-center" @click="reserve"> Забронировать </UButton>
      <UButton size="md" variant="outline" class="flex-1 justify-center" @click="detailsOpen = true">
        Подробнее
      </UButton>
    </div>
  </UCard>

  <Teleport to="body">
    <div
      v-if="detailsOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-6"
      role="dialog"
      aria-modal="true"
      @keydown.esc="detailsOpen = false"
      tabindex="0"
      ref="modalRootEl"
    >
      <button
        type="button"
        class="absolute inset-0 bg-black/50"
        aria-label="Закрыть"
        @click="detailsOpen = false"
      />

      <div class="relative w-[min(1100px,calc(100vw-3rem))] max-w-[1100px] bg-white rounded-3xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between gap-3 px-6 py-4 border-b border-gray-100">
          <div class="font-semibold text-black line-clamp-1">{{ title }}</div>
          <UButton variant="ghost" color="neutral" class="rounded-xl" @click="detailsOpen = false" aria-label="Закрыть">
            ✕
          </UButton>
        </div>

        <div class="p-6">
          <div class="grid grid-cols-1 lg:grid-cols-[340px_1fr] gap-6">
            <div class="flex items-center justify-center">
              <div class="w-full aspect-2/3 rounded-2xl bg-gray-100 shadow-md flex items-center justify-center max-h-[52vh] overflow-hidden">
                <img class="max-h-full max-w-full object-contain" :src="imageUrl" :alt="title" />
              </div>
            </div>

            <div class="flex flex-col">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h2 class="text-2xl font-bold text-black leading-tight">{{ title }}</h2>
                  <p class="text-base text-gray-800 mt-1">{{ author }}</p>
                </div>

                <span class="inline-flex px-4 py-1 text-sm font-medium rounded-full" :class="getGenreColor(genre)">
                  {{ genre }}
                </span>
              </div>

              <div class="mt-4">
                <div class="text-sm font-semibold text-black">Аннотация:</div>
                <div v-if="description" class="mt-2 text-sm text-gray-700 whitespace-pre-line leading-snug line-clamp-8">
                  {{ description }}
                </div>
                <div v-else class="mt-2 text-sm text-gray-500">Описание пока не добавлено.</div>
              </div>

              <div class="mt-4 text-sm text-gray-800">
                <span class="font-semibold text-black">Год написания:</span>
                <span>{{ year || '—' }}</span>
              </div>

              <div class="mt-auto pt-6 flex items-center justify-end gap-3">
                <UButton variant="outline" class="rounded-xl" @click="writeReview">Написать рецензию</UButton>
                <UButton color="primary" class="rounded-xl" @click="reserve">Забронировать</UButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getGenreColor } from '../constants/genreColors';

const props = defineProps({
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
});

const detailsOpen = ref(false);
const router = useRouter();
const modalRootEl = ref(null);

watch(detailsOpen, async (open) => {
  if (!open) return;
  await nextTick();
  modalRootEl.value?.focus?.();
});

function reserve() {
  window.open('https://library.bsuir.by/', '_blank', 'noopener,noreferrer');
}

function writeReview() {
  // Минимально: переходим на страницу рецензий.
  // Позже можно прокинуть выбранную книгу через store/route params.
  detailsOpen.value = false;
  router.push('/reviews');
}
</script>
