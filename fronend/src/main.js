import { createApp } from "vue";
import { createPinia } from "pinia";
import router from "@/router";

import App from "@/App.vue";

import "@/assets/styles/theme.css";
import "@/assets/styles/base.css";
import "@/assets/styles/utilities.css";

createApp(App).use(createPinia()).use(router).mount("#app");
