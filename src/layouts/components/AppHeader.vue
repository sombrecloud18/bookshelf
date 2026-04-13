<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

const AUTH_TOKEN_KEY = 'bookshelf_auth_token';
const AUTH_ROLE_KEY = 'bookshelf_auth_role';

const router = useRouter();
const logoutOpen = ref(false);

const isAuthed = computed(() => !!localStorage.getItem(AUTH_TOKEN_KEY));
const role = computed(() => localStorage.getItem(AUTH_ROLE_KEY) || 'user');
const homePath = computed(() => (role.value === 'admin' ? '/admin' : '/'));

function requestLogout() {
  logoutOpen.value = true;
}

function logout() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem('bookshelf_auth_login');
  localStorage.removeItem(AUTH_ROLE_KEY);
  logoutOpen.value = false;
  router.push('/auth');
}
</script>

<template>
  <header class="bg-blue-900 p-4 flex justify-between fixed top-0 left-0 right-0 z-10 h-16">
    <router-link :to="homePath" class="flex items-center gap-2">
      <span class="text-white text-2xl font-thin">Bookshelf</span>
      <svg width="39" height="42" viewBox="0 0 39 54" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path
          d="M3.80085 0.586956C3.23561 0.821739 2.2079 1.76087 1.4885 2.6413L0.203859 4.22609L0.049702 26.4717C-0.104455 51.1826 -0.104455 51.2413 3.23561 53.0609C4.72579 53.8826 7.29507 54 21.9914 54C38.6403 54 39 54 39 52.8261C39 51.6522 38.6403 51.6522 21.7858 51.6522H4.52025L3.287 50.1848C1.59127 48.3065 1.64266 46.1935 3.33838 44.6674C4.67441 43.4935 5.29103 43.4348 21.8372 43.4348H39V21.7174V0L21.94 0.0586967C12.5364 0.0586967 4.36609 0.29348 3.80085 0.586956ZM36.9446 21.7174V41.0283L20.3984 41.2043C11.2518 41.3217 3.38977 41.6152 2.9273 41.9087C2.05374 42.3783 2.00236 41.3217 2.00236 23.8304V5.22392L3.287 3.81522L4.52025 2.34783H20.7581H36.9446V21.7174Z"
          fill="white"
        />
        <path
          d="M15.0031 10.6239C13.1018 11.9739 11.252 15.9652 11.252 18.6652C11.252 22.8913 13.6157 26.4718 17.2127 27.587L19.4737 28.35V31.7544C19.4737 34.8065 19.5764 35.2174 20.5014 35.2174C21.4263 35.2174 21.5291 34.8065 21.5291 31.8131V28.4674L24.0984 27.4109C27.2329 26.1196 29.2369 23.3609 29.5966 19.7218C30.1105 14.5565 27.5412 10.1544 23.7387 9.56741C21.9402 9.27393 21.5291 9.45002 20.6041 10.8587C19.5764 12.3261 19.4737 13.2065 19.4737 19.1348C19.4737 22.8326 19.3709 25.8261 19.2167 25.8261C19.0112 25.8261 18.2404 25.4152 17.4182 24.9457C15.3114 23.7131 14.1809 20.9544 14.4892 17.7848C14.6948 15.5544 15.1059 14.5565 17.1613 11.387C18.1376 9.86089 16.7502 9.39132 15.0031 10.6239ZM25.3316 13.6761C26.1538 14.9087 26.4107 16.2 26.4107 18.8413C26.4107 21.7174 26.2052 22.4805 25.1261 23.7131C24.4067 24.4761 23.3276 25.2978 22.7109 25.5326C21.5291 25.8848 21.5291 25.8848 21.5291 19.5457C21.5291 13.0891 21.9402 11.5044 23.5331 11.8565C23.9442 11.9739 24.715 12.7957 25.3316 13.6761Z"
          fill="white"
        />
      </svg>
    </router-link>

    <UButton v-if="isAuthed" color="secondary" @click="requestLogout">Выйти</UButton>
  </header>

  <UModal v-model:open="logoutOpen" title="Выход из аккаунта" class="z-100">
    <template #body>
      <div class="text-sm text-gray-700">Вы уверены, что хотите выйти?</div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-3 w-full">
        <UButton variant="outline" @click="logoutOpen = false">Отмена</UButton>
        <UButton color="error" @click="logout">Выйти</UButton>
      </div>
    </template>
  </UModal>
</template>
