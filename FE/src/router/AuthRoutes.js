import { requireGuest } from './guards';
import { PATH } from './routePaths';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/authStore';

const AuthRoutes = {
    path: '/admin',
    component: () => import('@/layouts/blank/BlankLayout.vue'),
    meta: {
        requiresAuth: false
    },
    children: [
        // Mặc định khi vào /admin thì chuyển về trang login admin
        {
            path: '',
            redirect: PATH.ADMIN_LOGIN
        },
        // Nếu gõ /admin/user thì chuyển về login client (tùy chọn, để tránh 404)
        {
            path: 'user',
            redirect: PATH.LOGIN
        },
        // Admin Auth
        {
            name: 'Login',
            path: PATH.ADMIN_LOGIN,
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/admin/BoxedLogin.vue')
        },
        {
            name: 'AdminRegister',
            path: PATH.ADMIN_REGISTER,
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/admin/BoxedRegister.vue')
        },
        // Client Auth (Vẫn giữ absolute path để có thể truy cập từ /user/login)
        {
            name: 'ClientLogin',
            path: PATH.LOGIN,
            beforeEnter: requireGuest,
            component: () => import('@/views/authentication/client/ClientLogin.vue')
        },
        {
            name: 'Logout',
            path: PATH.LOGOUT,
            beforeEnter: async (to, from, next) => {
                const uiStore = useUIStore();
                uiStore.showLoading('Hệ thống đang đăng xuất...');
                
                const authStore = useAuthStore();
                
                setTimeout(async () => {
                    await authStore.logout();
                    uiStore.hideLoading();
                    // Quay về trang Landing (/) sau khi logout
                    next('/');
                }, 1500);
            }
        }
    ]
};

export default AuthRoutes;
