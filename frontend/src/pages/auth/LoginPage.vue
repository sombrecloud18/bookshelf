<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppHeader from '../../layouts/components/AppHeader.vue';
import { api } from '../../api.js';

const AUTH_TOKEN_KEY = 'bookshelf_auth_token';
const AUTH_ROLE_KEY = 'bookshelf_auth_role';

const router = useRouter();
const loading = ref(false);
const error = ref(null);

const form = reactive({
  login: '',
  password: '',
});

async function submit() {
  error.value = null;
  if (!form.login.trim() || !form.password) {
    error.value = 'Введите логин и пароль.';
    return;
  }

  loading.value = true;
  try {
    const data = await api.post('/auth/login', {
      login: form.login.trim(),
      password: form.password,
    });
    localStorage.setItem(AUTH_TOKEN_KEY, data.token);
    localStorage.setItem(AUTH_ROLE_KEY, data.role);
    localStorage.setItem('bookshelf_auth_login', data.user.login);
    localStorage.setItem('bookshelf_profile', JSON.stringify(data.user));
    router.push(data.role === 'admin' ? '/admin' : '/');
  } catch (e) {
    error.value = e.message || 'Неверный логин или пароль';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#a3b5ff]">
    <AppHeader />
    <main class="pt-20">
      <div class="max-w-7xl mx-auto px-8">
        <div class="flex justify-center">
          <UCard variant="soft" class="mt-6 bg-white rounded-2xl p-6 w-full max-w-3xl">
            <div class="space-y-4">
              <h1 class="text-black font-extrabold tracking-wide text-3xl uppercase">Вход</h1>
              <div class="flex items-center justify-between gap-4">
                <h2 class="text-lg font-semibold text-black">Данные для входа</h2>
                <router-link class="text-black underline" to="/registration"> Нет аккаунта? Регистрация </router-link>
              </div>

            <div class="grid grid-cols-1 gap-4 max-w-xl">
              <UFormField label="Логин" name="login">
                <UInput
                  v-model="form.login"
                  placeholder="Введите логин"
                  highlight
                  variant="outline"
                  size="xl"
                  class="w-full"
                />
              </UFormField>

              <UFormField label="Пароль" name="password">
                <UInput
                  v-model="form.password"
                  type="password"
                  placeholder="Password"
                  highlight
                  variant="outline"
                  size="xl"
                  class="w-full"
                />
              </UFormField>
            </div>

            <UAlert v-if="error" color="error" variant="soft" :description="error" class="max-w-xl" />

            <div class="flex gap-3 pt-2">
              <UButton :loading="loading" class="bg-[#e9b6f0] text-black rounded-md px-10" size="lg" @click="submit">
                Войти
              </UButton>
              <UButton variant="outline" color="neutral" class="rounded-md" size="lg" to="/registration">
                Регистрация
              </UButton>
            </div>
            </div>
          </UCard>
        </div>
      </div>
    </main>
  </div>
</template>
