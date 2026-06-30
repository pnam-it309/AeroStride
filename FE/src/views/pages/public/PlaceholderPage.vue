<script setup>
import { useRoute } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';

const route = useRoute();
const titleMap = {
    '/gioi-thieu': 'GIỚI THIỆU',
    '/tin-tuc': 'TIN TỨC',
    '/lien-he': 'LIÊN HỆ',
    '/he-thong-cua-hang': 'HỆ THỐNG CỬA HÀNG',
    '/tro-giup': 'TRỢ GIÚP'
};
const pageTitle = titleMap[route.path] || 'ĐANG CẬP NHẬT';
</script>

<template>
    <div class="app-container">
        <MainHeader />
        <div class="pt-16 pb-16 min-vh-100 d-flex flex-column align-center justify-center bg-grey-lighten-4">
            <v-icon size="64" color="grey-lighten-1" class="mb-6">mdi-tools</v-icon>
            <h1 class="text-h3 font-weight-black text-grey-darken-3 mb-2">{{ pageTitle }}</h1>
            <p class="text-subtitle-1 text-grey-darken-1 mb-8">Trang này đang trong quá trình xây dựng và hoàn thiện.</p>
            <v-btn color="primary" size="large" rounded="xl" to="/" prepend-icon="mdi-arrow-left">
                Quay lại trang chủ
            </v-btn>
        </div>
        
        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>
    </div>
</template>

<style scoped>
.min-vh-100 {
    min-height: 100vh;
}
.app-container {
    padding-top: 60px; /* Offset for fixed header */
}
</style>
