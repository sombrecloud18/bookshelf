<script setup>
import { ref, onMounted } from 'vue';
import Card from '../../components/Card.vue';

// Все доступные книги
const allBooks = [
  {
    id: 'book-satan',
    title: 'Скорбь сатаны',
    genre: 'Роман',
    imageUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
    author: 'Мария Корелли',
    description: 'Роман о талантливом писателе, сделке с совестью и цене успеха.',
    year: 1895,
  },
  {
    id: 'book-ave-maria',
    title: 'Проект "Ave Maria"',
    genre: 'Научная фантастика',
    imageUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
    author: 'Энди Вейер',
    description: 'Научная фантастика о спасении человечества и неожиданных союзниках.',
    year: 2021,
  },
  {
    id: 'book-hunger-games',
    title: 'Голодные игры',
    genre: 'Фэнтези',
    imageUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
    author: 'Сьюзен Коллинз',
    description: 'Антиутопия о выживании, выборе и цене власти.',
    year: 2008,
  },
  {
    id: 'book-kwebe',
    title: 'Правда о деле Гарри Квеберта',
    genre: 'Детектив',
    imageUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
    author: 'Жоэль Диккер',
    description: 'Детектив о тайнах прошлого и расследовании, которое меняет всё.',
    year: 2012,
  },
  {
    id: 'book-master',
    title: 'Мастер и Маргарита',
    genre: 'Роман',
    imageUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
    author: 'Михаил Булгаков',
    description: 'Классика: любовь, сатира и мистические события в Москве.',
    year: 1967,
  },
  {
    id: 'book-bookship',
    title: 'Bookship',
    genre: 'Фантастика',
    imageUrl: 'https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg',
    author: '—',
    description: 'Фантастическая история о приключениях и поиске своего пути.',
    year: null,
  },
  {
    id: 'book-institute',
    title: 'Институт',
    genre: 'Фантастика',
    imageUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
    author: 'Стивен Кинг',
    description: 'Триллер о детях с необычными способностями и тайной организации.',
    year: 2019,
  },
  {
    id: 'book-crows',
    title: 'Шестерка воронов',
    genre: 'Фэнтези',
    imageUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
    author: 'Ли Бардуго',
    description: 'Фэнтези об ограблении века, команде и высоких ставках.',
    year: 2015,
  },
];

const orderedBooks = ref([]);

// Функция для сохранения заказов в localStorage
function saveOrders(orderIds) {
  localStorage.setItem('userOrders', JSON.stringify(orderIds));
}

// Загрузка заказов из localStorage
function loadOrders() {
  const savedOrders = localStorage.getItem('userOrders');
  if (savedOrders) {
    const orderIds = JSON.parse(savedOrders);
    // Получаем полные данные книг по ID
    orderedBooks.value = allBooks.filter(book => orderIds.includes(book.id));
  }
}

// Удаление книги из заказов
function removeFromOrders(bookId) {
  // Получаем текущие ID заказов
  const currentOrders = JSON.parse(localStorage.getItem('userOrders') || '[]');

  // Удаляем книгу из массива
  const updatedOrders = currentOrders.filter(id => id !== bookId);

  // Сохраняем обновленный массив
  saveOrders(updatedOrders);

  // Обновляем отображаемые книги
  orderedBooks.value = orderedBooks.value.filter(book => book.id !== bookId);

  // Показываем уведомление (опционально)
  // Можно добавить toast уведомление
}

// Обработчик клика по сердечку
function handleToggleOrder(bookId) {
  removeFromOrders(bookId);
}

onMounted(() => {
  loadOrders();
});
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-4xl font-bold text-black mb-8">Мои заказы</h1>

      <div
        v-if="orderedBooks.length > 0"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr"
      >
        <Card
          v-for="book in orderedBooks"
          :id="book.id"
          :key="book.id"
          :title="book.title"
          :genre="book.genre"
          :image-url="book.imageUrl"
          :author="book.author"
          :description="book.description"
          :year="book.year"
          :is-ordered="true"
          @toggle-order="handleToggleOrder(book.id)"
        />
      </div>

      <div v-else class="bg-white rounded-2xl p-8 text-center">
        <svg class="w-24 h-24 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
          />
        </svg>
        <h2 class="text-2xl font-semibold text-gray-600 mb-2">Нет заказов</h2>
        <p class="text-gray-500 mb-4">У вас пока нет забронированных книг</p>
        <UButton to="/" color="primary" class="rounded-xl">Перейти в каталог</UButton>
      </div>
    </div>
  </div>
</template>
