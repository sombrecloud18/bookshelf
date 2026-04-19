// Константы для цветов жанров
export const GENRE_COLORS = {
  Роман: 'bg-blue-100 text-blue-800',
  Фантастика: 'bg-purple-100 text-purple-800',
  Детектив: 'bg-red-100 text-red-800',
  Фэнтези: 'bg-green-100 text-green-800',
  'Научная фантастика': 'bg-indigo-100 text-indigo-800',
  Триллер: 'bg-orange-100 text-orange-800',
  Ужасы: 'bg-pink-100 text-pink-800',
  Поэзия: 'bg-yellow-100 text-yellow-800',
  Драма: 'bg-teal-100 text-teal-800',
  Комедия: 'bg-lime-100 text-lime-800',
  Исторический: 'bg-amber-100 text-amber-800',
  Биография: 'bg-emerald-100 text-emerald-800',
  Приключения: 'bg-cyan-100 text-cyan-800',
  Мистика: 'bg-violet-100 text-violet-800',
};

// Функция для получения цвета жанра
export const getGenreColor = genre => {
  return GENRE_COLORS[genre] || 'bg-gray-100 text-gray-800';
};
