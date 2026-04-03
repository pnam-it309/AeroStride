import "./assets/index.css";
import "@phosphor-icons/web/bold"; 
import "@fortawesome/fontawesome-free/css/all.css";
import 'element-plus/dist/index.css' 
import { createApp } from "vue";
import { createPinia } from "pinia";
import { setupValidation } from "@/utils/validation";

import App from "./App.vue";
import router from "./router";

setupValidation(); // Register Vee-Validate rules

const app = createApp(App);

app.use(createPinia());
app.use(router);

app.mount("#app");
