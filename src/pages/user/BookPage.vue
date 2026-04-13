<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getGenreColor } from '../../constants/genreColors';
import BookRecommentations from './components/BookRecommentations.vue';

const route = useRoute();
const router = useRouter();

// Данные книги
const book = ref(null);
const reviews = ref([]);
const loading = ref(true);
const newReview = ref({ rating: 0, text: '' });
const showReviewForm = ref(false);

// Состояние для комментариев к рецензиям
const showCommentFormForReview = ref(null);
const newCommentText = ref('');

// Моковые данные книг (РАСШИРЕННЫЙ СПИСОК)
const booksMock = {
  'book-satan': {
    id: 'book-satan',
    title: 'Скорбь сатаны',
    genre: 'Роман',
    author: 'Мария Корелли',
    year: 1895,
    description:
      'Молодой писатель Джеффри Темпест, мечтая о славе и богатстве, заключает сделку с таинственным незнакомцем Луцио Риманцем.',
    fullDescription:
      '«Скорбь сатаны» — один из самых известных мистических романов Марии Корелли. История начинается с того, что бедный, но талантливый писатель Джеффри Темпест встречает загадочного Луцио Риманца, который предлагает ему неограниченные богатства и славу в обмен на его душу. \n\nПо мере развития сюжета Джеффри погружается в мир лондонского высшего общества, где сталкивается с лицемерием, предательством и коррупцией.',
    imageUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
    coverUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
    pages: 384,
    publisher: 'Эксмо',
    publishYear: 2020,
    isbn: '978-5-04-109876-5',
  },
  'book-ave-maria': {
    id: 'book-ave-maria',
    title: 'Проект "Ave Maria"',
    genre: 'Научная фантастика',
    author: 'Энди Вейер',
    year: 2021,
    description: 'Астронавт Райланд Грейс просыпается в космосе без памяти.',
    fullDescription:
      'Райланд Грейс просыпается в космическом корабле без памяти. Он не помнит своего имени, не знает, где находится. Постепенно он вспоминает, что его миссия — спасти человечество от вымирания.',
    imageUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
    coverUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
    pages: 480,
    publisher: 'АСТ',
    publishYear: 2021,
  },
  'book-hunger-games': {
    id: 'book-hunger-games',
    title: 'Голодные игры',
    genre: 'Фэнтези',
    author: 'Сьюзен Коллинз',
    year: 2008,
    description: 'В постапокалиптическом Панеме подростки вынуждены сражаться насмерть.',
    fullDescription:
      'В руинах Северной Америки возвышается государство Панем. Каждый год его районы отправляют двух "трибутов" на Голодные игры, где они должны сражаться до смерти на огромной арене.',
    imageUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
    coverUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
    pages: 416,
    publisher: 'АСТ',
    publishYear: 2010,
  },
  'book-kwebe': {
    id: 'book-kwebe',
    title: 'Правда о деле Гарри Квеберта',
    genre: 'Детектив',
    author: 'Жоэль Диккер',
    year: 2012,
    description: 'Писатель расследует убийство 30-летней давности.',
    fullDescription:
      'Маркус Гольдман — подающий надежды писатель, но его книги не продаются. Его издатель отправляет его к знаменитому писателю Гарри Квеберту, который не публиковался 33 года.',
    imageUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
    coverUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
    pages: 672,
    publisher: 'Иностранка',
    publishYear: 2014,
  },
  // ДОБАВЛЯЕМ НЕДОСТАЮЩИЕ КНИГИ ДЛЯ РЕКОМЕНДАЦИЙ
  'book-master': {
    id: 'book-master',
    title: 'Мастер и Маргарита',
    genre: 'Роман',
    author: 'Михаил Булгаков',
    year: 1967,
    description: 'Классический роман о любви, сатире и мистических событиях в Москве.',
    fullDescription:
      '«Мастер и Маргарита» — самый известный роман Михаила Булгакова, над которым он работал в течение 12 лет. Это гениальное произведение, сочетающее в себе сатиру, философию, любовную историю и мистику. \n\nДействие разворачивается в Москве 1930-х годов, где появляется Воланд — сатана в человеческом обличье — со своей свитой. Параллельно развивается история Мастера, написавшего роман о Понтии Пилате, и его возлюбленной Маргариты.',
    imageUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
    coverUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
    pages: 416,
    publisher: 'Азбука',
    publishYear: 2022,
    isbn: '978-5-389-12345-6',
  },
  'book-institute': {
    id: 'book-institute',
    title: 'Институт',
    genre: 'Фантастика',
    author: 'Стивен Кинг',
    year: 2019,
    description: 'Триллер о детях с необычными способностями и тайной организации.',
    fullDescription:
      'В темноте ночи в тихом городке пропадает 12-летний Люк Эллис. Его родители убиты, а сам он оказывается в "Институте" — загадочном учреждении, где над детьми с экстрасенсорными способностями проводятся жестокие эксперименты. \n\nЛюк должен найти способ выбраться и раскрыть миру правду об этом месте. Но сможет ли он доверять другим детям, которые стали жертвами тех же экспериментов?',
    imageUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
    coverUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
    pages: 560,
    publisher: 'АСТ',
    publishYear: 2019,
    isbn: '978-5-17-109876-5',
  },
  'book-crows': {
    id: 'book-crows',
    title: 'Шестерка воронов',
    genre: 'Фэнтези',
    author: 'Ли Бардуго',
    year: 2015,
    description: 'Фэнтези об ограблении века, команде и высоких ставках.',
    fullDescription:
      'Каз Бреккер — гениальный преступник, которому предлагают невыполнимое задание: проникнуть в неприступную крепость и выкрасть ученого, создавшего смертельно опасное оружие. \n\nДля этого он собирает команду из шести изгоев: шпионку, стрелка, бойца, шулера, штурмовика и самого Каза. У каждого из них свои демоны и свои причины участвовать в этой смертельной игре.',
    imageUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
    coverUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
    pages: 480,
    publisher: 'АСТ',
    publishYear: 2020,
    isbn: '978-5-17-123456-7',
  },
  'book-bookship': {
    id: 'book-bookship',
    title: 'Bookship',
    genre: 'Фантастика',
    author: 'Неизвестный автор',
    year: null,
    description: 'Фантастическая история о приключениях и поиске своего пути.',
    fullDescription:
      'Удивительная история о путешествиях между мирами, о поиске себя и своего места во вселенной. Книга, которая заставляет задуматься о важности выбора и цене свободы.',
    imageUrl: 'https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg',
    coverUrl: 'https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg',
    pages: 350,
    publisher: 'Самоиздат',
    publishYear: 2023,
    isbn: 'Не указан',
  },
};

