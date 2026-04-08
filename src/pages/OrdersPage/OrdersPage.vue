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
          author="Мария Корелли"
          description="Роман о талантливом писателе, сделке с совестью и цене успеха."
          :year="1895"
        />
        <Card
          title='Проект "Ave Maria"'
          genre="Научная фантастика"
          image-url="https://cdn.litres.ru/pub/c/cover_415/66986536.jpg"
          author="Энди Вейер"
          description="Научная фантастика о спасении человечества и неожиданных союзниках."
          :year="2021"
        />
        <Card
          title="Голодные игры"
          genre="Фэнтези"
          image-url="https://igromaster.by/upload/iblock/04f/04f3eacdf9593988d8e4f85bb6402705.webp?1602251488"
          author="Сьюзен Коллинз"
          description="Антиутопия о выживании, выборе и цене власти."
          :year="2008"
        />
        <Card
          title="Правда о деле Гарри Квеберта"
          genre="Детектив"
          image-url="https://avatars.mds.yandex.net/get-mpic/16148264/2a0000019b7c29f4486905ed413485483f01/orig"
          author="Жоэль Диккер"
          description="Детектив о тайнах прошлого и расследовании, которое меняет всё."
          :year="2012"
        />
        <Card
          title="Мастер и Маргарита"
          genre="Роман"
          image-url="https://imo10.labirint.ru/books/668307/cover.jpg/242-0"
          author="Михаил Булгаков"
          description="Классика: любовь, сатира и мистические события в Москве."
          :year="1967"
        />
        <Card
          title="Bookship"
          genre="Фантастика"
          image-url="https://s2-goods.ozstatic.by/1000/333/453/101/101453333_0.jpg"
          author="—"
          description="Фантастическая история о приключениях и поиске своего пути."
          year="—"
        />
        <Card
          title="Институт"
          genre="Фантастика"
          image-url="https://imo10.labirint.ru/books/903891/cover.jpg/242-0"
          author="Стивен Кинг"
          description="Триллер о детях с необычными способностями и тайной организации."
          :year="2019"
        />
        <Card
          title="Шестерка воронов"
          genre="Фэнтези"
          image-url="https://imo10.labirint.ru/books/635534/cover.jpg/242-0"
          author="Ли Бардуго"
          description="Фэнтези об ограблении века, команде и высоких ставках."
          :year="2015"
        />
      </div>
    </div>
  </div>
</template>
