import apiService from '../apiService';

export const dichVuKhachHang = {
    // Lấy thông tin cá nhân của khách hàng đang đăng nhập
    layThongTinCaNhan: async () => {
        try {
            const response = await apiService.get('/customer/profile/me');
            return response.data;
        } catch (error) {
            throw error;
        }
    },
    
    // Cập nhật thông tin cá nhân
    capNhatHoSo: async (data) => {
        try {
            const response = await apiService.put('/customer/profile/update', data);
            return response.data;
        } catch (error) {
            throw error;
        }
    },
    
    // Đổi mật khẩu
    doiMatKhau: async (data) => {
        try {
            const response = await apiService.put('/customer/profile/change-password', data);
            return response.data;
        } catch (error) {
            throw error;
        }
    }
};
