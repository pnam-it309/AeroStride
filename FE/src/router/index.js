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
            path: '/:pathMatch(.*)*',
            component: () => import('@/views/authentication/Error.vue')
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
