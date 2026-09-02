import api from '../apiService';
import { API_AUTH } from '@/constants/apiPaths';
import { APP_ROLES } from '@/constants/appConstants';

export const dichVuXacThuc = {
    // Đăng nhập
    async dangNhap(loginData) {
        const response = await api.post(API_AUTH.LOGIN, loginData, {
            bigOp: true,
            loadingMessage: 'Đang xác thực bảo mật...'
        });
        if (response.data.data) {
            const { accessToken, refreshToken, username, role } = response.data.data;
            sessionStorage.setItem('accessToken', accessToken);
            sessionStorage.setItem('refreshToken', refreshToken);
            sessionStorage.setItem('user', JSON.stringify({ username, role }));
        }
        return response.data;
    },

    // Đăng nhập qua mạng xã hội (Google, Facebook)
    async dangNhapSocial(socialData) {
        const response = await api.post(API_AUTH.SOCIAL_LOGIN, socialData, {
            bigOp: true,
            loadingMessage: `Đang đăng nhập bằng ${socialData.provider || 'mạng xã hội'}...`
        });
        if (response.data.data) {
            const { accessToken, refreshToken, username, role } = response.data.data;
            sessionStorage.setItem('accessToken', accessToken);
            sessionStorage.setItem('refreshToken', refreshToken);
            sessionStorage.setItem('user', JSON.stringify({ username, role }));
        }
        return response.data;
    },

    // Làm mới token
    async lamMoiToken() {
        const refreshToken = sessionStorage.getItem('refreshToken');
        if (!refreshToken) {
            throw new Error('Không có refresh token');
        }
        const response = await api.post(API_AUTH.REFRESH, { refreshToken });
        if (response.data.data) {
            const { accessToken, refreshToken: newRefreshToken } = response.data.data;
            sessionStorage.setItem('accessToken', accessToken);
            if (newRefreshToken) {
                sessionStorage.setItem('refreshToken', newRefreshToken);
            }
        }
        return response.data;
    },

    // Bộ nhớ đệm profile nhân viên (tránh gọi /me lặp lại nhiều lần)
    _cachedProfile: (() => {
        try {
            const cached = sessionStorage.getItem('userProfile');
            return cached ? JSON.parse(cached) : null;
        } catch (e) {
            return null;
        }
    })(),
    _pendingProfilePromise: null,

    // Đăng xuất
    async dangXuat() {
        this._cachedProfile = null;
        this._pendingProfilePromise = null;
        try {
            await api.post(API_AUTH.LOGOUT);
        } catch (error) {
            console.error('Lỗi đăng xuất:', error);
        } finally {
            sessionStorage.removeItem('accessToken');
            sessionStorage.removeItem('refreshToken');
            sessionStorage.removeItem('user');
            sessionStorage.removeItem('userProfile');
        }
    },

    // Lấy thông tin user hiện tại
    layUserHienTai() {
        const userStr = sessionStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    },

    // Lấy hồ sơ nhân viên đang đăng nhập (tự động cache để tránh gọi lặp lại)
    async layThongTinCaNhan(forceRefresh = false) {
        if (!forceRefresh) {
            if (this._cachedProfile) {
                return this._cachedProfile;
            }
            try {
                const stored = sessionStorage.getItem('userProfile');
                if (stored) {
                    this._cachedProfile = JSON.parse(stored);
                    return this._cachedProfile;
                }
            } catch (e) {}
            if (this._pendingProfilePromise) {
                return this._pendingProfilePromise;
            }
        }

        if (!this.layAccessToken()) {
            return null;
        }

        this._pendingProfilePromise = (async () => {
            try {
                const response = await api.get(API_AUTH.ME);
                if (response.data?.data) {
                    this._cachedProfile = response.data.data;
                    sessionStorage.setItem('userProfile', JSON.stringify(response.data.data));
                }
                return response.data?.data;
            } finally {
                this._pendingProfilePromise = null;
            }
        })();

        return this._pendingProfilePromise;
    },

    // Xóa cache profile thủ công
    xoaCacheProfile() {
        this._cachedProfile = null;
        this._pendingProfilePromise = null;
        sessionStorage.removeItem('userProfile');
    },

    // Cập nhật thông tin cá nhân nhân viên đang đăng nhập
    async capNhatThongTin(payload) {
        this.xoaCacheProfile();
        const response = await api.put(`${API_AUTH.BASE}/profile`, payload);
        if (response.data?.data) {
            this._cachedProfile = response.data.data;
            sessionStorage.setItem('userProfile', JSON.stringify(response.data.data));
        }
        return response.data;
    },

    // Đổi mật khẩu nhân viên đang đăng nhập
    async doiMatKhau(payload) {
        const response = await api.put(API_AUTH.CHANGE_PASSWORD, payload);
        return response.data;
    },

    // Lấy access token
    layAccessToken() {
        return sessionStorage.getItem('accessToken');
    },

    // Kiểm tra đã đăng nhập chưa
    daDangNhap() {
        return !!sessionStorage.getItem('accessToken');
    },

    // Kiểm tra vai trò
    coVaiTro(role) {
        const user = this.layUserHienTai();
        return user && user.role === role;
    },

    // Kiểm tra có phải admin không
    laAdmin() {
        return this.coVaiTro(APP_ROLES.ADMIN);
    },

    // Kiểm tra có phải staff không
    laStaff() {
        return this.coVaiTro(APP_ROLES.STAFF);
    }
};
