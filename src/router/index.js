import { createRouter, createWebHistory } from 'vue-router';
import AccountPage from '../pages/AccountPage/AccountPage.vue';
import CollectionPage from '../pages/CollectionsPage/CollectionPage.vue';
import OrdersPage from '../pages/OrdersPage/OrdersPage.vue';
import ReviewsPage from '../pages/ReviewsPage/ReviewsPage.vue';
import MainPage from '../pages/MainPage/MainPage.vue';
import AppLayout from '../layouts/AppLayout.vue';
import LoginPage from '../pages/auth/LoginPage.vue';
import RegistrationPage from '../pages/auth/RegistrationPage.vue';
import Page404 from '../pages/Page404/Page404.vue';

const routes = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: MainPage,
      },
      {
        path: 'account',
        name: 'account',
        component: AccountPage,
      },
      {
        path: 'collections',
        name: 'collections',
        component: CollectionPage,
      },
      {
        path: 'orders',
        name: 'orders',
        component: OrdersPage,
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: ReviewsPage,
      },
    ],
  },
  {
    path: '/auth',
    name: 'login',
    component: LoginPage,
  },
  {
    path: '/registration',
    name: 'registration',
    component: RegistrationPage,
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

export default router;
