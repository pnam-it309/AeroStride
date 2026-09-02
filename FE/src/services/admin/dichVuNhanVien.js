import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

let cachedStaffOptions = null;
let inFlightStaffOptions = null;

export const dichVuNhanVien = {
    // Lấy danh sách nhân viên tinh gọn cho select dropdown (có cache in-memory)
    async layOptionsNhanVien(forceRefresh = false) {
        if (!forceRefresh && cachedStaffOptions) {
            return cachedStaffOptions;
        }
        if (inFlightStaffOptions) {
            return inFlightStaffOptions;
        }
        inFlightStaffOptions = (async () => {
            try {
                const response = await api.get(`${API_ADMIN.NHAN_VIEN}/options`);
                const data = response.data?.data || response.data || [];
                cachedStaffOptions = Array.isArray(data) ? data : (data.content || []);
                return cachedStaffOptions;
            } catch (e) {
                // Fallback nếu endpoint options chưa có
                const fallbackRes = await api.get(`${API_ADMIN.NHAN_VIEN}/hien-thi`);
                const fbData = fallbackRes.data?.data || fallbackRes.data || [];
                cachedStaffOptions = Array.isArray(fbData) ? fbData : (fbData.content || []);
                return cachedStaffOptions;
            } finally {
                inFlightStaffOptions = null;
            }
        })();
        return inFlightStaffOptions;
    },

    // Lấy tất cả nhân viên
    async layTatCaNhanVien() {
        const response = await api.get(`${API_ADMIN.NHAN_VIEN}/hien-thi`);
        return response.data;
    },

    // Lấy nhân viên có phân trang
    async layNhanVienPhanTrang(params) {
        const response = await api.get(`${API_ADMIN.NHAN_VIEN}/phan-trang`, { params });
        return response.data;
    },

    // Tìm kiếm nhân viên
    async timKiemNhanVien(params) {
        const response = await api.get(`${API_ADMIN.NHAN_VIEN}/tim-kiem`, { params });
        return response.data;
    },

    // Lấy chi tiết nhân viên
    async layChiTietNhanVien(id) {
        const response = await api.get(`${API_ADMIN.NHAN_VIEN}/detail/${id}`);
        return response.data.data;
    },

    // Tạo nhân viên mới
    async taoNhanVien(employeeData) {
        cachedStaffOptions = null;
        try {
            const response = await api.post(`${API_ADMIN.NHAN_VIEN}/add`, employeeData);
            return response.data.data;
        } catch (error) {
            if (error.response?.status === 404) {
                const response = await api.post(API_ADMIN.NHAN_VIEN, employeeData);
                return response.data.data;
            }
            throw error;
        }
    },

    // Cập nhật nhân viên
    async capNhatNhanVien(id, employeeData) {
        cachedStaffOptions = null;
        try {
            // Thử đường dẫn hiện tại
            const response = await api.put(`${API_ADMIN.NHAN_VIEN}/update/${id}`, employeeData);
            return response.data.data;
        } catch (error) {
            if (error.response?.status === 404) {
                // Nếu 404, thử đường dẫn chuẩn RESTful (không có /update)
                const response = await api.put(`${API_ADMIN.NHAN_VIEN}/${id}`, employeeData);
                return response.data.data;
            }
            throw error;
        }
    },

    // Xóa nhân viên
    async xoaNhanVien(id) {
        cachedStaffOptions = null;
        const response = await api.delete(`${API_ADMIN.NHAN_VIEN}/delete/${id}`);
        return response.data;
    },

    // Thay đổi trạng thái nhân viên
    async thayDoiTrangThaiNhanVien(id, status) {
        cachedStaffOptions = null;
        const response = await api.patch(`${API_ADMIN.NHAN_VIEN}/${id}/status`, { status });
        return response.data.data;
    },

    // Xuất Excel
    async xuatExcelNhanVien() {
        const response = await api.get(`${API_ADMIN.NHAN_VIEN}/export-excel`, { responseType: 'blob' });
        return response.data;
    },

    // Lấy danh sách phân quyền
    async layDanhSachPhanQuyen() {
        const response = await api.get(`${API_ADMIN.NHAN_VIEN}/phan-quyen`);
        return response.data.data;
    },

    // Lấy thông tin cá nhân (dùng chung cache từ dichVuXacThuc)
    async layThongTinCaNhan(forceRefresh = false) {
        return dichVuXacThuc.layThongTinCaNhan(forceRefresh);
    }
};
