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

  // Lấy biến thể của sản phẩm
  async layBienTheSanPham(productId) {
    const response = await api.get(`/admin/san-pham/${productId}/variants`);
    return response.data.data;
  },

  // Thêm biến thể sản phẩm
  async themBienTheSanPham(productId, variantData) {
    const response = await api.post(`/admin/san-pham/${productId}/variants`, variantData);
    return response.data.data;
  },

  // Cập nhật biến thể sản phẩm
  async capNhatBienTheSanPham(productId, variantId, variantData) {
    const response = await api.put(`/admin/san-pham/${productId}/variants/${variantId}`, variantData);
    return response.data.data;
  },

  // Xóa biến thể sản phẩm
  async xoaBienTheSanPham(productId, variantId) {
    const response = await api.delete(`/admin/san-pham/${productId}/variants/${variantId}`);
    return response.data;
  }
};
