import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuHoaDon = {
  // Lấy hóa đơn có phân trang
  async layHoaDonPhanTrang(params) {
    const response = await api.get(`${API_ADMIN.HOA_DON}/phan-trang`, { params });
    return response.data.data;
  },

  // Lấy chi tiết hóa đơn
  async layChiTietHoaDon(id) {
    const response = await api.get(`${API_ADMIN.HOA_DON}/detail/${id}`);
    return response.data.data;
  },

  // Cập nhật trạng thái
  async capNhatTrangThaiHoaDon(id, status) {
    // Backend supports PATCH /{id}/status or PUT /status/{id}
    const response = await api.patch(`${API_ADMIN.HOA_DON}/${id}/status`, null, { params: { status } });
    return response.data.data;
  },

  // Cập nhật thông tin giao hàng/khách hàng
  async capNhatThongTinHoaDon(id, data) {
    const response = await api.put(`${API_ADMIN.HOA_DON}/${id}/info`, data);
    return response.data.data;
  },

  // Thêm/Cập nhật sản phẩm trong hóa đơn
  async capNhatSanPhamHoaDon(id, data) {
    const response = await api.put(`${API_ADMIN.HOA_DON}/${id}/products`, data);
    return response.data.data;
  },

  // Xóa sản phẩm khỏi hóa đơn
  async xoaSanPhamHoaDon(id, idHdct) {
    const response = await api.delete(`${API_ADMIN.HOA_DON}/${id}/products/${idHdct}`);
    return response.data.data;
  },

  // Lấy số lượng theo trạng thái
  async laySoLuongHoaDon(params) {
    const response = await api.get(`${API_ADMIN.HOA_DON}/counts`, { params });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelHoaDon(params) {
    const response = await api.get(`${API_ADMIN.HOA_DON}/export-excel`, { params, responseType: 'blob' });
    return response.data;
  },

  // In hóa đơn (trả về HTML)
  async inHoaDon(id) {
    const response = await api.get(`${API_ADMIN.HOA_DON}/${id}/print`);
    return response.data;
  }
};
