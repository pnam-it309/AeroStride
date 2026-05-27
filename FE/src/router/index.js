import { createRouter, createWebHistory } from 'vue-router';
import MainRoutes from './MainRoutes';
import AuthRoutes from './AuthRoutes';
import { requireAuth, requireGuest } from './guards';
import { PATH } from './routePaths';

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: PATH.LANDING,
            name: 'Landing',
            component: () => import('@/views/pages/landing/LandingPage.vue')
        },
        {
            path: PATH.SHOES,
            name: 'ShoeListing',
            component: () => import('@/views/pages/products/ShoeListing.vue')
        },
        {
            path: PATH.VOUCHERS,
            name: 'VoucherListing',
            component: () => import('@/views/pages/vouchers/VoucherListing.vue')
        },
        {
            path: '/product/:id',
            name: 'ProductDetail',
            component: () => import('@/views/pages/products/ProductDetail.vue')
        },
        {
            path: PATH.CHECKOUT,
            name: 'Checkout',
            component: () => import('@/views/pages/orders/CheckoutPage.vue'),
            meta: { requiresCustomerAuth: true }
        },
        {
            path: `${PATH.ORDER_SUCCESS}/:id`,
            name: 'OrderSuccess',
            component: () => import('@/views/pages/orders/OrderSuccessPage.vue'),
            meta: { requiresCustomerAuth: true }
        },
        {
            path: PATH.ORDERS,
            name: 'MyOrders',
            component: () => import('@/views/pages/orders/MyOrdersPage.vue'),
            meta: { requiresCustomerAuth: true }
        },
        {
            path: `${PATH.ORDER_DETAIL}/:id`,
            name: 'OrderDetail',
            component: () => import('@/views/pages/orders/OrderDetailPage.vue'),
            meta: { requiresCustomerAuth: true }
        },
        // Error Routes
        {
            path: PATH.ERROR_401,
            name: 'Error401',
            component: () => import('@/views/error/Error401.vue')
        },
        {
            path: PATH.ERROR_403,
            name: 'Error403',
            component: () => import('@/views/error/Error403.vue')
        },
        {
            path: PATH.ERROR_404,
            name: 'Error404',
            component: () => import('@/views/error/Error404.vue')
        },
        {
            path: PATH.ERROR_405,
            name: 'Error405',
            component: () => import('@/views/error/Error405.vue')
        },
        {
            path: PATH.ERROR_410,
            name: 'Error410',
            component: () => import('@/views/error/Error410.vue')
        },
        {
            path: PATH.ERROR_429,
            name: 'Error429',
            component: () => import('@/views/error/Error429.vue')
        },
        {
            path: PATH.ERROR_500,
            name: 'Error500',
            component: () => import('@/views/error/Error500.vue')
        },
        {
            path: PATH.ERROR_503,
            name: 'Error503',
            component: () => import('@/views/error/Error503.vue')
        },
        {
            path: PATH.ERROR_OFFLINE,
            name: 'ErrorOffline',
            component: () => import('@/views/error/ErrorOffline.vue')
        },
        {
            ...MainRoutes,
            beforeEnter: requireAuth
        },
        {
            ...AuthRoutes
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'Error404',
            component: () => import('@/views/error/Error404.vue')
        }
    ]
});

// Global guard: Customer auth pages (checkout, orders)
router.beforeEach((to, from, next) => {
    if (to.meta.requiresCustomerAuth) {
        const { dichVuXacThuc } = require('@/services/auth/dichVuXacThuc');
        if (!dichVuXacThuc.daDangNhap()) {
            next(PATH.LOGIN);
        } else {
            next();
        }
    } else {
        next();
    }
});
