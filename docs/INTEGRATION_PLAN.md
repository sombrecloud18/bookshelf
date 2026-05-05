# ПЛАН ИНТЕГРАЦИИ ФРОНТЕНДА С БЕКЕНДОМ

Фронтенд — эталон. Все его страницы сейчас работают на моковых данных (hardcoded arrays, localStorage). Задача — заменить моки реальными API-вызовами к Spring Boot бекенду, при необходимости дорабатывая бекенд.

---

## ФАЗА 0: ИНФРАСТРУКТУРА (сделать ПЕРВОЙ)

### 0.1. Создать API-утилиту `frontend/src/api.js`

Создать файл `frontend/src/api.js` — единая точка для всех HTTP-запросов:

```js
const API_BASE = '/api';
const AUTH_TOKEN_KEY = 'bookshelf_auth_token';

async function request(path, options = {}) {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const headers = { ...options.headers };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401) {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem('bookshelf_auth_role');
    window.location.href = '/auth';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
};
```

Все компоненты должны использовать `api` вместо прямого `fetch`.

### 0.2. Настроить Vite proxy

В `frontend/vite.config.js` добавить proxy для `/api`:

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true,
    },
  },
},
```

**Важно**: бекенд работает на порту 8088 (не 8080!).

### 0.3. Добавить `login` поле в форму регистрации (Backend fix)

**Проблема**: `RegisterRequestDTO` на бекенде требует поле `login` (3-100 символов, обязательное), но фронтенд-форма регистрации (`RegistrationPage.vue`) не имеет поля "логин" — только `email`, `firstName`, `lastName` и т.д.

**Решение**: Добавить поле "Логин" в форму регистрации на фронтенде (между "Почта" и "Пароль"), чтобы пользователь вводил уникальный логин. При отправке формы поле `login` должно передаваться в запросе.

---

## ФАЗА 1: АУТЕНТИФИКАЦИЯ

### 1.1. Подключить страницу логина (`frontend/src/pages/auth/LoginPage.vue`)

**Текущее состояние**: Моковая авторизация. `admin/admin` = роль admin, иначе = user. Токен генерируется как `token_{timestamp}`.

**Что сделать**:
1. Импортировать `api` из `@/api.js`
2. В функции `submit()` заменить мок на:
```js
const data = await api.post('/auth/login', { login: form.login.trim(), password: form.password });
```
3. Из ответа `data` сохранить:
   - `localStorage.setItem(AUTH_TOKEN_KEY, data.token)` — настоящий JWT
   - `localStorage.setItem(AUTH_ROLE_KEY, data.role)` — бекенд вернёт `"user"` или `"admin"`
   - `localStorage.setItem('bookshelf_auth_login', data.user.login)`
   - `localStorage.setItem('bookshelf_profile', JSON.stringify(data.user))`
4. При ошибке показать `error.value = 'Неверный логин или пароль'`
5. Убрать `await new Promise(r => setTimeout(r, 300))` — это моковая задержка

**Бекенд-эндпоинт**: `POST /api/auth/login`
- Request: `{ "login": "...", "password": "..." }`
- Response: `{ "token": "jwt...", "role": "user"|"admin", "user": { id, login, email, firstName, lastName, ... } }`

**Проверить на бекенде**: убедиться, что `AuthResponseDTO` при роли `MODERATOR` возвращает `role = "admin"` (а не `"moderator"`), иначе фронтенд не перенаправит на `/admin`. Если нет — исправить маппинг в `UserService.login()`.

### 1.2. Подключить страницу регистрации (`frontend/src/pages/auth/RegistrationPage.vue`)

**Текущее состояние**: Сохраняет данные только в localStorage, генерирует фейковый токен.

**Что сделать**:
1. Добавить поле `login` в `form` reactive и в шаблон (UInput)
2. В `submit()` заменить мок на:
```js
const data = await api.post('/auth/register', {
  login: form.login.trim(),    // НОВОЕ ПОЛЕ
  email: form.email.trim(),
  password: form.password,
  firstName: form.firstName.trim(),
  lastName: form.lastName.trim(),
  patronymic: form.patronymic.trim() || null,
  faculty: form.faculty || null,
  specialty: form.specialty || null,
  course: form.course || null,
  phoneNumber: form.phoneNumber.trim() || null,
  avatarUrl: form.avatarUrl.trim() || null,
});
```
3. Из ответа сохранить token, role, user в localStorage (как в логине)
4. Обработать ошибки (409 = логин/email уже занят)
5. Убрать моковую задержку

**Бекенд-эндпоинт**: `POST /api/auth/register`
- Response: `AuthResponseDTO` с token, role, user

### 1.3. Logout (`frontend/src/layouts/components/AppHeader.vue`)

**Текущее состояние**: Уже реализован корректно — очищает localStorage и редиректит на `/auth`.

**Что сделать**: Ничего, logout на JWT-бекенде — чисто клиентская операция.

---

## ФАЗА 2: КАТАЛОГ И КНИГИ

### 2.1. Загрузка каталога книг (`frontend/src/pages/MainPage/MainPage.vue`)

**Текущее состояние**: Захардкоженный массив `books` из 8 книг со строковыми ID (`'book-satan'`, `'book-master'` и т.д.).

**Что сделать**:
1. Сделать `books` реактивным (`ref([])`) вместо константного массива
2. В `onMounted` вызвать:
```js
const res = await api.get('/books');
books.value = res.content; // Page<BookDTO> — данные в поле content
```
3. Передавать `books` в дочерние компоненты как и раньше (через props)
4. Поиск: при изменении `query` вызывать `api.get('/books?query=' + encodeURIComponent(query))` (можно с debounce)

**Маппинг полей** (бекенд → фронтенд):
- `id` (UUID) → `id` — **тип изменится со строки на UUID**, это повлияет на роутинг `/book/:id`
- `title`, `author`, `genre`, `year`, `description` — совпадают
- `imageUrl` → `imageUrl` — совпадает
- `coverUrl`, `fullDescription`, `pages`, `publisher`, `publishYear`, `isbn`, `averageRating` — дополнительные поля, уже используются в `BookPage.vue`

**Важно**: После перехода на UUID-идентификаторы, все hardcoded `bookIds` в других компонентах перестанут работать. Это нормально — они тоже будут заменены API-вызовами.

### 2.2. Поиск книг в каталоге (`CatalogTab.vue`)

**Текущее состояние**: Фильтрация по массиву через `computed`.

**Что сделать**: Поиск уже работает через props `query` и `books`, передаваемых из `MainPage.vue`. Если `MainPage` будет выполнять поиск через API, `CatalogTab` можно оставить как есть (получает отфильтрованный массив). Либо можно оставить клиентский поиск и загружать все книги разом — зависит от объёма каталога.

### 2.3. Страница книги (`frontend/src/pages/user/BookPage.vue`)

**Текущее состояние**: Огромный объект `booksMock` с захардкоженными книгами. Рецензии — моковый массив. Всё клиентское.

**Что сделать**:
1. Убрать `booksMock` полностью
2. В `onMounted`:
```js
const bookId = route.params.id;
// Загрузить книгу
book.value = await api.get(`/books/${bookId}`);
// Загрузить рецензии
const reviewsData = await api.get(`/reviews/book/${bookId}?page=0&size=20`);
reviews.value = reviewsData.content;
loading.value = false;
```
3. Отправка рецензии (`submitReview`):
```js
const data = await api.post('/reviews', {
  bookId: book.value.id,
  rating: newReview.value.rating,
  text: newReview.value.text,
});
reviews.value.unshift(data);
```
4. Лайк рецензии (`likeReview`):
```js
await api.post(`/reviews/${reviewId}/like`);
review.likes++;
```
5. Отправка комментария (`submitComment`):
```js
const comment = await api.post(`/comments/review/${reviewId}`, { text: newCommentText.value });
review.comments.unshift(comment);
```
6. Лайк комментария:
```js
await api.post(`/comments/${commentId}/like`);
comment.likes++;
```

**Бекенд-нюанс**: Рецензия создаётся со статусом `PENDING` и не будет видна в `GET /reviews/book/{id}` (который показывает только `APPROVED`). Нужно показать пользователю сообщение: "Ваша рецензия отправлена на модерацию".

**Бекенд-нюанс**: `CreateReviewDTO` требует `text` минимум 200 символов. Добавить валидацию на фронтенде (показать ошибку, если текст < 200 символов).

### 2.4. Похожие книги (`frontend/src/pages/user/components/BookRecommentations.vue`)

**Текущее состояние**: Вызывает `fetch(/api/recommendations/similar/${bookId})`, ожидает `data.similar`.

**Что исправить**:
- Бекенд возвращает `List<RecommendationItemDTO>` (плоский массив), а не `{ similar: [...] }`
- Заменить `similarBooks.value = data.similar` на `similarBooks.value = data`
- Использовать `api.get(...)` вместо прямого `fetch`

---

## ФАЗА 3: ЗАКАЗЫ (СПИСОК ЧТЕНИЯ)

### 3.1. Заказы — добавление/удаление (`frontend/src/pages/MainPage/components/CatalogTab.vue`)

**Текущее состояние**: Заказы хранятся в `localStorage['userOrders']` как массив строковых ID.

**Что сделать**:
1. При монтировании загрузить заказы:
```js
const ordersData = await api.get('/orders');
orderedBooks.value = new Set(ordersData.map(b => b.id));
```
2. `toggleOrder(bookId)`:
```js
if (orderedBooks.value.has(bookId)) {
  await api.delete(`/orders/${bookId}`);
  orderedBooks.value.delete(bookId);
} else {
  await api.post('/orders', { bookId });
  orderedBooks.value.add(bookId);
}
```
3. Убрать `saveOrders()` и `localStorage`

### 3.2. Страница заказов (`frontend/src/pages/OrdersPage/OrdersPage.vue`)

**Текущее состояние**: Загружает ID из `localStorage['userOrders']`, ищет книги по ID в захардкоженном массиве `allBooks`.

**Что сделать**:
1. Убрать `allBooks` массив
2. В `onMounted`:
```js
orderedBooks.value = await api.get('/orders');
```
Бекенд `GET /api/orders` возвращает `List<BookDTO>` — полные данные книг.
3. Удаление: заменить `removeFromOrders(bookId)`:
```js
await api.delete(`/orders/${bookId}`);
orderedBooks.value = orderedBooks.value.filter(b => b.id !== bookId);
```
4. Убрать всё взаимодействие с `localStorage`

---

## ФАЗА 4: РЕЦЕНЗИИ ПОЛЬЗОВАТЕЛЯ

### 4.1. Страница рецензий (`frontend/src/pages/ReviewsPage/ReviewsPage.vue`)

**Текущее состояние**: Захардкоженные массивы `booksCatalog` и `reviews`. Все CRUD-операции — в памяти.

**Что сделать**:
1. Загрузка рецензий при монтировании:
```js
const data = await api.get('/reviews/my?page=0&size=50');
reviews.splice(0, reviews.length, ...data.content);
```
2. Для каталога книг (модалка "Добавить рецензию"):
```js
// Загрузить книги для выбора
const booksData = await api.get('/books?size=100');
booksCatalog = booksData.content;
```
3. Создание рецензии (`addReview`):
```js
const data = await api.post('/reviews', {
  bookId: selectedBook.value.id,
  rating: addRating.value,
  text: addText.value.trim(),
});
reviews.unshift(data);
```
4. Обновление рецензии (`save`):
```js
const data = await api.put(`/reviews/${editingId.value}`, {
  rating: current.value.rating,
  text: current.value.text,
});
```
5. Удаление рецензии (`deleteReview`):
```js
await api.delete(`/reviews/${id}`);
```

**Маппинг полей ReviewDTO**: Бекенд возвращает `{ id, bookId, bookTitle, author, genre, coverUrl, rating, text, status, likes, createdAt, comments }`. Фронтенд ReviewsPage уже использует те же имена полей (`bookTitle`, `author`, `genre`, `coverUrl`, `rating`, `text`, `createdAt`).

**Нюанс**: Показать статус рецензии (`PENDING`, `APPROVED`, `REJECTED`). Бекенд возвращает поле `status` — добавить бейдж статуса в карточку рецензии.

---

## ФАЗА 5: ПОДБОРКИ ПОЛЬЗОВАТЕЛЯ

### 5.1. Страница подборок (`frontend/src/pages/CollectionsPage/CollectionPage.vue`)

**Текущее состояние**: Захардкоженные `allBooks` и `collections`. Drag-and-drop для добавления книг.

**Что сделать**:
1. Загрузка подборок при монтировании:
```js
collections.value = await api.get('/collections/my');
```
2. Загрузка каталога книг для добавления:
```js
const booksData = await api.get('/books?size=200');
allBooks = booksData.content.map(b => ({ id: b.id, title: b.title, coverUrl: b.coverUrl || b.imageUrl, shortDescription: b.description }));
```
3. Создание подборки (`createCollection`):
```js
const data = await api.post('/collections', {
  title: createTitle.value.trim(),
  genre: createGenre.value.trim() || null,
  description: createDescription.value.trim() || null,
  bookIds: createSelectedIds.value,
});
collections.value.push(data);
```
4. Обновление (`saveEdit`):
```js
const data = await api.put(`/collections/${editingCollectionId.value}`, {
  title: editTitle.value.trim(),
  genre: editGenre.value.trim() || null,
  description: editDescription.value.trim() || null,
  bookIds: editSelectedIds.value,
});
// Обновить в массиве
```
5. Удаление (`deleteCollectionById`):
```js
await api.delete(`/collections/${collectionId}`);
```
6. Убрать `allBooks` hardcoded массив
7. `booksById` — вычислять из загруженного каталога

**CollectionDTO** бекенда: `{ id, userId, title, genre, description, status, bookIds, author, authorName, createdAt }` — совместимо с фронтендом.

---

## ФАЗА 6: АККАУНТ ПОЛЬЗОВАТЕЛЯ

### 6.1. Страница аккаунта (`frontend/src/pages/AccountPage/AccountPage.vue`)

**Текущее состояние**: Захардкоженные данные пользователя. Нет API-вызовов.

**Что сделать**:
1. Загрузка профиля при монтировании:
```js
const profile = await api.get('/users/me');
// Заполнить userData и formData из profile
userData.fullName = [profile.lastName, profile.firstName, profile.patronymic].filter(Boolean).join(' ');
userData.studyInfo = [profile.faculty, profile.specialty, profile.course].filter(Boolean).join(', ');
userData.email = profile.email;
userData.phoneNumber = profile.phoneNumber;
userData.avatarUrl = profile.avatarUrl;
userData.role = profile.role === 'admin' ? 'Модератор' : 'Студент';
// + заполнить formData
```
2. Сохранение (`saveUserData`):
```js
await api.put('/users/me', {
  firstName: formData.firstName,
  lastName: formData.lastName,
  patronymic: formData.patronymic || null,
  faculty: formData.faculty || null,
  specialty: formData.specialty || null,
  course: formData.course || null,
  phoneNumber: formData.phoneNumber || null,
  email: formData.email,
});
```
3. Смена пароля — вынести в отдельную форму или кнопку:
```js
await api.put('/users/me/password', {
  currentPassword: formData.currentPassword,
  newPassword: formData.newPassword,
});
```
4. Убрать захардкоженные данные в `userData`

---

## ФАЗА 7: РЕКОМЕНДАЦИИ

### 7.1. Виджет рекомендаций (`frontend/src/pages/MainPage/components/RecommendationWidget.vue`)

**Текущее состояние**: Моковые функции `getMockPersonalRecommendations()` и т.д.

**Что сделать**:
1. В `fetchPersonalRecommendations`:
```js
const data = await api.get('/recommendations');
personalRecommendations.value = data.personal || [];
popularBooks.value = data.popular || [];
newBooks.value = data.newBooks || [];
```
2. Убрать все `getMock*` функции
3. Убрать `setTimeout`

**Маппинг**: Бекенд `RecommendationResponseDTO` возвращает `{ personal, popular, newBooks }`. `RecommendationItemDTO` имеет поля `{ id, bookId, title, genre, author, imageUrl, description, matchScore, views, addedDate }` — совместимо с тем, что ожидает фронтенд.

### 7.2. Стор рекомендаций (`frontend/src/stores/recommendationStore.js`)

**Текущее состояние**: URL-пути не совпадают с бекендом.

**Исправить URL-пути**:
- `GET /api/user/preferences` → `GET /api/recommendations/preferences`
- `POST /api/user/preferences` → `PUT /api/recommendations/preferences` (метод тоже другой!)
- `GET /api/recommendations/personal` → `GET /api/recommendations`

**Или**: если этот стор не используется (он действительно нигде не импортируется в компонентах), можно удалить его и интегрировать вызовы напрямую в `RecommendationWidget.vue`.

---

## ФАЗА 8: ПУБЛИЧНЫЕ ПОДБОРКИ И ПРЕДМЕТЫ

### 8.1. Вкладка "Подборки" на главной (`frontend/src/pages/MainPage/components/CollectionsTab.vue`)

**Текущее состояние**: Получает `collections` и `books` как props из `MainPage.vue` (захардкоженные).

**Что сделать в MainPage.vue**:
1. Загрузить публичные подборки:
```js
const collectionsData = await api.get('/collections?page=0&size=12');
collections = collectionsData.content;
```
2. Для отображения обложек книг в подборках, загрузить книги по `bookIds`:
```js
// Собрать все bookIds из подборок
const allBookIds = collections.flatMap(c => c.bookIds);
if (allBookIds.length > 0) {
  const booksData = await api.post('/books/by-ids', { ids: [...new Set(allBookIds)] });
  // Создать маппинг по ID
}
```
3. Передать как props в `CollectionsTab`

### 8.2. Вкладка "Предметы" (`frontend/src/pages/MainPage/components/SubjectsTab.vue`)

**Текущее состояние**: Захардкоженные предметы по специальностям. Показывает те же `collections` из `MainPage`.

**Что сделать**:
1. При выборе предмета и специальности загрузить подборки по предмету:
```js
const data = await api.get(`/subject-collections?subject=${encodeURIComponent(subject)}&specialty=${encodeURIComponent(specialty)}`);
subjectCollections.value = data.content;
```
2. Загрузить книги для подборок через `POST /books/by-ids`
3. Предметы/специальности пока можно оставить захардкоженными (они зависят от учебного заведения)

### 8.3. Вкладка "Книжный клуб" (`frontend/src/pages/MainPage/components/ClubTab.vue`)

**Текущее состояние**: Получает `items` из `MainPage.vue` (захардкоженный массив `clubItems`).

**Что сделать в MainPage.vue**:
1. Загрузить мероприятия:
```js
const eventsData = await api.get('/events?page=0&size=10');
clubItems = eventsData.content.map(e => ({
  id: e.id,
  title: e.title,
  text: e.description,
  participantsCount: e.currentParticipants,
  date: e.date,
  time: e.time,
  location: e.location,
  maxParticipants: e.maxParticipants,
}));
```
2. В `ClubTab.vue` — "Подтвердить участие":
```js
await api.post(`/events/${selectedId.value}/register`);
```
3. "Я не приду":
```js
await api.delete(`/events/${selectedId.value}/register`);
```
4. Проверить статус регистрации:
```js
const { registered } = await api.get(`/events/${id}/registered`);
```

---

## ФАЗА 9: АДМИН-ПАНЕЛЬ

### 9.1. Дашборд (`frontend/src/pages/admin/AdminHomePage.vue`)

**Текущее состояние**: Захардкоженные `stats` (3 рецензии, 2 подборки, 2 мероприятия).

**Что сделать**:
1. Загрузить статистику:
```js
const data = await api.get('/admin/dashboard');
stats = [
  { label: 'Рецензий на модерации', value: data.pendingReviews },
  { label: 'Подборок на модерации', value: data.pendingCollections },
  { label: 'Подборок по предметам на модерации', value: data.pendingSubjectCollections },
  { label: 'Активных мероприятий', value: data.upcomingEvents },
  { label: 'Всего пользователей', value: data.totalUsers },
  { label: 'Всего книг', value: data.totalBooks },
];
```

### 9.2. Модерация рецензий (`frontend/src/pages/admin/ModerationReviewsPage.vue`)

**Текущее состояние**: Захардкоженный `reviewsQueue`.

**Что сделать**:
1. Загрузка:
```js
const data = await api.get('/reviews/pending?page=0&size=20');
reviewsQueue.value = data.content;
```
2. Одобрение:
```js
await api.post(`/reviews/${id}/approve`);
reviewsQueue.value = reviewsQueue.value.filter(r => r.id !== id);
```
3. Отклонение:
```js
await api.post(`/reviews/${id}/reject`);
reviewsQueue.value = reviewsQueue.value.filter(r => r.id !== id);
```

**Маппинг**: ReviewDTO бекенда содержит `bookTitle`, `author`, `genre`, `coverUrl`, `rating`, `text`, `reviewAuthor`, `reviewAuthorName`, `createdAt`, `status` — все поля совпадают с тем, что использует фронтенд.

### 9.3. Модерация подборок (`frontend/src/pages/admin/ModerationCollectionsPage.vue`)

**Текущее состояние**: Захардкоженный `collectionsQueue`.

**Что сделать**:
1. Загрузка:
```js
const data = await api.get('/collections/pending?page=0&size=20');
collectionsQueue.value = data.content;
```
2. Загрузить книги для отображения обложек:
```js
const allIds = collectionsQueue.value.flatMap(c => c.bookIds);
if (allIds.length > 0) {
  const books = await api.post('/books/by-ids', { ids: [...new Set(allIds)] });
  booksById = Object.fromEntries(books.map(b => [b.id, { id: b.id, title: b.title, coverUrl: b.coverUrl || b.imageUrl, shortDescription: b.description }]));
}
```
3. Одобрение:
```js
await api.post(`/collections/${id}/approve`);
collectionsQueue.value = collectionsQueue.value.filter(c => c.id !== id);
```
4. Отклонение:
```js
await api.post(`/collections/${id}/reject`);
collectionsQueue.value = collectionsQueue.value.filter(c => c.id !== id);
```
5. Убрать `allBooks` hardcoded массив

### 9.4. Модерация подборок по предметам (`frontend/src/pages/admin/ModerationSubjectCollectionsPage.vue`)

**Текущее состояние**: Использует `subjectCollectionsStore.js` (мок-стор).

**Что сделать**:
1. Загрузка:
```js
const data = await api.get('/subject-collections/pending?page=0&size=20');
pendingCollections.value = data.content;
```
2. Одобрение с комментарием:
```js
await api.post(`/subject-collections/${id}/approve`, { moderatorComment: comment || null });
```
3. Отклонение с причиной:
```js
await api.post(`/subject-collections/${id}/reject`, { moderatorComment: reason });
```
4. Загрузка книг: аналогично через `POST /books/by-ids`
5. Убрать или рефакторить `subjectCollectionsStore.js`

### 9.5. Управление мероприятиями (`frontend/src/pages/admin/EventsPage.vue`)

**Текущее состояние**: Моковые данные. Все CRUD — в памяти.

**Что сделать**:
1. Загрузка:
```js
const data = await api.get('/events?page=0&size=50');
events.value = data.content;
```
2. Создание (`addEvent`):
```js
const data = await api.post('/events', {
  title: form.value.title.trim(),
  description: form.value.description.trim() || null,
  date: form.value.date,
  time: form.value.time || null,
  location: form.value.location.trim() || null,
  maxParticipants: form.value.maxParticipants ? Number(form.value.maxParticipants) : null,
});
events.value.unshift(data);
```
3. Обновление (`saveEdit`):
```js
const data = await api.put(`/events/${selectedEvent.value.id}`, { ...selectedEvent.value });
// Обновить в массиве
```
4. Удаление (`removeEvent`):
```js
await api.delete(`/events/${id}`);
events.value = events.value.filter(e => e.id !== id);
```

**Маппинг EventDTO**: `{ id, title, description, date, time, location, maxParticipants, currentParticipants, organizer }` — совпадает с фронтендом.

---

## ФАЗА 10: ДОРАБОТКИ БЕКЕНДА

### 10.1. Проверить маппинг ролей в AuthResponseDTO

В `UserService.login()` и `UserService.register()` убедиться, что:
- Роль `MODERATOR` маппится в `role = "admin"` в `AuthResponseDTO`
- Роль `USER` маппится в `role = "user"`

Фронтенд сравнивает `role === 'admin'` в роутере. Если бекенд возвращает `"moderator"`, ничего не заработает.

### 10.2. CORS для порта 5173

Проверить `application.properties`:
```
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```
Порт 5173 — это Vite dev server. Но если используется Vite proxy (фаза 0.2), CORS не нужен для dev — все запросы идут через прокси.

### 10.3. Вернуть поле `login` из формы регистрации

Фронтенд регистрации НЕ отправляет поле `login`. Бекенд `RegisterRequestDTO` требует его как обязательное (@NotBlank, @Size(min=3, max=100)).

**Варианты решения** (выбрать один):
- **(A)** Добавить поле "Логин" в форму регистрации на фронтенде (рекомендуется)
- **(B)** Сделать `login` необязательным на бекенде и автогенерировать из email

### 10.4. BookDTO — поле `id` как строка в JSON

Бекенд использует `UUID` для ID книг. Фронтенд использовал строковые ID (`'book-satan'`). После интеграции все ID будут UUID-строками (`"550e8400-e29b-41d4-a716-446655440000"`). Это автоматически сериализуется Jackson'ом как строка — проблем быть не должно.

### 10.5. Проверить что `GET /api/recommendations` требует авторизации

Эндпоинт `/api/recommendations` не в списке `permitAll()` в `SecurityConfig`, а значит требует токен. Фронтенд будет отправлять токен через `api.js` — ОК.

Но `GET /api/recommendations/similar/{bookId}` — в `permitAll()` (публичный) — ОК.

### 10.6. Убедиться что `organizer` заполняется для events

В `EventsPage.vue` фронтенд-формы не содержит поле organizer — хардкодят `'Администратор'`. Бекенд `CreateEventDTO` имеет поле `organizer` (опциональное). Можно оставить как есть — бекенд `EventService.createEvent()` автоматически устанавливает `createdBy` из токена.

---

## ЧЕКЛИСТ ПОРЯДКА ВЫПОЛНЕНИЯ

1. [ ] **Фаза 0**: Создать `api.js`, настроить Vite proxy, добавить поле `login` в регистрацию
2. [ ] **Фаза 1**: Подключить логин и регистрацию
3. [ ] **Проверка**: Зарегистрировать пользователя → залогиниться → убедиться что токен сохраняется и роутер работает
4. [ ] **Фаза 2**: Каталог книг и страница книги
5. [ ] **Проверка**: Открыть каталог → увидеть книги из БД → перейти на страницу книги → увидеть детали
6. [ ] **Фаза 3**: Заказы
7. [ ] **Проверка**: Добавить книгу в заказы → открыть страницу заказов → увидеть книгу → удалить
8. [ ] **Фаза 4**: Рецензии пользователя
9. [ ] **Проверка**: Создать рецензию → увидеть на странице рецензий → отредактировать → удалить
10. [ ] **Фаза 5**: Подборки пользователя
11. [ ] **Проверка**: Создать подборку → добавить книги → отредактировать → удалить
12. [ ] **Фаза 6**: Аккаунт
13. [ ] **Проверка**: Открыть аккаунт → увидеть данные из БД → отредактировать → сохранить
14. [ ] **Фаза 7**: Рекомендации
15. [ ] **Фаза 8**: Публичные подборки, предметы, книжный клуб
16. [ ] **Фаза 9**: Вся админ-панель (дашборд, модерация рецензий/подборок/предметных подборок, мероприятия)
17. [ ] **Фаза 10**: Проверка бекенда (роли, CORS, валидация)

---

## ТАБЛИЦА СООТВЕТСТВИЙ: ФРОНТЕНД-ФАЙЛ → БЕКЕНД-ЭНДПОИНТ

| Фронтенд-файл | API-вызовы | Текущий статус |
|---|---|---|
| `auth/LoginPage.vue` | `POST /api/auth/login` | МОК |
| `auth/RegistrationPage.vue` | `POST /api/auth/register` | МОК |
| `layouts/components/AppHeader.vue` | — (только localStorage) | ГОТОВ |
| `MainPage/MainPage.vue` | `GET /api/books`, `GET /api/collections`, `GET /api/events` | МОК |
| `MainPage/components/CatalogTab.vue` | — (получает через props) | МОК (данные) |
| `MainPage/components/CollectionsTab.vue` | — (получает через props) | МОК (данные) |
| `MainPage/components/SubjectsTab.vue` | `GET /api/subject-collections?subject=&specialty=` | МОК |
| `MainPage/components/ClubTab.vue` | `POST/DELETE /api/events/{id}/register` | МОК |
| `MainPage/components/RecommendationWidget.vue` | `GET /api/recommendations` | МОК |
| `user/BookPage.vue` | `GET /api/books/{id}`, `GET /api/reviews/book/{id}`, `POST /api/reviews`, `POST /api/reviews/{id}/like`, `POST /api/comments/review/{id}`, `POST /api/comments/{id}/like` | МОК |
| `user/components/BookRecommentations.vue` | `GET /api/recommendations/similar/{id}` | ЧАСТИЧНО (неправильный парсинг ответа) |
| `OrdersPage/OrdersPage.vue` | `GET /api/orders`, `DELETE /api/orders/{bookId}` | МОК (localStorage) |
| `ReviewsPage/ReviewsPage.vue` | `GET /api/reviews/my`, `POST /api/reviews`, `PUT /api/reviews/{id}`, `DELETE /api/reviews/{id}`, `GET /api/books` | МОК |
| `CollectionsPage/CollectionPage.vue` | `GET /api/collections/my`, `POST /api/collections`, `PUT /api/collections/{id}`, `DELETE /api/collections/{id}`, `GET /api/books` | МОК |
| `AccountPage/AccountPage.vue` | `GET /api/users/me`, `PUT /api/users/me`, `PUT /api/users/me/password` | МОК |
| `admin/AdminHomePage.vue` | `GET /api/admin/dashboard` | МОК |
| `admin/ModerationReviewsPage.vue` | `GET /api/reviews/pending`, `POST /api/reviews/{id}/approve`, `POST /api/reviews/{id}/reject` | МОК |
| `admin/ModerationCollectionsPage.vue` | `GET /api/collections/pending`, `POST /api/collections/{id}/approve`, `POST /api/collections/{id}/reject`, `POST /api/books/by-ids` | МОК |
| `admin/ModerationSubjectCollectionsPage.vue` | `GET /api/subject-collections/pending`, `POST /api/subject-collections/{id}/approve`, `POST /api/subject-collections/{id}/reject` | МОК |
| `admin/EventsPage.vue` | `GET /api/events`, `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}` | МОК |
| `stores/recommendationStore.js` | `GET /api/recommendations/preferences`, `PUT /api/recommendations/preferences`, `GET /api/recommendations` | НЕ ИСПОЛЬЗУЕТСЯ (URL-ы неправильные) |
| `stores/subjectCollectionsStore.js` | — (чистый мок-стор) | МОК |

---

## КРИТИЧЕСКИЕ РАСХОЖДЕНИЯ (исправить обязательно)

1. **URL рекомендаций**: `recommendationStore.js` вызывает `/api/user/preferences` и `/api/recommendations/personal` — таких эндпоинтов нет. Правильно: `/api/recommendations/preferences` (GET/PUT) и `/api/recommendations` (GET)
2. **Парсинг ответа similar books**: `BookRecommentations.vue` ожидает `data.similar`, бекенд возвращает плоский массив
3. **Поле `login`**: Отсутствует в форме регистрации фронтенда, но обязательно на бекенде
4. **Роль в DTO**: Необходимо подтвердить, что MODERATOR маппится в `"admin"` в AuthResponseDTO
5. **Минимум 200 символов в рецензии**: Бекенд валидирует @Size(min=200) на `text` в CreateReviewDTO. Фронтенд не показывает это ограничение пользователю
6. **Статус рецензий**: Фронтенд BookPage не показывает, что рецензия на модерации (PENDING). Пользователь может подумать, что рецензия потерялась