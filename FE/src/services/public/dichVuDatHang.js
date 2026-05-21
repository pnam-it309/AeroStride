import api from '../apiService';
import { API_CUSTOMER } from '@/constants/apiPaths';

export const dichVuDatHang = {
    // Đặt hàng (checkout)
    async datHang(checkoutData) {
        const response = await api.post(API_CUSTOMER.CHECKOUT, checkoutData, {
            bigOp: true,
            loadingMessage: 'Đang xử lý đơn hàng...'
        });
        return response.data;
    },

    // Lấy danh sách đơn hàng của khách
    async layDonHangCuaToi(trangThai = '') {
        const response = await api.get(API_CUSTOMER.MY_ORDERS, {
            params: { trangThai }
        });
        return response.data.data;
    },

    // Lấy chi tiết đơn hàng
    async layChiTietDonHang(id) {
        const response = await api.get(`${API_CUSTOMER.ORDER}/${id}`);
        return response.data.data;
    },

    // Hủy đơn hàng
    async huyDonHang(id) {
        const response = await api.put(`${API_CUSTOMER.ORDER}/${id}/cancel`, null, {
            bigOp: true,
            loadingMessage: 'Đang hủy đơn hàng...'
        });
        return response.data;
    },

    // Lấy voucher khả dụng
    async layVoucherKhaDung(tongTien = 0) {
        const response = await api.get(API_CUSTOMER.VOUCHERS, {
            params: { tongTien }
        });
        return response.data.data;
    }
};
