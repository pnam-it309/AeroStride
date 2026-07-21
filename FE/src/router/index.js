import { createRouter, createWebHistory } from 'vue-router';
import MainRoutes from './MainRoutes';
import AuthRoutes from './AuthRoutes';
import { requireAuth, requireGuest } from './guards';
import { PATH } from './routePaths';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
export const router = createRouter({
    history: createWebHistory(),
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition;
        } else {
            return { top: 0 };
        }
    },
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
            meta: { seoTitle: 'Thanh Toán' }
        },
        {
            path: `${PATH.ORDER_SUCCESS}/:id`,
            name: 'OrderSuccess',
            component: () => import('@/views/pages/orders/OrderSuccessPage.vue'),
            meta: { seoTitle: 'Đặt Hàng Thành Công' }
        },
        {
            path: PATH.ORDERS,
            name: 'MyOrders',
            component: () => import('@/views/pages/orders/MyOrdersPage.vue'),
            meta: { seoTitle: 'Đơn Hàng Của Tôi' }
        },
        {
            path: PATH.PROFILE,
            name: 'CustomerProfile',
            component: () => import('@/views/pages/customer/CustomerProfilePage.vue'),
            meta: { requiresCustomerAuth: true, seoTitle: 'Hồ Sơ Của Tôi' }
        },
        {
            path: `${PATH.ORDER_DETAIL}/:id`,
            name: 'OrderDetail',
            component: () => import('@/views/pages/orders/OrderDetailPage.vue'),
            meta: { seoTitle: 'Chi Tiết Đơn Hàng' }
        },
        {
            path: PATH.FAVORITES,
            name: 'FavoritesPage',
            component: () => import('@/views/pages/customer/FavoritesPage.vue'),
            meta: { seoTitle: 'Danh Sách Yêu Thích' }
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
            path: '/gioi-thieu',
            name: 'GioiThieu',
            component: () => import('@/views/pages/public/AboutPage.vue'),
            meta: { seoTitle: 'Giới Thiệu' }
        },
        {
            path: '/tin-tuc',
            name: 'TinTuc',
            component: () => import('@/views/pages/public/NewsPage.vue'),
            meta: { seoTitle: 'Tin Tức' }
        },
        {
            path: '/lien-he',
            name: 'LienHe',
            component: () => import('@/views/pages/public/ContactPage.vue'),
            meta: { seoTitle: 'Liên Hệ' }
        },
        {
            path: '/he-thong-cua-hang',
            name: 'HeThongCuaHang',
            component: () => import('@/views/pages/public/StoreLocatorPage.vue'),
            meta: { seoTitle: 'Hệ Thống Cửa Hàng' }
        },
        {
            path: '/tro-giup',
            name: 'TroGiup',
            component: () => import('@/views/pages/public/HelpPage.vue'),
            meta: { seoTitle: 'Trợ Giúp' }
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
    // SEO: Update document title based on route meta
    if (to.meta.seoTitle) {
        document.title = `${to.meta.seoTitle} | AeroStride`;
    }

    if (to.meta.requiresCustomerAuth) {
        if (!dichVuXacThuc.daDangNhap()) {
            next(PATH.LOGIN);
        } else {
            next();
        }
    } else {
        next();
    }
});
