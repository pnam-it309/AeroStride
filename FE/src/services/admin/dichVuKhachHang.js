import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

const customerDetailCache = new Map();
const customerAddressCache = new Map();
const inFlightRequests = new Map();

export const dichVuKhachHang = {
    // Lấy tất cả khách hàng
    async layTatCaKhachHang() {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/hien-thi`, { params: { size: 1000 } });
        return response.data?.data || response.data;
    },

    // Lấy khách hàng có phân trang
    async layKhachHangPhanTrang(params) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/phan-trang`, { params });
        return response.data;
    },

    // Tìm kiếm khách hàng
    async timKiemKhachHang(params) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/tim-kiem`, { params });
        return response.data;
    },

    // Lọc khách hàng
    async locKhachHang(params) {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/filter`, { params });
        return response.data;
    },

    // Lấy chi tiết khách hàng
    async layChiTietKhachHang(id, force = false) {
        if (!id) return null;
        const cacheKey = `detail_${id}`;
        if (!force && customerDetailCache.has(id)) {
            return customerDetailCache.get(id);
        }
        if (inFlightRequests.has(cacheKey)) {
            return inFlightRequests.get(cacheKey);
        }

        const promise = api.get(`${API_ADMIN.KHACH_HANG}/detail/${id}`)
            .then((res) => {
                const data = res.data?.data || res.data;
                customerDetailCache.set(id, data);
                return data;
            })
            .finally(() => {
                inFlightRequests.delete(cacheKey);
            });

        inFlightRequests.set(cacheKey, promise);
        return promise;
    },

    // Tạo khách hàng mới
    async taoKhachHang(customerData) {
        const response = await api.post(`${API_ADMIN.KHACH_HANG}/add`, customerData);
        return response.data.data;
    },

    // Cập nhật khách hàng
    async capNhatKhachHang(id, customerData) {
        customerDetailCache.delete(id);
        const response = await api.put(`${API_ADMIN.KHACH_HANG}/update/${id}`, customerData);
        return response.data.data;
    },

    // Đổi trạng thái khách hàng
    async thayDoiTrangThaiKhachHang(id, trangThai) {
        customerDetailCache.delete(id);
        const response = await api.put(`${API_ADMIN.KHACH_HANG}/status/${id}`, { status: trangThai });
        return response.data?.data || response.data;
    },

    // Xóa khách hàng
    async xoaKhachHang(id) {
        customerDetailCache.delete(id);
        customerAddressCache.delete(id);
        const response = await api.delete(`${API_ADMIN.KHACH_HANG}/delete/${id}`);
        return response.data;
    },

    // Xuất Excel
    async xuatExcelKhachHang() {
        const response = await api.get(`${API_ADMIN.KHACH_HANG}/export-excel`, { responseType: 'blob' });
        return response.data;
    },

    // --- DIA CHI ---
    async layDanhSachDiaChi(khId, force = false) {
        if (!khId) return [];
        const cacheKey = `addr_${khId}`;
        if (!force && customerAddressCache.has(khId)) {
            return customerAddressCache.get(khId);
        }
        if (inFlightRequests.has(cacheKey)) {
            return inFlightRequests.get(cacheKey);
        }

        const promise = api.get(`${API_ADMIN.DIA_CHI}/khach-hang/${khId}`)
            .then((res) => {
                const data = res.data;
                customerAddressCache.set(khId, data);
                return data;
            })
            .finally(() => {
                inFlightRequests.delete(cacheKey);
            });

        inFlightRequests.set(cacheKey, promise);
        return promise;
    },

    async taoDiaChi(data) {
        if (data?.khachHangId || data?.idKhachHang) {
            customerAddressCache.delete(data.khachHangId || data.idKhachHang);
        } else {
            customerAddressCache.clear();
        }
        const response = await api.post(`${API_ADMIN.DIA_CHI}/add`, data);
        return response.data;
    },

    async capNhatDiaChi(id, data) {
        customerAddressCache.clear();
        const response = await api.put(`${API_ADMIN.DIA_CHI}/update/${id}`, data);
        return response.data;
    },

    async xoaDiaChi(id) {
        customerAddressCache.clear();
        const response = await api.delete(`${API_ADMIN.DIA_CHI}/delete/${id}`);
        return response.data;
    },

    async datDiaChiMacDinh(id) {
        customerAddressCache.clear();
        const response = await api.patch(`${API_ADMIN.DIA_CHI}/set-default/${id}`);
        return response.data;
    }
};
