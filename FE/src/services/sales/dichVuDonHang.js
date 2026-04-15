import api from '../apiService';

export const dichVuDonHang = {
  // Lấy đơn hàng chờ (hóa đơn chờ)
  async layDonHangCho() {
    const response = await api.get('/admin/ban-hang');
    return response.data.data;
  },

  // Tạo đơn hàng mới
  async taoDonHang() {
    const response = await api.post('/admin/ban-hang');
    return response.data.data;
  },

  // Xóa đơn hàng
  async xoaDonHang(id) {
    const response = await api.delete(`/admin/ban-hang/${id}`);
    return response.data;
  },

  // Thêm sản phẩm vào đơn hàng
  async themSanPhamVaoDonHang(orderId, productData) {
    const response = await api.post(`/admin/ban-hang/${orderId}/add-product`, productData);
    return response.data.data;
  },

  // Cập nhật số lượng sản phẩm trong đơn hàng
  async capNhatSoLuongSanPham(orderId, orderDetailId, quantity) {
    const response = await api.put(`/admin/ban-hang/${orderId}/update-quantity/${orderDetailId}`, { quantity });
    return response.data.data;
  },

  // Xóa sản phẩm khỏi đơn hàng
  async xoaSanPhamKhoiDonHang(orderId, orderDetailId) {
    const response = await api.delete(`/admin/ban-hang/${orderId}/remove-product/${orderDetailId}`);
    return response.data;
  },

  // Lấy chi tiết đơn hàng
  async layChiTietDonHang(id) {
    const response = await api.get(`/admin/ban-hang/${id}`);
    return response.data.data;
  },

  // Thanh toán đơn hàng
  async thanhToanDonHang(orderId, checkoutData) {
    const response = await api.post(`/admin/ban-hang/${orderId}/checkout`, checkoutData);
    return response.data.data;
  },

  // Tìm kiếm sản phẩm cho đơn hàng
  async timKiemSanPhamChoDonHang(params) {
    const response = await api.get('/admin/ban-hang/search-products', { params });
    return response.data.data;
  },

  // Lấy thông tin khách hàng cho đơn hàng
  async layThongTinKhachHang(phoneNumber) {
    const response = await api.get(`/admin/ban-hang/customer/${phoneNumber}`);
    return response.data.data;
  }
};
