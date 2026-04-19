<script setup>
import AppHeader from './components/AppHeader.vue';
import AdminSidebar from './components/AdminSidebar.vue';
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <AppHeader />
    <AdminSidebar />
    <main class="ml-64 pt-16">
      <router-view />
    </main>
  </div>
</template>
