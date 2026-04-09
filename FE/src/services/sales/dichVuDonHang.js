import api from '../apiService';

export const dichVuDonHang = {
  // Lấy danh sách hóa đơn chờ
  async layDonHangCho() {
    const response = await api.get('/admin/ban-hang');
    return response.data.data;
  },

  // Tạo hóa đơn mới
  async taoDonHang() {
    const response = await api.post('/admin/ban-hang');
    return response.data.data;
  },

  // Xóa hóa đơn chờ
  async xoaDonHang(id) {
    const response = await api.delete(`/admin/ban-hang/${id}`);
    return response.data;
  },

  // Tìm kiếm sản phẩm
  async searchSanPham(keyword = '') {
    const response = await api.get('/admin/ban-hang/search-san-pham', { params: { keyword } });
    return response.data.data;
  },

  // Thêm sản phẩm vào hóa đơn
  async addSanPham(idHoaDon, payload) {
    // payload: { idChiTietSanPham, soLuong }
    const response = await api.post(`/admin/ban-hang/${idHoaDon}/add-product`, payload);
    return response.data.data;
  },

  // Cập nhật số lượng
  async updateSoLuong(idHoaDon, idHdct, soLuong) {
    const response = await api.put(`/admin/ban-hang/${idHoaDon}/update-quantity/${idHdct}`, null, {
      params: { soLuong }
    });
    return response.data.data;
  },

  // Xóa sản phẩm khỏi hóa đơn
  async removeSanPham(idHoaDon, idHdct) {
    const response = await api.delete(`/admin/ban-hang/${idHoaDon}/remove-product/${idHdct}`);
    return response.data;
  },

  // Tìm kiếm khách hàng
  async searchKhachHang(keyword = '') {
    const response = await api.get('/admin/ban-hang/search-khach-hang', { params: { keyword } });
    return response.data.data;
  },

  // Set khách hàng cho hóa đơn
  async setKhachHang(idHoaDon, idKhachHang = null) {
    const response = await api.put(`/admin/ban-hang/${idHoaDon}/khach-hang`, null, {
      params: { idKhachHang }
    });
    return response.data.data;
  },

  // Lấy danh sách voucher phù hợp
  async getVouchers(tongTien) {
    const response = await api.get('/admin/ban-hang/vouchers', { params: { tongTien } });
    return response.data.data;
  },

  // Set voucher cho hóa đơn
  async setVoucher(idHoaDon, idVoucher = null) {
    const response = await api.put(`/admin/ban-hang/${idHoaDon}/voucher`, null, {
      params: { idVoucher }
    });
    return response.data.data;
  },

  // Checkout (Thanh toán)
  async checkout(idHoaDon, checkoutData) {
    // checkoutData: { loaiHoaDon, idKhachHang, idPhieuGiamGia, tongTien, soTienGiam, tongTienSauGiam, hinhThucThanhToan, ghiChu }
    const response = await api.post(`/admin/ban-hang/${idHoaDon}/checkout`, checkoutData);
    return response.data;
  }
};
