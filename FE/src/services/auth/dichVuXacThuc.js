import api from '../apiService';
import { API_AUTH } from '@/constants/apiPaths';

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

  // Đăng xuất
  async dangXuat() {
    try {
      await api.post(API_AUTH.LOGOUT);
    } catch (error) {
      console.error('Lỗi đăng xuất:', error);
    } finally {
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('refreshToken');
      sessionStorage.removeItem('user');
    }
  },

  // Lấy thông tin user hiện tại
  layUserHienTai() {
    const userStr = sessionStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
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
    return this.coVaiTro('ADMIN');
  },

  // Kiểm tra có phải staff không
  laStaff() {
    return this.coVaiTro('STAFF');
  }
};
