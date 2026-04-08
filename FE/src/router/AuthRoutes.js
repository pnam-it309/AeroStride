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
            name: 'Register',
            path: '/auth/register',
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/BoxedRegister.vue')
        },
        {
            name: 'Logout',
            path: '/auth/logout',
            beforeEnter: async (to, from, next) => {
                const { useLoaderStore } = await import('@/stores/loader');
                const loaderStore = useLoaderStore();
                loaderStore.showOverlay('Hệ thống đang đăng xuất...');
                
                const { dichVuXacThuc } = await import('@/services/auth/dichVuXacThuc');
                
                // Add a small delay for the 'Restart' feel
                setTimeout(async () => {
                    await dichVuXacThuc.dangXuat();
                    loaderStore.hideOverlay();
                    next('/auth/login');
                }, 1500);
            }
        }
    ]
};

export default AuthRoutes;
