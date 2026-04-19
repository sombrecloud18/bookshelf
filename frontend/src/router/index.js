import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '../layouts/AppLayout.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import LoginPage from '../pages/auth/LoginPage.vue';
import RegistrationPage from '../pages/auth/RegistrationPage.vue';
import Page404 from '../pages/Page404/Page404.vue';

import UserMainPage from '../pages/user/MainPage.vue';
import UserAccountPage from '../pages/user/AccountPage.vue';
import UserCollectionsPage from '../pages/user/CollectionsPage.vue';
import UserOrdersPage from '../pages/user/OrdersPage.vue';
import UserReviewsPage from '../pages/user/ReviewsPage.vue';
import BookPage from '../pages/user/BookPage.vue'; // Добавляем импорт

import AdminHomePage from '../pages/admin/AdminHomePage.vue';
import ModerationCollectionsPage from '../pages/admin/ModerationCollectionsPage.vue';
import ModerationReviewsPage from '../pages/admin/ModerationReviewsPage.vue';
import EventsPage from '../pages/admin/EventsPage.vue';
import ModerationSubjectCollectionsPage from '../pages/admin/ModerationSubjectCollectionsPage.vue';

const AUTH_TOKEN_KEY = 'bookshelf_auth_token';
const AUTH_ROLE_KEY = 'bookshelf_auth_role';

function getAuth() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const role = localStorage.getItem(AUTH_ROLE_KEY) || 'user';
  return { token, role };
}

const routes = [
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true, role: 'user' },
    children: [
      {
        path: '',
        name: 'home',
        component: UserMainPage,
      },
      {
        path: 'account',
        name: 'account',
        component: UserAccountPage,
      },
      {
        path: 'collections',
        name: 'collections',
        component: UserCollectionsPage,
      },
      {
        path: 'orders',
        name: 'orders',
        component: UserOrdersPage,
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: UserReviewsPage,
      },
      {
        path: 'book/:id', // Добавляем маршрут для книги
        name: 'book',
        component: BookPage,
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', name: 'admin-home', component: AdminHomePage },
      { path: 'collections', name: 'admin-collections-moderation', component: ModerationCollectionsPage },
      { path: 'reviews', name: 'admin-reviews-moderation', component: ModerationReviewsPage },
      { path: 'events', name: 'admin-events', component: EventsPage },
      { path: 'subject-collections', name: 'admin-subject-collections', component: ModerationSubjectCollectionsPage },
    ],
  },
  {
    path: '/auth',
    name: 'login',
    component: LoginPage,
    meta: { authOnly: true },
  },
  {
    path: '/registration',
    name: 'registration',
    component: RegistrationPage,
    meta: { authOnly: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: Page404,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(to => {
  const { token, role } = getAuth();

  const isAuthRoute = to.meta?.authOnly || to.path === '/auth' || to.path === '/registration';
  const requiresAuth = !!to.meta?.requiresAuth || (to.path !== '/auth' && to.path !== '/registration');

  if (!token && requiresAuth && !isAuthRoute) {
    return { path: '/auth' };
  }

  if (token && isAuthRoute) {
    return { path: role === 'admin' ? '/admin' : '/' };
  }

  const requiredRole = to.meta?.role;
  if (token && requiredRole && requiredRole !== role) {
    return { path: role === 'admin' ? '/admin' : '/' };
  }

  return true;
});

export default router;
