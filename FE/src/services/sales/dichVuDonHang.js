import api from '@/services/api';
import { API_ADMIN } from '@/constants/appConstants';

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

    // Xóa hóa đơn
    async xoaDonHang(idHoaDon) {
        const response = await api.delete(`${API_ADMIN.BAN_HANG}/${idHoaDon}`);
        return response.data;
    },

    // Thêm sản phẩm vào hóa đơn
    async addSanPham(idHoaDon, payload) {
        // payload: { idChiTietSanPham, soLuong, giaDuKien }
        const response = await api.post(`${API_ADMIN.BAN_HANG}/${idHoaDon}/chi-tiet`, payload);
        return response.data.data;
    },

    // Cập nhật số lượng sản phẩm trong hóa đơn
    async updateSanPhamQuantity(idHoaDon, idHoaDonChiTiet, payload) {
        // payload: { soLuong }
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/chi-tiet/${idHoaDonChiTiet}`, payload);
        return response.data.data;
    },

    // Xóa sản phẩm khỏi hóa đơn
    async removeSanPham(idHoaDon, idHoaDonChiTiet) {
        const response = await api.delete(`${API_ADMIN.BAN_HANG}/${idHoaDon}/chi-tiet/${idHoaDonChiTiet}`);
        return response.data;
    },

    // Tìm kiếm sản phẩm
    async searchSanPham(params) {
        // params: { keyword, thuongHieu, mucDich, mauSac, kichCo, minGia, maxGia }
        const response = await api.get(`${API_ADMIN.BAN_HANG}/search-san-pham`, { params });
        return response.data.data;
    },

    // Tìm kiếm khách hàng
    async searchKhachHang(keyword) {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/search-khach-hang`, {
            params: { keyword }
        });
        return response.data.data;
    },

    // Lấy danh sách voucher hợp lệ
    async getVouchers(tongTien) {
        const response = await api.get(`${API_ADMIN.BAN_HANG}/vouchers`, {
            params: { tongTien }
        });
        return response.data.data;
    },

    // Áp dụng voucher vào hóa đơn
    async setVoucher(idHoaDon, idPhieuGiamGia) {
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/voucher`, null, {
            params: { idPhieuGiamGia: idPhieuGiamGia || '' }
        });
        return response.data.data;
    },

    // Gán / đổi / gỡ khách hàng khỏi hóa đơn
    async setKhachHang(idHoaDon, idKhachHang) {
        const response = await api.put(`${API_ADMIN.BAN_HANG}/${idHoaDon}/khach-hang`, null, {
            params: { idKhachHang: idKhachHang || '' }
        });
        return response.data.data;
    },

    // Cập nhật phí vận chuyển và loại đơn hàng
    async updateShippingAndChannel(idHoaDon, payload) {
        // payload: { orderType: 'IN_STORE', deliveryMethod, phiVanChuyen }
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
        const response = await api.get(`${API_ADMIN.BAN_HANG}/${idHoaDon}/payment-status`, { silent: true });
        return response.data.data;
    }
};
