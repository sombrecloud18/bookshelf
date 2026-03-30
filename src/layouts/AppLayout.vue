<script setup lang="ts">
import AppHeader from './components/AppHeader.vue';
import AppSidebar from './components/AppSidebar.vue';
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <AppHeader />
    <AppSidebar />
    <main class="ml-64 pt-16">
      <div>
        <router-view />
      </div>
    </main>
  </div>
</template>
