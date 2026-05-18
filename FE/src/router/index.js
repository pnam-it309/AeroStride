import { createRouter, createWebHistory } from 'vue-router';
import MainRoutes from './MainRoutes';
import AuthRoutes from './AuthRoutes';
import { requireAuth, requireGuest } from './guards';

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'Landing',
            component: () => import('@/views/pages/LandingPage.vue')
        },
        {
            path: '/shoes',
            name: 'ShoeListing',
            component: () => import('@/views/pages/ShoeListing.vue')
        },
        {
            path: '/product/:id',
            name: 'ProductDetail',
            component: () => import('@/views/pages/ProductDetail.vue')
        },
        {
            path: '/error/403',
            name: 'Error403',
            component: () => import('@/views/error/Error403.vue')
        },
        {
            path: '/error/500',
            name: 'Error500',
            component: () => import('@/views/error/Error500.vue')
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'Error404',
            component: () => import('@/views/error/Error404.vue')
        },
        {
            ...MainRoutes,
            beforeEnter: requireAuth
        },
        {
            ...AuthRoutes
        }
    ]
});
