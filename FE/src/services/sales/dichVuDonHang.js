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
        // Cho phép truyền trực tiếp chuỗi mã quét QR/barcode hoặc object bộ lọc.
        // Nếu không chuẩn hóa ở đây, Object.keys("SP001") sẽ biến thành key 0,1,2... và API không nhận được keyword.
        const normalizedParams = typeof params === 'string' ? { keyword: params } : (params || {});

        // params: { keyword, thuongHieu, chatLieu, xuatXu, mucDich }
        // Clean params (remove 'ALL' or empty strings)
        const cleanParams = Object.keys(normalizedParams).reduce((acc, key) => {
            if (normalizedParams[key] && normalizedParams[key] !== 'ALL') {
                acc[key] = normalizedParams[key];
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
    },

    // Kiểm tra trạng thái thanh toán
    async checkPaymentStatus(idHoaDon) {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/${idHoaDon}/payment-status`);
        return response.data.data;
    }
};
