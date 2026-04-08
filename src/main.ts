import { createApp } from "vue";
import { createPinia } from "pinia";
import ui from "@nuxt/ui/vue-plugin";
import App from "./App.vue";
import router from "./router";
import "./style.css";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ui);

// Auth guard: нельзя попасть в приложение без логина/регистрации
const AUTH_TOKEN_KEY = "bookshelf_auth_token";
router.beforeEach((to) => {
  const isAuthed = !!localStorage.getItem(AUTH_TOKEN_KEY);
  const isAuthRoute = to.path === "/auth" || to.path === "/registration";

  if (!isAuthed && !isAuthRoute) {
    return { path: "/auth" };
  }

  if (isAuthed && isAuthRoute) {
    return { path: "/" };
  }

  return true;
});

app.mount("#app");
