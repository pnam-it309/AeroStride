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
import { initRoutePreloader } from '@/utils/routePreloader';
app.component('AppDatePicker', AppDatePicker);

app.use(vuetify).mount('#app');

// Tự động tải lại trang khi có phiên bản mới làm thay đổi hash chunk JS
window.addEventListener('vite:preloadError', (event) => {
    event.preventDefault();
    window.location.reload();
});

window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason?.message || String(event.reason || '');
    if (
        reason.includes('Failed to fetch dynamically imported module') ||
        reason.includes('Importing a module script failed') ||
        reason.includes('dynamically imported module')
    ) {
        event.preventDefault();
        console.warn('Phát hiện bản cập nhật ứng dụng mới, tự động tải lại...', event.reason);
        window.location.reload();
    }
});

// Kích hoạt preload các route và component khi trình duyệt rảnh
initRoutePreloader();
