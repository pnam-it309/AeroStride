import api from '../apiService';

export const dichVuXacThuc = {
  // Đăng nhập
  async dangNhap(loginData) {
    const response = await api.post('/auth/login', loginData);
    if (response.data.data) {
      const { accessToken, refreshToken, username, role } = response.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify({ username, role }));
    }
    return response.data;
  },

  // Làm mới token
  async lamMoiToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      throw new Error('Không có refresh token');
    }
    const response = await api.post('/auth/refresh-token', { refreshToken });
    if (response.data.data) {
      const { accessToken, refreshToken: newRefreshToken } = response.data.data;
      localStorage.setItem('accessToken', accessToken);
      if (newRefreshToken) {
        localStorage.setItem('refreshToken', newRefreshToken);
      }
    }
    return response.data;
  },

  // Đăng xuất
  async dangXuat() {
    try {
      await api.post('/auth/logout');
    } catch (error) {
      console.error('Lỗi đăng xuất:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  // Lấy thông tin user hiện tại
  layUserHienTai() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  // Lấy access token
  layAccessToken() {
    return localStorage.getItem('accessToken');
  },

  // Kiểm tra đã đăng nhập chưa
  daDangNhap() {
    return !!localStorage.getItem('accessToken');
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
