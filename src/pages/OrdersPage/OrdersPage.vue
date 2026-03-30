<script setup>
import Card from '../../components/Card.vue';
</script>

<template>
  <div class="min-h-screen p-8 bg-[#a3b5ff]">
    <div class="max-w-7xl mx-auto">
      <h1 class="text-4xl font-bold text-black mb-8">Мои заказы</h1>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 auto-rows-fr">
        <Card
          title="Скорбь сатаны"
          genre="Роман"
          image-url="https://cdn21vek.by/img/galleries/9475/355/eksmo_9475355_ecbee5c8328985f84402e430947ecd9e.jpg"
        />
        <Card
          title='Проект "Ave Maria"'
          genre="Научная фантастика"
          image-url="https://cdn.litres.ru/pub/c/cover_415/66986536.jpg"
        />
        <Card
          title="Голодные игры"
          genre="Фэнтези"
          image-url="https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488"
        />
        <Card
          title="Правда о деле Гарри Квеберта"
          genre="Детектив"
          image-url="https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig"
        />
        <Card
          title="Мастер и Маргарита"
          genre="Роман"
          image-url="https://imo10.labirint.ru/books/668307/cover.jpg/242-0"
        />
        <Card
          title="Bookship"
          genre="Фантастика"
          image-url="https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg"
        />
        <Card
          title="Институт"
          genre="Фантастика"
          image-url="https://bukvaeshka.by/upload/medialibrary/39d/pe1bza59e83k3q5cvo5a47k0rw32tem1.jpg"
        />
        <Card
          title="Шестерка воронов"
          genre="Фэнтези"
          image-url="https://imo10.labirint.ru/books/635534/cover.jpg/242-0"
        />
      </div>
    </div>
  </div>
</template>
