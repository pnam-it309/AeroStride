import { defineStore } from 'pinia';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { APP_ROLES } from '@/constants/appConstants';
import { useUIStore } from '@/stores/ui';
import { notifyFavoritesUpdated } from '@/utils/favoritesUtil';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(sessionStorage.getItem('user')) || null,
        accessToken: sessionStorage.getItem('accessToken') || null,
        loading: false,
        error: null
    }),

    getters: {
        isLoggedIn: (state) => !!state.accessToken,
        isAdmin: (state) => state.user?.role === APP_ROLES.ADMIN,
        isStaff: (state) => state.user?.role === APP_ROLES.STAFF
    },

    actions: {
        async login(loginData) {
            this.loading = true;
            this.error = null;
            try {
                const response = await dichVuXacThuc.dangNhap(loginData);
                if (response.success && response.data) {
                    const { accessToken, refreshToken, username, role } = response.data;
                    this.user = { username, role };
                    this.accessToken = accessToken;

                    // Xóa cache trang cũ và chuyển giỏ hàng/yêu thích sang tài khoản mới
                    const uiStore = useUIStore();
                    uiStore.clearCache();
                    sessionStorage.removeItem('page-cache');
                    sessionStorage.removeItem('page-preferences');

                    const { useCartStore } = await import('@/stores/cartStore');
                    const cartStore = useCartStore();
                    await cartStore.reloadCart();

                    notifyFavoritesUpdated();
                }
                return response;
            } catch (err) {
                this.error = err.message || 'Đăng nhập không thành công';
                throw err;
            } finally {
                this.loading = false;
            }
        },

        async socialLogin(socialData) {
            this.loading = true;
            this.error = null;
            try {
                const response = await dichVuXacThuc.dangNhapSocial(socialData);
                if (response.success && response.data) {
                    const { accessToken, refreshToken, username, role } = response.data;
                    this.user = { username, role };
                    this.accessToken = accessToken;

                    // Xóa cache trang cũ và chuyển giỏ hàng/yêu thích sang tài khoản mới
                    const uiStore = useUIStore();
                    uiStore.clearCache();
                    sessionStorage.removeItem('page-cache');
                    sessionStorage.removeItem('page-preferences');

                    const { useCartStore } = await import('@/stores/cartStore');
                    const cartStore = useCartStore();
                    await cartStore.reloadCart();

                    notifyFavoritesUpdated();
                }
                return response;
            } catch (err) {
                this.error = err.message || 'Đăng nhập mạng xã hội không thành công';
                throw err;
            } finally {
                this.loading = false;
            }
        },

        async logout() {
            await dichVuXacThuc.dangXuat();
            this.user = null;
            this.accessToken = null;

            // Xóa cache dữ liệu trang & preferences khi đăng xuất
            const uiStore = useUIStore();
            uiStore.clearCache();
            sessionStorage.removeItem('page-cache');
            sessionStorage.removeItem('page-preferences');

            // Cập nhật lại giỏ hàng cho tài khoản khách (guest)
            const { useCartStore } = await import('@/stores/cartStore');
            const cartStore = useCartStore();
            await cartStore.reloadCart();

            // Cập nhật lại danh sách yêu thích cho khách
            notifyFavoritesUpdated();
        }
    }
});
