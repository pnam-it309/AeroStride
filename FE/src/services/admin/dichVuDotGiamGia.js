import api from '../apiService';

export const dichVuDotGiamGia = {
  // Lấy tất cả đợt giảm giá
  async layTatCaDotGiamGia() {
    const response = await api.get('/admin/dot-giam-gia');
    return response.data.data;
  },

  // Lấy đợt giảm giá có phân trang
  async layDotGiamGiaPhanTrang(params) {
    const response = await api.get('/admin/dot-giam-gia/phan-trang', { params });
    return response.data.data;
  },

  // Lấy chi tiết đợt giảm giá
  async layChiTietDotGiamGia(id) {
    const response = await api.get(`/admin/dot-giam-gia/detail/${id}`);
    return response.data.data;
  },

  // Tạo đợt giảm giá mới
  async taoDotGiamGia(campaignData) {
    const response = await api.post('/admin/dot-giam-gia/add', campaignData);
    return response.data.data;
  },

  // Cập nhật đợt giảm giá
  async capNhatDotGiamGia(id, campaignData) {
    const response = await api.put(`/admin/dot-giam-gia/update/${id}`, campaignData);
    return response.data.data;
  },

  // Xóa đợt giảm giá
  async xoaDotGiamGia(id) {
    const response = await api.delete(`/admin/dot-giam-gia/delete/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái đợt giảm giá
  async thayDoiTrangThaiDotGiamGia(id, status) {
    const response = await api.put(`/admin/dot-giam-gia/status/${id}`, null, { params: { status } });
    return response.data;
  },

  // Lấy danh sách sản phẩm để áp dụng (Dropdown)
  async layDanhSachSanPhamApDung() {
    const response = await api.get('/admin/dot-giam-gia/san-pham-ap-dung');
    return response.data.data;
  },

  // Lấy danh sách biến thể đã áp dụng cho đợt giảm giá
  async layDanhSachBienTheApDung(id) {
    const response = await api.get(`/admin/dot-giam-gia/bien-the-ap-dung/${id}`);
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelDotGiamGia() {
    const response = await api.get('/admin/dot-giam-gia/export-excel', { responseType: 'blob' });
    return response.data;
  }
};
