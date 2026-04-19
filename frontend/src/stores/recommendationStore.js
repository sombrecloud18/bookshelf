// stores/recommendationsStore.js
import { ref } from 'vue';

export function useRecommendationsStore() {
  const recommendations = ref([]);
  const userPreferences = ref({
    favoriteGenres: [],
    favoriteAuthors: [],
    readingHistory: [],
  });
  const loading = ref(false);

  // Загрузка пользовательских предпочтений
  async function loadUserPreferences() {
    try {
      const response = await fetch('/api/user/preferences');
      const data = await response.json();
      userPreferences.value = data;
    } catch (error) {
      console.error('Ошибка загрузки предпочтений:', error);
    }
  }

  // Обновление предпочтений на основе действий пользователя
  async function updateUserPreferences(action) {
    try {
      await fetch('/api/user/preferences', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(action),
      });
      // Перезагружаем рекомендации после обновления предпочтений
      await loadRecommendations();
    } catch (error) {
      console.error('Ошибка обновления предпочтений:', error);
    }
  }

  // Загрузка рекомендаций
  async function loadRecommendations() {
    loading.value = true;
    try {
      const response = await fetch('/api/recommendations/personal');
      const data = await response.json();
      recommendations.value = data.recommendations;
    } catch (error) {
      console.error('Ошибка загрузки рекомендаций:', error);
    } finally {
      loading.value = false;
    }
  }

  return {
    recommendations,
    userPreferences,
    loading,
    loadUserPreferences,
    updateUserPreferences,
    loadRecommendations,
  };
}
