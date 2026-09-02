import { createRouter, createWebHistory } from 'vue-router';
import MainRoutes from './MainRoutes';
import AuthRoutes from './AuthRoutes';
import { requireAuth, requireGuest } from './guards';
import { PATH } from './routePaths';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { useUIStore } from '@/stores/ui';
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
            alias: ['/san-pham', '/products'],
            name: 'ShoeListing',
            component: () => import('@/views/pages/products/ShoeListing.vue')
        },
        {
            path: PATH.AI_RECOMMEND,
            name: 'AiRecommendQuiz',
            component: () => import('@/views/pages/products/AiRecommendQuiz.vue'),
            meta: { seoTitle: 'Gợi Ý Chọn Sản Phẩm Phù Hợp Với Bạn' }
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
            alias: ['/order-success/:id', '/order/success/:id'],
            name: 'OrderSuccess',
            component: () => import('@/views/pages/orders/OrderSuccessPage.vue'),
            meta: { seoTitle: 'Đặt Hàng Thành Công' }
        },
        {
            path: PATH.ORDERS,
            name: 'MyOrders',
            component: () => import('@/views/pages/orders/MyOrdersPage.vue'),
            meta: { requiresCustomerAuth: true, seoTitle: 'Đơn Hàng Của Tôi' }
        },
        {
            path: PATH.TRACK_ORDER,
            alias: ['/tra-cuu', '/tracking', '/order-tracking'],
            name: 'OrderTracking',
            component: () => import('@/views/pages/orders/OrderTrackingPage.vue'),
            meta: { seoTitle: 'Tra Cứu Đơn Hàng' }
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
            alias: ['/about', '/ve-chung-toi'],
            name: 'GioiThieu',
            component: () => import('@/views/pages/public/AboutPage.vue'),
            meta: { seoTitle: 'Giới Thiệu' }
        },
        {
            path: '/tin-tuc',
            alias: ['/news'],
            name: 'TinTuc',
            component: () => import('@/views/pages/public/NewsPage.vue'),
            meta: { seoTitle: 'Tin Tức' }
        },
        {
            path: '/tin-tuc/:id',
            alias: ['/news/:id'],
            name: 'NewsDetail',
            component: () => import('@/views/pages/public/NewsDetailPage.vue'),
            meta: { seoTitle: 'Chi Tiết Tin Tức' }
        },
        {
            path: '/lien-he',
            alias: ['/contact', '/ho-tro'],
            name: 'LienHe',
            component: () => import('@/views/pages/public/ContactPage.vue'),
            meta: { seoTitle: 'Liên Hệ' }
        },
        {
            path: '/he-thong-cua-hang',
            alias: ['/stores', '/store-locator', '/cua-hang'],
            name: 'HeThongCuaHang',
            component: () => import('@/views/pages/public/StoreLocatorPage.vue'),
            meta: { seoTitle: 'Hệ Thống Cửa Hàng' }
        },
        {
            path: '/tro-giup',
            alias: ['/help', '/chinh-sach', '/chinh-sach-doi-tra', '/chinh-sach-bao-mat', '/dieu-khoan-dich-vu', '/faq'],
            name: 'TroGiup',
            component: () => import('@/views/pages/public/HelpPage.vue'),
            meta: { seoTitle: 'Trợ Giúp & Chính Sách' }
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

// Dọn dẹp tooltip còn sót lại khi chuyển trang
const cleanupLingeringTooltips = () => {
    try {
        if (typeof document !== 'undefined') {
            if (document.activeElement && typeof document.activeElement.blur === 'function') {
                document.activeElement.blur();
            }
            const tooltips = document.querySelectorAll('.v-overlay-container .v-tooltip, .v-overlay-container .v-overlay');
            tooltips.forEach((el) => {
                if (el.querySelector('.v-tooltip__content') || el.classList.contains('v-tooltip')) {
                    el.remove();
                }
            });
        }
    } catch (e) {}
};

if (typeof window !== 'undefined') {
    window.addEventListener('popstate', cleanupLingeringTooltips);
}

// Global guard: Customer auth pages (checkout, orders)
router.beforeEach((to, from, next) => {
    cleanupLingeringTooltips();
    try {
        const uiStore = useUIStore();
        uiStore.startProgress();
    } catch (e) {}

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

router.afterEach(() => {
    cleanupLingeringTooltips();
    setTimeout(cleanupLingeringTooltips, 60);
    try {
        const uiStore = useUIStore();
        uiStore.stopProgress();
    } catch (e) {}
});

// Auto-recovery for dynamic module load / chunk MIME type errors
router.onError((error, to) => {
    if (
        error.message?.includes('Failed to fetch dynamically imported module') ||
        error.message?.includes('Importing a module script failed') ||
        error.message?.includes('Expected a JavaScript-or-Wasm module script') ||
        error.message?.includes('MIME type')
    ) {
        console.warn('Lỗi nạp module JS, tự động tái nạp trang:', error);
        if (to?.fullPath) {
            window.location.href = to.fullPath;
        } else {
            window.location.reload();
        }
    }
});