// Моковые рецензии с комментариями
const reviewsMock = ref([
  {
    id: 'rev-1',
    bookId: 'book-satan',
    userId: 'user1',
    userName: 'Анна Смирнова',
    userAvatar: null,
    rating: 5,
    text: 'В центре повествования — талантливый, но бедный и гордый писатель. Роман читается легко, при этом оставляет сильное послевкусие и заставляет задуматься о выборе и ответственности.\n\nОсобенно понравилась атмосфера и то, как автор раскрывает характеры.',
    createdAt: '2026-04-10',
    likes: 12,
    comments: [
      {
        id: 'comm-1',
        userId: 'user3',
        userName: 'Елена Морозова',
        userAvatar: null,
        text: 'Полностью согласна! Эта книга - настоящий шедевр.',
        createdAt: '2026-04-11',
        likes: 3,
      },
      {
        id: 'comm-2',
        userId: 'user2',
        userName: 'Михаил Петров',
        userAvatar: null,
        text: 'Спасибо за рецензию! Тоже очень понравилась книга.',
        createdAt: '2026-04-11',
        likes: 1,
      },
    ],
  },
  {
    id: 'rev-2',
    bookId: 'book-satan',
    userId: 'user2',
    userName: 'Михаил Петров',
    userAvatar: null,
    rating: 4,
    text: 'Интересный философский подтекст. Книга заставляет задуматься о морали и искушениях. Немного затянуто в середине, но финал того стоит.',
    createdAt: '2026-04-08',
    likes: 5,
    comments: [],
  },
]);

onMounted(() => {
  const bookId = route.params.id;

  // Загружаем книгу
  if (booksMock[bookId]) {
    book.value = booksMock[bookId];
  } else {
    console.error('Книга не найдена:', bookId);
    router.push('/404');
    return;
  }

  // Загружаем рецензии для этой книги
  reviews.value = reviewsMock.value.filter(r => r.bookId === bookId);

  loading.value = false;
});

function setRating(value) {
  newReview.value.rating = value;
}

function submitReview() {
  if (newReview.value.rating === 0) {
    alert('Пожалуйста, поставьте оценку');
    return;
  }
  if (!newReview.value.text.trim()) {
    alert('Пожалуйста, напишите рецензию');
    return;
  }

  const review = {
    id: `rev-${Date.now()}`,
    bookId: book.value.id,
    userId: 'current-user',
    userName: 'Вы',
    userAvatar: null,
    rating: newReview.value.rating,
    text: newReview.value.text,
    createdAt: new Date().toISOString().split('T')[0],
    likes: 0,
    comments: [],
  };

  reviews.value.unshift(review);
  newReview.value = { rating: 0, text: '' };
  showReviewForm.value = false;
}

