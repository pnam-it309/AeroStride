import { requireGuest } from './guards';

const AuthRoutes = {
    path: '/auth',
    component: () => import('@/layouts/blank/BlankLayout.vue'),
    meta: {
        requiresAuth: false
    },
    children: [

        {
            name: 'Login',
            path: '/auth/login',
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/BoxedLogin.vue')
        },
        {
            name: 'ForgotPassword',
            path: '/auth/forgot-password',
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/ForgotPassword.vue')
        },
        {
            name: 'Register',
            path: '/auth/register',
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/BoxedRegister.vue')
        },
        {
            name: 'Logout',
            path: '/auth/logout',
            beforeEnter: async (to, from, next) => {
                const { useUIStore } = await import('@/stores/ui');
                const uiStore = useUIStore();
                uiStore.showLoading('Hệ thống đang đăng xuất...');
                
                const { dichVuXacThuc } = await import('@/services/auth/dichVuXacThuc');
                
                // Add a small delay for the 'Restart' feel
                setTimeout(async () => {
                    await dichVuXacThuc.dangXuat();
                    uiStore.hideLoading();
                    next('/auth/login');
                }, 1500);
            }
        }
    ]
};

export default AuthRoutes;
