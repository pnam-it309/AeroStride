import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuKhachHang = {
    // Lấy tất cả khách hàng
    async layTatCaKhachHang() {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/hien-thi`);
        return response.data?.data || response.data;
    },

    // Lấy khách hàng có phân trang
    async layKhachHangPhanTrang(params) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/phan-trang`, { params });
        return response.data.data;
    },

    // Tìm kiếm khách hàng
    async timKiemKhachHang(params) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/tim-kiem`, { params });
        return response.data.data;
    },

    // Lọc khách hàng
    async locKhachHang(params) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/filter`, { params });
        return response.data.data;
    },

    // Lấy chi tiết khách hàng
    async layChiTietKhachHang(id) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/detail/${id}`);
        return response.data.data;
    },

    // Tạo khách hàng mới
    async taoKhachHang(customerData) {
        const response = await api.post(`${API_ADMIN.KHACH_HANG}/add`, customerData);
        return response.data.data;
    },

    // Cập nhật khách hàng
    async capNhatKhachHang(id, customerData) {
        const response = await api.put(`${API_ADMIN.KHACH_HANG}/update/${id}`, customerData);
        return response.data.data;
    },

    // Đổi trạng thái khách hàng (hỗ trợ nhiều route backend)
    async thayDoiTrangThaiKhachHang(id, trangThai) {
        const payload = { trangThai };
        const attempts = [
            () => api.patch(`${API_ADMIN.KHACH_HANG}/update-trang-thai/${id}`, payload),
            () => api.put(`${API_ADMIN.KHACH_HANG}/update-trang-thai/${id}`, payload),
            () => api.patch(`${API_ADMIN.KHACH_HANG}/trang-thai/${id}`, payload)
        ];

        let lastError;
        for (const request of attempts) {
            try {
                const response = await request();
                return response.data?.data || response.data;
            } catch (error) {
                lastError = error;
            }
        }

        throw lastError;
    },

    // Xóa khách hàng
    async xoaKhachHang(id) {
        const response = await api.delete(`${API_ADMIN.KHACH_HANG}/delete/${id}`);
        return response.data;
    },

    // Xuất Excel
    async xuatExcelKhachHang() {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/export-excel`, { responseType: 'blob' });
        return response.data;
    },

    // --- DIA CHI ---
    async layDanhSachDiaChi(khId) {
        const response = await api.get(`${API_ADMIN.DIA_CHI}/khach-hang/${khId}`);
        return response.data;
    },

    async taoDiaChi(data) {
        const response = await api.post(`${API_ADMIN.DIA_CHI}/add`, data);
        return response.data;
    },

    async capNhatDiaChi(id, data) {
        const response = await api.put(`${API_ADMIN.DIA_CHI}/update/${id}`, data);
        return response.data;
    },

    async xoaDiaChi(id) {
        const response = await api.delete(`${API_ADMIN.DIA_CHI}/delete/${id}`);
        return response.data;
    },

    async datDiaChiMacDinh(id) {
        const response = await api.patch(`${API_ADMIN.DIA_CHI}/set-default/${id}`);
        return response.data;
    }
};
