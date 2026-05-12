// Константы для цветов жанров. Известные жанры зафиксированы — это сохраняет
// привычный пользователю цвет (Роман → синий, Детектив → красный и т.д.).
// Незнакомым жанрам автоматически назначаем цвет из расширенного пула — по
// детерминированному хешу названия, чтобы один и тот же жанр всегда получал
// один и тот же оттенок.
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

// Расширенный пул для незнакомых жанров. Каждая строка — полный набор классов
// Tailwind, чтобы сканер их подхватил при сборке (динамическая конкатенация
// классов Tailwind v4 бы не нашёл).
const FALLBACK_PALETTE = [
  'bg-rose-100 text-rose-800',
  'bg-sky-100 text-sky-800',
  'bg-fuchsia-100 text-fuchsia-800',
  'bg-stone-100 text-stone-800',
  'bg-slate-100 text-slate-800',
  'bg-zinc-100 text-zinc-800',
  'bg-neutral-100 text-neutral-800',
  // Используем повторно цвета из основной палитры — это норма: если новых
  // жанров много, повторы неизбежны, но детерминированный hash гарантирует
  // стабильность.
  'bg-blue-100 text-blue-800',
  'bg-purple-100 text-purple-800',
  'bg-green-100 text-green-800',
  'bg-orange-100 text-orange-800',
  'bg-pink-100 text-pink-800',
  'bg-yellow-100 text-yellow-800',
  'bg-teal-100 text-teal-800',
  'bg-lime-100 text-lime-800',
  'bg-amber-100 text-amber-800',
  'bg-emerald-100 text-emerald-800',
  'bg-cyan-100 text-cyan-800',
  'bg-violet-100 text-violet-800',
  'bg-indigo-100 text-indigo-800',
];

function hashString(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = (h * 31 + str.charCodeAt(i)) | 0;
  }
  return Math.abs(h);
}

export const getGenreColor = genre => {
  if (!genre) return 'bg-gray-100 text-gray-800';
  if (GENRE_COLORS[genre]) return GENRE_COLORS[genre];
  const idx = hashString(genre) % FALLBACK_PALETTE.length;
  return FALLBACK_PALETTE[idx];
};
