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
    async layDonHangCuaToi(trangThai = '', keyword = '') {
        const response = await api.get(API_CUSTOMER.MY_ORDERS, {
            params: { trangThai, keyword }
        });
        return response.data.data;
    },

    async layThongKeDonHang() {
        const response = await api.get('/customer/order/stats');
        return response.data.data;
    },

    // Tra cứu đơn hàng công khai (Khách vãng lai)
    async traCuuDonHang(maHoaDon, soDienThoai) {
        const response = await api.get('/customer/order/track', {
            params: { maHoaDon, soDienThoai },
            bigOp: true,
            loadingMessage: 'Đang tra cứu đơn hàng...'
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

    // Khách cập nhật thông tin nhận hàng (sđt, địa chỉ, ghi chú) khi đơn chờ xác nhận
    async capNhatThongTinNhanHang(id, payload) {
        const response = await api.put(`${API_CUSTOMER.ORDER}/${id}/shipping`, payload, {
            bigOp: true,
            loadingMessage: 'Đang cập nhật thông tin nhận hàng...'
        });
        return response.data;
    },

    // Khách cập nhật số lượng sản phẩm (chỉ đơn tiền mặt, đang chờ xác nhận)
    async capNhatSanPham(id, items) {
        const response = await api.put(`${API_CUSTOMER.ORDER}/${id}/items`, { items }, {
            bigOp: true,
            loadingMessage: 'Đang cập nhật sản phẩm...'
        });
        return response.data;
    },

    // Xác nhận kết quả VNPay với backend (verify chữ ký + lưu trạng thái đã thanh toán)
    async xacNhanThanhToanVnPay(params) {
        const response = await api.get('/payment/vnpay-callback', { params });
        return response.data;
    },

    // Xác nhận thanh toán QR online
    async xacNhanDonHangQr(orderId) {
        const response = await api.post(`/payment/confirm-order/${orderId}`);
        return response.data;
    },

    // Tạo lại URL thanh toán VNPay cho đơn chuyển khoản chưa thanh toán
    async taoUrlThanhToanLai(id, returnUrl) {
        const response = await api.post(`${API_CUSTOMER.ORDER}/${id}/vnpay-url`, null, {
            params: { returnUrl }
        });
        return response.data.data;
    },

    // Lấy voucher khả dụng
    async layVoucherKhaDung(tongTien = 0) {
        const response = await api.get(API_CUSTOMER.VOUCHERS, {
            params: { tongTien }
        });
        return response.data.data;
    }
};
