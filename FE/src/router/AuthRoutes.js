import { requireGuest } from './guards';
import { PATH } from './routePaths';

const AuthRoutes = {
    path: '/auth',
    component: () => import('@/layouts/blank/BlankLayout.vue'),
    meta: {
        requiresAuth: false
    },
    children: [

        {
            name: 'Login',
            path: PATH.LOGIN,
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/BoxedLogin.vue')
        },
        {
            name: 'ForgotPassword',
            path: PATH.FORGOT_PASSWORD,
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/ForgotPassword.vue')
        },
        {
            name: 'Register',
            path: PATH.REGISTER,
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/BoxedRegister.vue')
        },
        {
            name: 'Logout',
            path: PATH.LOGOUT,
            beforeEnter: async (to, from, next) => {
                const { useUIStore } = await import('@/stores/ui');
                const uiStore = useUIStore();
                uiStore.showLoading('Hệ thống đang đăng xuất...');
                
                const { dichVuXacThuc } = await import('@/services/auth/dichVuXacThuc');
                
                // Add a small delay for the 'Restart' feel
                setTimeout(async () => {
                    await dichVuXacThuc.dangXuat();
                    uiStore.hideLoading();
                    next(PATH.LOGIN);
                }, 1500);
            }
        }
    ]
};

export default AuthRoutes;
