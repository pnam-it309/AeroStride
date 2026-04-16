import api from '../apiService';

export const dichVuSanPham = {
  // Lấy options cho form sản phẩm
  async layOptionsForm() {
    const response = await api.get('/admin/san-pham/form-options');
    return response.data.data;
  },

  // Lấy danh sách sản phẩm
  async layDanhSachSanPham(params) {
    const response = await api.get('/admin/san-pham', { params });
    return response.data.data;
  },

  // Lấy chi tiết sản phẩm
  async layChiTietSanPham(id) {
    const response = await api.get(`/admin/san-pham/${id}`);
    return response.data.data;
  },

  // Tạo sản phẩm mới
  async taoSanPham(productData) {
    const response = await api.post('/admin/san-pham', productData);
    return response.data.data;
  },

  // Cập nhật sản phẩm
  async capNhatSanPham(id, productData) {
    const response = await api.put(`/admin/san-pham/${id}`, productData);
    return response.data.data;
  },

  // Xóa sản phẩm
  async xoaSanPham(id) {
    const response = await api.delete(`/admin/san-pham/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái sản phẩm
  async thayDoiTrangThai(id, status) {
    const response = await api.put(`/admin/san-pham/${id}/status`, { status });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelSanPham() {
    const response = await api.get('/admin/san-pham/export-excel', { responseType: 'blob' });
    return response.data;
  },

  // Tải template Excel
  async taiTemplateExcel() {
    const response = await api.get('/admin/san-pham/download-template', { responseType: 'blob' });
    return response.data;
  },

  // Nhập Excel
  async nhapExcelSanPham(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/admin/san-pham/import-excel', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  }
};
