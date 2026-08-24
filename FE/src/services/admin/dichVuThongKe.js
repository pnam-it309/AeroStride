import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuThongKe = {
    // Lấy dữ liệu tổng quan thống kê (kèm cả top sản phẩm & danh mục doanh thu)
    async layTongQuan(tuNgay, denNgay) {
        const params = {};
        if (tuNgay) params.tuNgay = tuNgay;
        if (denNgay) params.denNgay = denNgay;

        const response = await api.get(`${API_ADMIN.THONG_KE}/tong-quan`, { params });
        return response.data.data;
    },

    // Lấy doanh thu theo ngày
    async layDoanhThuTheoNgay(tuNgay, denNgay) {
        const params = {};
        if (tuNgay) params.tuNgay = tuNgay;
        if (denNgay) params.denNgay = denNgay;

        const response = await api.get(`${API_ADMIN.THONG_KE}/doanh-thu-theo-ngay`, { params });
        return response.data.data;
    },

    // Lấy thống kê sản phẩm có phân trang, tìm kiếm, sắp xếp
    async layThongKeSanPham(params, options = {}) {
        const response = await api.get(`${API_ADMIN.THONG_KE}/san-pham`, {
            params,
            silent: options.silent !== undefined ? options.silent : true
        });
        return response.data.data;
    },

    // Xuất báo cáo thống kê Excel đa sheet
    async xuatBaoCaoExcel(tuNgay, denNgay) {
        const params = {};
        if (tuNgay) params.tuNgay = tuNgay;
        if (denNgay) params.denNgay = denNgay;

        const response = await api.get(`${API_ADMIN.THONG_KE}/export-excel`, {
            params,
            responseType: 'blob'
        });
        return response.data;
    }
};