function likeReview(reviewId) {
  const review = reviews.value.find(r => r.id === reviewId);
  if (review) {
    review.likes++;
  }
}

function likeComment(reviewId, commentId) {
  const review = reviews.value.find(r => r.id === reviewId);
  if (review) {
    const comment = review.comments.find(c => c.id === commentId);
    if (comment) {
      comment.likes++;
    }
  }
}

function showCommentForm(reviewId) {
  showCommentFormForReview.value = reviewId;
  newCommentText.value = '';
}

function hideCommentForm() {
  showCommentFormForReview.value = null;
  newCommentText.value = '';
}

function submitComment(reviewId) {
  if (!newCommentText.value.trim()) return;

  const review = reviews.value.find(r => r.id === reviewId);
  if (review) {
    const comment = {
      id: `comm-${Date.now()}`,
      userId: 'current-user',
      userName: 'Вы',
      userAvatar: null,
      text: newCommentText.value,
      createdAt: new Date().toISOString().split('T')[0],
      likes: 0,
    };

    if (!review.comments) {
      review.comments = [];
    }
    review.comments.unshift(comment);
  }

  hideCommentForm();
}

const averageRating = computed(() => {
  if (reviews.value.length === 0) return 0;
  const sum = reviews.value.reduce((acc, r) => acc + r.rating, 0);
  return (sum / reviews.value.length).toFixed(1);
});

function reserve() {
  window.open('https://library.bsuir.by/', '_blank', 'noopener,noreferrer');
}
</script>

