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
  }
};
