<script setup>
import { computed, ref, watch } from 'vue';
import { getGenreColor } from '../../../constants/genreColors';
import { api } from '../../../api.js';
import LikeButton from '../../../components/LikeButton.vue';

const props = defineProps({
  collections: { type: Array, default: () => [] },
  books: { type: Array, default: () => [] }, // expects { id, coverUrl } or { id, imageUrl }
  // 'COLLECTION' for user-made book sets, 'SUBJECT_COLLECTION' for academic ones —
  // drives which /api/comments/* and /api/likes/* endpoints we hit from the modal.
  type: { type: String, default: 'COLLECTION' },
});

const booksById = computed(() => Object.fromEntries(props.books.map(b => [b.id, b])));
const viewOpen = ref(false);
const viewingId = ref(null);

const viewingCollection = computed(() => props.collections.find(c => c.id === viewingId.value) || null);

const commentsPath = computed(() => props.type === 'SUBJECT_COLLECTION' ? 'subject-collection' : 'collection');

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

function formatDate(dateStr) {
  if (!dateStr) return '';
  return String(dateStr).split('T')[0];
}

// ── Comments ─────────────────────────────────────────────────────────────────
const comments = ref([]);
const commentsLoading = ref(false);
const newCommentText = ref('');
const submittingComment = ref(false);
const commentError = ref(null);

async function loadComments(id) {
  if (!id) return;
  commentsLoading.value = true;
  commentError.value = null;
  try {
    comments.value = await api.get(`/comments/${commentsPath.value}/${id}`) || [];
  } catch (e) {
    console.error('Ошибка загрузки комментариев:', e);
    comments.value = [];
  } finally {
    commentsLoading.value = false;
  }
}

async function submitComment() {
  const text = newCommentText.value.trim();
  if (!text || !viewingCollection.value) return;
  submittingComment.value = true;
  commentError.value = null;
  try {
    const created = await api.post(`/comments/${commentsPath.value}/${viewingCollection.value.id}`, { text });
    comments.value = [...comments.value, created];
    newCommentText.value = '';
  } catch (e) {
    commentError.value = e.message || 'Не удалось отправить комментарий';
  } finally {
    submittingComment.value = false;
  }
}

// Refetch comments whenever the modal opens for a different collection.
watch(viewingId, (id) => {
  comments.value = [];
  newCommentText.value = '';
  commentError.value = null;
  if (id && viewOpen.value) loadComments(id);
});

watch(viewOpen, (open) => {
  if (open && viewingId.value) loadComments(viewingId.value);
});
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
        <div class="relative isolate h-[80%] w-[78%]">
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

      <div class="flex items-center justify-between gap-2 mt-auto pt-4">
        <LikeButton
          :target-type="type"
          :target-id="String(c.id)"
          :initial-count="Number(c.likes || 0)"
          :initial-liked="!!c.liked"
        />
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

          <div class="mt-3 flex items-center gap-4 pt-3 border-t border-gray-100">
            <LikeButton
              :target-type="type"
              :target-id="String(viewingCollection.id)"
              :initial-count="Number(viewingCollection.likes || 0)"
              :initial-liked="!!viewingCollection.liked"
            />
            <span class="text-xs text-gray-500">Комментариев: {{ comments.length }}</span>
          </div>
        </div>

        <div class="space-y-3 max-h-[300px] overflow-auto pr-1">
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

        <!-- Comments -->
        <div class="border-t border-gray-200 pt-4">
          <h3 class="text-lg font-bold text-black mb-3">Комментарии</h3>

          <div class="mb-4">
            <UTextarea
              v-model="newCommentText"
              :rows="3"
              placeholder="Поделитесь мнением о подборке..."
              class="w-full"
            />
            <UAlert v-if="commentError" color="error" variant="soft" :description="commentError" class="mt-2" />
            <div class="flex justify-end mt-2">
              <UButton
                size="sm"
                color="primary"
                class="rounded-xl"
                :loading="submittingComment"
                :disabled="!newCommentText.trim()"
                @click="submitComment"
              >
                Отправить
              </UButton>
            </div>
          </div>

          <div v-if="commentsLoading" class="flex justify-center py-4">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
          <div v-else-if="comments.length === 0" class="text-sm text-gray-500 text-center py-4">
            Пока нет комментариев. Будьте первым!
          </div>
          <div v-else class="space-y-3 max-h-[260px] overflow-auto pr-1">
            <div v-for="comment in comments" :key="comment.id" class="bg-gray-50 rounded-xl p-3">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-6 h-6 bg-gray-400 rounded-full flex items-center justify-center">
                  <span class="text-white text-xs font-semibold">
                    {{ (comment.userName || '?').charAt(0).toUpperCase() }}
                  </span>
                </div>
                <div>
                  <p class="font-semibold text-black text-sm">{{ comment.userName }}</p>
                  <p class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</p>
                </div>
              </div>
              <p class="text-gray-700 text-sm ml-8 break-words" style="overflow-wrap: anywhere">{{ comment.text }}</p>
              <div class="ml-8 mt-2">
                <LikeButton
                  target-type="COMMENT"
                  :target-id="String(comment.id)"
                  :initial-count="Number(comment.likes || 0)"
                  :initial-liked="!!comment.liked"
                  size="xs"
                />
              </div>
            </div>
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
