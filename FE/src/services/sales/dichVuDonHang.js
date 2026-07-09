import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuDonHang = {
    // Lấy danh sách hóa đơn chờ
    async layDonHangCho() {
        const response = await api.get(API_ADMIN.BAN_HANG);
        return response.data.data;
    },

    // Tạo hóa đơn mới
    async taoDonHang() {
        const response = await api.post(API_ADMIN.BAN_HANG);
        return response.data.data;
    },

    // Xóa hóa đơn chờ
    async xoaDonHang(id) {
        const response = await api.delete(`${API_ADMIN.BAN_HANG}/${id}`);
        return response.data;
    },

    // Tìm kiếm sản phẩm
    async searchSanPham(params = {}) {
        // params: { keyword, thuongHieu, chatLieu, xuatXu, mucDich }
        // Clean params (remove 'ALL' or empty strings)
        const cleanParams = Object.keys(params).reduce((acc, key) => {
            if (params[key] && params[key] !== 'ALL') {
                acc[key] = params[key];
            }
            return acc;
        }, {});
        const response = await api.get(`${API_ADMIN.BAN_HANG}/search-san-pham`, { params: cleanParams });
        return response.data.data;
    },

    // Thêm sản phẩm vào hóa đơn
    async addSanPham(idHoaDon, payload) {
        // payload: { idChiTietSanPham, soLuong }
        const response = await api.post(`${API_ADMIN.BAN_HANG}/${idHoaDon}/add-product`, payload);
        return response.data.data;
    },

    // Cập nhật số lượng
    async updateSoLuong(idHoaDon, idHdct, soLuong) {
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/update-quantity/${idHdct}`, null, {
            params: { soLuong }
        });
        return response.data.data;
    },

    // Xóa sản phẩm khỏi hóa đơn
    async removeSanPham(idHoaDon, idHdct) {
        const response = await api.delete(`${API_ADMIN.BAN_HANG}/${idHoaDon}/remove-product/${idHdct}`);
        return response.data;
    },

    // Tìm kiếm khách hàng
    async searchKhachHang(keyword = '') {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/search-khach-hang`, { params: { keyword } });
        return response.data.data;
    },

    // Set khách hàng cho hóa đơn
    async setKhachHang(idHoaDon, idKhachHang = null) {
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/khach-hang`, null, {
            params: { idKhachHang }
        });
        return response.data.data;
    },

    // Lấy danh sách voucher phù hợp
    async getVouchers(tongTien) {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/vouchers`, { params: { tongTien } });
        return response.data.data;
    },

    // Set voucher cho hóa đơn
    async setVoucher(idHoaDon, idVoucher = null) {
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/voucher`, null, {
            params: { idVoucher }
        });
        return response.data.data;
    },

    // Cập nhật phí vận chuyển và loại đơn hàng
    async updateShippingAndChannel(idHoaDon, payload) {
        // payload: { loaiDon, phiVanChuyen }
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/shipping`, payload);
        return response.data.data;
    },

    // Checkout (Thanh toán)
    async checkout(idHoaDon, checkoutData) {
        const response = await api.post(`${API_ADMIN.BAN_HANG}/${idHoaDon}/checkout`, checkoutData);
        return response.data;
    },

    // Lấy voucher tốt nhất
    async getBestVoucher(idHoaDon) {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/${idHoaDon}/best-voucher`);
        return response.data.data;
    },

    // Lấy gợi ý sản phẩm để mua thêm
    async getProductSuggestions(idHoaDon) {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/${idHoaDon}/product-suggestions`);
        return response.data.data;
    }
};
