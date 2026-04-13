<script setup>
import { ref } from 'vue';
import CatalogTab from './components/CatalogTab.vue';
import CollectionsTab from './components/CollectionsTab.vue';
import SubjectsTab from './components/SubjectsTab.vue';
import ClubTab from './components/ClubTab.vue';
import RecommendationWidget from './components/RecommendationWidget.vue';

const tabs = [
  { id: 'catalog', label: 'Каталог' },
  { id: 'recommendations', label: 'Рекомендации' },
  { id: 'collections', label: 'Подборки' },
  { id: 'subjects', label: 'Предметы' },
  { id: 'club', label: 'Книжный клуб' },
];

const activeTab = ref('catalog');
const query = ref('');

const books = [
  {
    id: 'book-satan',
    title: 'Скорбь сатаны',
    genre: 'Роман',
    author: 'Мария Корелли',
    year: 1895,
    description: 'Молодой писатель в поиске известности сталкивается с искушением и ценой успеха.',
    imageUrl: 'https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg',
  },
  {
    id: 'book-ave-maria',
    title: 'Проект "Ave Maria"',
    genre: 'Научная фантастика',
    author: 'Энди Вейер',
    year: 2021,
    description: 'Научная фантастика о спасении человечества и неожиданных союзниках.',
    imageUrl: 'https://cdn.litres.ru/pub/c/cover_415/66986536.jpg',
  },
  {
    id: 'book-hunger-games',
    title: 'Голодные игры',
    genre: 'Фэнтези',
    author: 'Сьюзен Коллинз',
    year: 2008,
    description: 'Антиутопия о выживании, выборе и цене власти.',
    imageUrl: 'https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488',
  },
  {
    id: 'book-kwebe',
    title: 'Правда о деле Гарри Квеберта',
    genre: 'Детектив',
    author: 'Жоэль Диккер',
    year: 2012,
    description: 'Детектив о тайнах прошлого и расследовании, которое меняет всё.',
    imageUrl: 'https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig',
  },
  {
    id: 'book-master',
    title: 'Мастер и Маргарита',
    genre: 'Роман',
    author: 'Михаил Булгаков',
    year: 1967,
    description: 'Классика: любовь, сатира и мистические события в Москве.',
    imageUrl: 'https://imo10.labirint.ru/books/668307/cover.jpg/242-0',
  },
  {
    id: 'book-bookship',
    title: 'Bookship',
    genre: 'Фантастика',
    author: '—',
    year: null,
    description: 'Фантастическая история о приключениях и поиске своего пути.',
    imageUrl: 'https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg',
  },
  {
    id: 'book-institute',
    title: 'Институт',
    genre: 'Фантастика',
    author: 'Стивен Кинг',
    year: 2019,
    description: 'Триллер о детях с необычными способностями и тайной организации.',
    imageUrl: 'https://imo10.labirint.ru/books/903891/cover.jpg/242-0',
  },
  {
    id: 'book-crows',
    title: 'Шестерка воронов',
    genre: 'Фэнтези',
    author: 'Ли Бардуго',
    year: 2015,
    description: 'Фэнтези об ограблении века, команде и высоких ставках.',
    imageUrl: 'https://imo10.labirint.ru/books/635534/cover.jpg/242-0',
  },
];

const collections = [
  {
    id: 'col-ru-classic',
    title: 'Русская классика',
    genre: 'Роман',
    description: 'Классические произведения для вдумчивого чтения.',
    bookIds: ['book-satan', 'book-kwebe', 'book-master'],
  },
  {
    id: 'col-js',
    title: 'Книги по JS',
    genre: 'Фантастика',
    description: 'Подборка для тех, кто любит код и новые идеи.',
    bookIds: ['book-ave-maria', 'book-hunger-games', 'book-institute'],
  },
  {
    id: 'col-fantasy',
    title: 'Фэнтези эпосы',
    genre: 'Фэнтези',
    description: 'Эпические фэнтези саги и приключения.',
    bookIds: ['book-crows', 'book-hunger-games'],
  },
];

const clubItems = [
  { id: 'club-1', title: 'Анонс мероприятия', text: 'Встреча книжного клуба в пятницу в 18:00. Тема: классика.' },
  { id: 'club-2', title: 'Обсуждение месяца', text: 'Читаем и обсуждаем роман. Тема: "Куджо" Стивен Кинг' },
  { id: 'club-3', title: 'Новинки литературы', text: 'Поступление новых книг в библиотеку на следующей неделе.' },
];
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <div class="space-y-4">
        <div class="flex items-center gap-4">
          <UInput v-model="query" icon="i-lucide-search" placeholder="Искать книги" class="w-full" />
        </div>

        <div class="flex gap-3 flex-wrap">
          <UButton
            v-for="t in tabs"
            :key="t.id"
            size="lg"
            :variant="activeTab === t.id ? 'solid' : 'outline'"
            :color="activeTab === t.id ? 'primary' : 'neutral'"
            class="rounded-xl"
            @click="activeTab = t.id"
          >
            {{ t.label }}
          </UButton>
        </div>
      </div>

      <!-- Рекомендации -->
      <div v-if="activeTab === 'recommendations'" class="mt-6">
        <RecommendationWidget />
      </div>

      <!-- Каталог -->
      <div v-else-if="activeTab === 'catalog'" class="mt-6">
        <CatalogTab :query="query" :books="books" />
      </div>

      <!-- Подборки -->
      <div v-else-if="activeTab === 'collections'" class="mt-6">
        <CollectionsTab :collections="collections" :books="books" />
      </div>

      <!-- Предметы -->
      <div v-else-if="activeTab === 'subjects'" class="mt-6">
        <SubjectsTab :collections="collections" :books="books" />
      </div>

      <!-- Книжный клуб -->
      <div v-else class="mt-6">
        <ClubTab :items="clubItems" />
      </div>
    </div>
  </div>
</template>
