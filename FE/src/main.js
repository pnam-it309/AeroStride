window.global = window;
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import { router } from './router';
import vuetify from './plugins/vuetify';
import '@/scss/style.scss';
import { PerfectScrollbarPlugin } from 'vue3-perfect-scrollbar';
import 'vue3-perfect-scrollbar/style.css';
import { vMaska } from 'maska/vue';
import { createHead } from '@unhead/vue/client';

const app = createApp(App);
const head = createHead();

app.use(head);
app.use(router);
app.use(PerfectScrollbarPlugin);
app.use(createPinia());
app.directive('maska', vMaska);

// Đăng ký AppDatePicker global
import AppDatePicker from '@/components/common/AppDatePicker.vue';
app.component('AppDatePicker', AppDatePicker);

app.use(vuetify).mount('#app');