<template>
  <div v-if="loading" class="flex justify-center items-center min-h-100">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
      <p class="mt-4 text-gray-600">Загрузка...</p>
    </div>
  </div>

  <div v-else-if="book" class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <!-- Кнопка назад -->
      <button
        class="mb-6 flex items-center gap-2 text-gray-700 hover:text-black transition-colors"
        @click="router.back()"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
        Назад
      </button>

      <!-- Информация о книге -->
      <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-8">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 p-6 md:p-8">
          <!-- Обложка -->
          <div class="flex justify-center md:justify-center">
            <img
              class="w-full rounded-2xl shadow-lg object-cover"
              :src="book.coverUrl || book.imageUrl"
              :alt="book.title"
            />
          </div>

          <!-- Детали книги -->
          <div class="md:col-span-2">
            <div class="flex items-center gap-3 flex-wrap mb-3">
              <span class="inline-flex px-4 py-1 text-sm font-medium rounded-full" :class="getGenreColor(book.genre)">
                {{ book.genre }}
              </span>
              <span class="text-sm text-gray-500">{{ book.year }}</span>
            </div>

            <h1 class="text-3xl md:text-4xl font-bold text-black mb-2">{{ book.title }}</h1>
            <p class="text-xl text-gray-700 mb-4">{{ book.author }}</p>

            <div class="flex items-center gap-4 mb-6">
              <div class="flex items-center gap-1">
                <span class="text-yellow-500 text-2xl">★</span>
                <span v-if="reviews.length == 0">Нет оценок</span>
                <div v-else>
                  <span class="text-2xl font-bold">{{ averageRating }}</span>
                  <span class="text-gray-500">/5</span>
                </div>
              </div>
              <span class="text-gray-500">•</span>
              <span class="text-gray-600"
                >{{ reviews.length }} {{ reviews.length === 1 ? 'рецензия' : 'рецензий' }}</span
              >
            </div>

            <div class="prose prose-lg max-w-none mb-6">
              <h3 class="text-lg font-semibold text-black mb-2">О книге</h3>
              <p class="text-gray-700 whitespace-pre-line">{{ book.fullDescription || book.description }}</p>
            </div>

            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-gray-200">
              <div>
                <p class="text-xs text-gray-500">Издательство</p>
                <p class="text-sm font-medium">{{ book.publisher || 'Не указано' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500">Год издания</p>
                <p class="text-sm font-medium">{{ book.publishYear || book.year }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500">Страниц</p>
                <p class="text-sm font-medium">{{ book.pages || 'Не указано' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500">ISBN</p>
                <p class="text-sm font-medium">{{ book.isbn || 'Не указан' }}</p>
              </div>
            </div>

            <div class="mt-6 flex gap-3">
              <UButton color="primary" class="rounded-xl" @click="reserve"> Забронировать </UButton>
            </div>
          </div>
        </div>
      </div>

      <!-- Рецензии -->
      <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-8">
        <div class="p-6 md:p-8">
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-black">Рецензии</h2>
            <UButton color="primary" class="rounded-xl" @click="showReviewForm = !showReviewForm">
              {{ showReviewForm ? 'Отмена' : 'Написать рецензию' }}
            </UButton>
          </div>

          <!-- Форма добавления рецензии -->
          <div v-if="showReviewForm" class="mb-8 p-5 bg-gray-50 rounded-2xl">
            <h3 class="text-lg font-semibold text-black mb-4">Ваша рецензия</h3>

            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">Оценка</label>
              <div class="flex items-center gap-1">
                <button
                  v-for="i in 5"
                  :key="i"
                  type="button"
                  class="transition-transform hover:scale-105"
                  @click="setRating(i)"
                >
                  <svg
                    viewBox="0 0 24 24"
                    class="w-10 h-10"
                    :class="i <= newReview.rating ? 'text-yellow-300' : 'text-gray-200'"
                    fill="currentColor"
                  >
                    <path
                      d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">Текст рецензии</label>
              <UTextarea
                v-model="newReview.text"
                :rows="6"
                placeholder="Поделитесь впечатлениями о книге..."
                class="w-full"
              />
            </div>

            <div class="flex justify-end">
              <UButton color="green" class="rounded-xl" @click="submitReview"> Опубликовать рецензию </UButton>
            </div>
          </div>

          <!-- Список рецензий -->
          <div v-if="reviews.length > 0" class="space-y-6">
            <div v-for="review in reviews" :key="review.id" class="border-b border-gray-200 pb-6 last:border-0">
              <!-- Информация о рецензии -->
              <div class="flex justify-between items-start mb-3">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center">
                    <span class="text-gray-600 font-semibold">
                      {{ review.userName.charAt(0).toUpperCase() }}
                    </span>
                  </div>
                  <div>
                    <p class="font-semibold text-black">{{ review.userName }}</p>
                    <p class="text-xs text-gray-500">{{ review.createdAt }}</p>
                  </div>
                </div>
                <div class="flex items-center gap-1">
                  <span class="text-yellow-500">★</span>
                  <span class="font-semibold">{{ review.rating }}</span>
                </div>
              </div>

              <p class="text-gray-700 whitespace-pre-wrap ml-12">{{ review.text }}</p>

              <!-- Кнопки действий с рецензией -->
              <div class="ml-12 mt-3 flex items-center gap-4">
                <button
                  class="flex items-center gap-1 text-sm text-gray-500 hover:text-red-500 transition-colors"
                  @click="likeReview(review.id)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                    />
                  </svg>
                  <span>{{ review.likes }}</span>
                </button>

                <button
                  class="flex items-center gap-1 text-sm text-gray-500 hover:text-blue-500 transition-colors"
                  @click="showCommentForm(review.id)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                    />
                  </svg>
                  <span>{{ review.comments?.length || 0 }} комментариев</span>
                </button>
              </div>

              <!-- Форма добавления комментария -->
              <div v-if="showCommentFormForReview === review.id" class="ml-12 mt-4">
                <div class="flex gap-3">
                  <div class="flex-1">
                    <UTextarea
                      v-model="newCommentText"
                      :rows="3"
                      placeholder="Написать комментарий..."
                      class="w-full"
                    />
                  </div>
                </div>
                <div class="flex justify-end gap-2 mt-2">
                  <UButton size="sm" variant="outline" class="rounded-xl" @click="hideCommentForm"> Отмена </UButton>
                  <UButton size="sm" color="primary" class="rounded-xl" @click="submitComment(review.id)">
                    Отправить
                  </UButton>
                </div>
              </div>

              <!-- Список комментариев к рецензии -->
              <div v-if="review.comments && review.comments.length > 0" class="ml-12 mt-4 space-y-3">
                <div v-for="comment in review.comments" :key="comment.id" class="bg-gray-50 rounded-xl p-3">
                  <div class="flex justify-between items-start mb-2">
                    <div class="flex items-center gap-2">
                      <div class="w-6 h-6 bg-gray-400 rounded-full flex items-center justify-center">
                        <span class="text-white text-xs font-semibold">
                          {{ comment.userName.charAt(0).toUpperCase() }}
                        </span>
                      </div>
                      <div>
                        <p class="font-semibold text-black text-sm">{{ comment.userName }}</p>
                        <p class="text-xs text-gray-500">{{ comment.createdAt }}</p>
                      </div>
                    </div>
                  </div>
                  <p class="text-gray-700 text-sm ml-8">{{ comment.text }}</p>
                  <div class="ml-8 mt-2">
                    <button
                      class="flex items-center gap-1 text-xs text-gray-500 hover:text-red-500 transition-colors"
                      @click="likeComment(review.id, comment.id)"
                    >
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                        />
                      </svg>
                      <span>{{ comment.likes }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="text-center py-8 text-gray-500">
            <p>Пока нет рецензий. Будьте первым!</p>
          </div>
        </div>
      </div>

      <!-- Похожие книги -->
      <BookRecommentations :book-id="book.id" />
    </div>
  </div>
</template>
