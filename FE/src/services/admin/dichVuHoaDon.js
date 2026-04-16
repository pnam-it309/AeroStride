import api from '../apiService';

export const dichVuHoaDon = {
  // Lấy hóa đơn có phân trang
  async layHoaDonPhanTrang(params) {
    const response = await api.get('/admin/hoa-don', { params });
    return response.data.data;
  },

  // Lấy chi tiết hóa đơn
  async layChiTietHoaDon(id) {
    const response = await api.get(`/admin/hoa-don/${id}`);
    return response.data.data;
  },

  // Cập nhật trạng thái
  async capNhatTrangThaiHoaDon(id, status) {
    const response = await api.put(`/admin/hoa-don/${id}/status`, null, { params: { status } });
    return response.data.data;
  },

  // Cập nhật thông tin giao hàng/khách hàng
  async capNhatThongTinHoaDon(id, data) {
    const response = await api.put(`/admin/hoa-don/${id}/info`, data);
    return response.data.data;
  },

  // Thêm/Cập nhật sản phẩm trong hóa đơn
  async capNhatSanPhamHoaDon(id, data) {
    const response = await api.put(`/admin/hoa-don/${id}/products`, data);
    return response.data.data;
  },

  // Xóa sản phẩm khỏi hóa đơn
  async xoaSanPhamHoaDon(id, idHdct) {
    const response = await api.delete(`/admin/hoa-don/${id}/products/${idHdct}`);
    return response.data.data;
  },

  // Lấy số lượng theo trạng thái
  async laySoLuongHoaDon(params) {
    const response = await api.get('/admin/hoa-don/counts', { params });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelHoaDon(params) {
    const response = await api.get('/admin/hoa-don/export-excel', { params, responseType: 'blob' });
    return response.data;
  },

  // In hóa đơn (trả về HTML)
  async inHoaDon(id) {
    const response = await api.get(`/admin/hoa-don/${id}/print`);
    return response.data; // Đây là nội dung HTML
  }
};
