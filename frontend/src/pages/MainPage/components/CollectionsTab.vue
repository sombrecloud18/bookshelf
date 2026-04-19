<script setup>
import { computed, ref } from 'vue';
import { getGenreColor } from '../../../constants/genreColors';

const props = defineProps({
  collections: { type: Array, default: () => [] },
  books: { type: Array, default: () => [] }, // expects { id, coverUrl } or { id, imageUrl }
});

const booksById = computed(() => Object.fromEntries(props.books.map(b => [b.id, b])));
const viewOpen = ref(false);
const viewingId = ref(null);

const viewingCollection = computed(() => props.collections.find(c => c.id === viewingId.value) || null);

function lastTwoCovers(bookIds) {
  const map = booksById.value;
  return (bookIds || [])
    .map(id => map[id])
    .filter(Boolean)
    .map(b => ({ ...b, coverUrl: b.coverUrl || b.imageUrl }))
    .slice(-2);
}

function selectedBooks(ids) {
  const map = booksById.value;
  return (ids || []).map(id => map[id]).filter(Boolean);
}

function openView(id) {
  viewingId.value = id;
  viewOpen.value = true;
}
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr">
    <UCard
      v-for="c in collections"
      :key="c.id"
      variant="soft"
      class="hover:shadow-xl transition-all duration-300 rounded-2xl bg-white flex flex-col h-full"
    >
      <div class="flex flex-col">
        <div class="flex items-center justify-between gap-3 h-8">
          <span
            v-if="c.genre"
            class="inline-flex px-4 py-1 text-sm font-medium rounded-md"
            :class="getGenreColor(c.genre)"
          >
            {{ c.genre }}
          </span>
        </div>

        <div class="flex min-h-16 items-center justify-center">
          <div class="w-full">
            <h2 class="text-lg font-extrabold text-center line-clamp-2 text-black min-h-12">
              {{ c.title }}
            </h2>
            <p v-if="c.description" class="mt-2 text-sm text-gray-700 text-center line-clamp-2 min-h-10">
              {{ c.description }}
            </p>
            <p class="mt-2 text-xs text-gray-500 text-center">Книг: {{ (c.bookIds || []).length }}</p>
          </div>
        </div>
      </div>

      <div class="aspect-2/3 w-full overflow-hidden rounded-lg mt-4 bg-gray-100 flex items-center justify-center">
        <div class="relative h-[80%] w-[78%]">
          <div v-if="(c.bookIds || []).length === 0" class="text-xs text-gray-500 text-center">Нет книг</div>
          <template v-else>
            <img
              v-for="(b, idx) in lastTwoCovers(c.bookIds).reverse()"
              :key="b.id"
              class="absolute h-full rounded-2xl shadow-lg border border-white bg-white/90 p-1"
              :class="idx === 0 ? 'object-contain' : 'object-cover'"
              :style="{
                right: `${idx * 22}px`,
                top: `calc(50% + ${idx * 10}px)`,
                transform: 'translateY(-50%)',
                zIndex: 10 + idx,
              }"
              :src="b.coverUrl"
              :alt="b.title"
            />
          </template>
        </div>
      </div>

      <div class="flex gap-2 justify-center mt-auto pt-4">
        <UButton size="md" variant="outline" class="flex-1 justify-center" @click="openView(c.id)">
          Узнать подробнее
        </UButton>
      </div>
    </UCard>
  </div>

  <UModal v-model:open="viewOpen" title="Подборка" class="z-100">
    <template #body>
      <div v-if="viewingCollection" class="space-y-4">
        <div class="bg-white rounded-2xl border border-gray-200 p-4">
          <div class="flex items-start justify-between gap-4">
            <div>
              <div class="flex items-center gap-2 flex-wrap">
                <h2 class="text-2xl font-bold text-black">{{ viewingCollection.title }}</h2>
                <span
                  v-if="viewingCollection.genre"
                  class="inline-flex px-4 py-1 text-sm font-medium rounded-md"
                  :class="getGenreColor(viewingCollection.genre)"
                >
                  {{ viewingCollection.genre }}
                </span>
              </div>
              <p v-if="viewingCollection.description" class="mt-2 text-sm text-gray-700">
                {{ viewingCollection.description }}
              </p>
            </div>
            <div class="text-xs text-gray-500 mt-1">Книг: {{ (viewingCollection.bookIds || []).length }}</div>
          </div>
        </div>

        <div class="space-y-3 max-h-[420px] overflow-auto pr-1">
          <div
            v-for="b in selectedBooks(viewingCollection.bookIds)"
            :key="b.id"
            class="p-3 rounded-xl border border-gray-200 bg-white"
          >
            <div class="flex items-start gap-3">
              <img class="w-12 h-16 object-cover rounded-md flex-none" :src="b.coverUrl || b.imageUrl" :alt="b.title" />
              <div>
                <div class="font-semibold text-black">{{ b.title }}</div>
                <div v-if="b.shortDescription || b.description" class="text-xs text-gray-600 mt-1">
                  {{ b.shortDescription || b.description }}
                </div>
              </div>
            </div>
          </div>

          <div v-if="(viewingCollection.bookIds || []).length === 0" class="text-sm text-gray-500">
            В подборке пока нет книг.
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" class="rounded-xl" @click="viewOpen = false">Закрыть</UButton>
      </div>
    </template>
  </UModal>
</template>
