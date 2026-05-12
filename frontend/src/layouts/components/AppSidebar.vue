<template>
  <aside class="fixed left-0 top-16 h-[calc(100vh-4rem)] w-64 bg-gray-100 shadow-lg overflow-y-auto">
    <nav>
      <ul class="pt-3">
        <li
          v-for="item in menuItems"
          :key="item.path"
          class="p-2 mb-1 ms-0.5 text-black hover:bg-blue-900 hover:text-white hover:rounded-s-xl active:bg-blue-900 active:text-white active:rounded-s-xl"
          :class="{ 'bg-blue-900 text-white rounded-s-xl': isActive(item.path) }"
        >
          <router-link :to="item.path" class="flex items-center gap-1">
            <UIcon :name="item.icon" class="size-7" />
            {{ item.label }}
          </router-link>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const menuItems = [
  { path: '/account', icon: 'material-symbols-light:person-outline-rounded', label: 'Мой аккаунт' },
  { path: '/orders', icon: 'prime:shopping-bag', label: 'Мои заказы' },
  { path: '/reviews', icon: 'mynaui:list', label: 'Мои рецензии' },
  { path: '/collections', icon: 'uil:clipboard-notes', label: 'Мои подборки' },
  { path: '/', icon: 'mdi:house-outline', label: 'Главная страница' },
];

// /book/:id и другие «вторичные» пути не входят в боковое меню — в этом случае
// держим подсветку на разделе, с которого пользователь сюда пришёл (роутер
// пишет последний «primary» путь в sessionStorage).
const activePath = computed(() => {
  const direct = menuItems.find(m => m.path === route.path);
  if (direct) return route.path;
  return sessionStorage.getItem('bookshelf_active_sidebar') || '/';
});

const isActive = path => activePath.value === path;
</script>
