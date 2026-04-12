import { defineStore } from 'pinia';
import { useRouter } from 'vue-router';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(sessionStorage.getItem('user')) || null,
        accessToken: sessionStorage.getItem('accessToken') || null,
        loading: false,
        error: null
    }),
    
    getters: {
        isLoggedIn: (state) => !!state.accessToken,
        isAdmin: (state) => state.user?.role === 'ADMIN',
        isStaff: (state) => state.user?.role === 'STAFF',
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
                }
                return response;
            } catch (err) {
                this.error = err.message || 'Đăng nhập không thành công';
                throw err;
            } finally {
                this.loading = false;
            }
        },
        
        async logout() {
            await dichVuXacThuc.dangXuat();
            this.user = null;
            this.accessToken = null;
        }
    }
});
