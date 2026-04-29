import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuDotGiamGia = {
  // Lấy tất cả đợt giảm giá
  async layTatCaDotGiamGia() {
    const response = await api.get(API_ADMIN.DOT_GIAM_GIA);
    return response.data.data;
  },

  // Lấy đợt giảm giá có phân trang
  async layDotGiamGiaPhanTrang(params) {
    const response = await api.get(`${API_ADMIN.DOT_GIAM_GIA}/phan-trang`, { params });
    return response.data;
  },

  // Lấy chi tiết đợt giảm giá
  async layChiTietDotGiamGia(id) {
    const response = await api.get(`${API_ADMIN.DOT_GIAM_GIA}/detail/${id}`);
    return response.data.data;
  },

  // Tạo đợt giảm giá mới
  async taoDotGiamGia(campaignData) {
    const response = await api.post(API_ADMIN.DOT_GIAM_GIA, campaignData);
    return response.data.data;
  },

  // Cập nhật đợt giảm giá
  async capNhatDotGiamGia(id, campaignData) {
    const response = await api.put(`${API_ADMIN.DOT_GIAM_GIA}/${id}`, campaignData);
    return response.data.data;
  },

  // Xóa đợt giảm giá
  async xoaDotGiamGia(id) {
    const response = await api.delete(`${API_ADMIN.DOT_GIAM_GIA}/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái đợt giảm giá
  async thayDoiTrangThaiDotGiamGia(id, status) {
    // Backend: @PatchMapping("/{id}/trang-thai")
    const response = await api.patch(`${API_ADMIN.DOT_GIAM_GIA}/${id}/trang-thai`, null, { params: { status } });
    return response.data;
  },

  // Lấy danh sách sản phẩm để áp dụng (Dropdown)
  async layDanhSachSanPhamApDung() {
    const response = await api.get(`${API_ADMIN.DOT_GIAM_GIA}/san-pham-ap-dung`);
    return response.data.data;
  },

  // Lấy danh sách biến thể đã áp dụng cho đợt giảm giá
  async layDanhSachBienTheApDung(id) {
    const response = await api.get(`${API_ADMIN.DOT_GIAM_GIA}/bien-the-ap-dung/${id}`);
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelDotGiamGia() {
    const response = await api.get(`${API_ADMIN.DOT_GIAM_GIA}/export-excel`, { responseType: 'blob' });
    return response.data;
  }
};
