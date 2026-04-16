import api from '../apiService';

const unwrap = (response) => response?.data?.data ?? response?.data ?? null;

export const dichVuSanPham = {
  // Lấy options cho form sản phẩm
  async layOptionsForm() {
    const response = await api.get('/admin/san-pham/form-options');
    return unwrap(response);
  },

  // Lấy danh sách sản phẩm
  async layDanhSachSanPham(params) {
    const response = await api.get('/admin/san-pham', { params });
    return unwrap(response);
  },

  // Lấy chi tiết sản phẩm
  async layChiTietSanPham(id) {
    const response = await api.get(`/admin/san-pham/${id}`);
    return unwrap(response);
  },

  // Tạo sản phẩm mới
  async taoSanPham(productData) {
    const response = await api.post('/admin/san-pham', productData);
    return unwrap(response);
  },

  // Cập nhật sản phẩm
  async capNhatSanPham(id, productData) {
    const response = await api.put(`/admin/san-pham/${id}`, productData);
    return unwrap(response);
  },

  // Xóa sản phẩm
  async xoaSanPham(id) {
    const response = await api.delete(`/admin/san-pham/${id}`);
    return unwrap(response);
  },

  // Thay đổi trạng thái sản phẩm
  async thayDoiTrangThai(id, status) {
    const response = await api.put(`/admin/san-pham/${id}/status`, { status });
    return unwrap(response);
  },

  // Lấy biến thể của sản phẩm
  async layBienTheSanPham(productId) {
    const response = await api.get(`/admin/san-pham/${productId}/variants`);
    return unwrap(response);
  },

  // Thêm biến thể sản phẩm
  async themBienTheSanPham(productId, variantData) {
    const response = await api.post(`/admin/san-pham/${productId}/variants`, variantData);
    return unwrap(response);
  },

  // Cập nhật biến thể sản phẩm
  async capNhatBienTheSanPham(variantId, variantData) {
    const response = await api.put(`/admin/san-pham/variants/${variantId}`, variantData);
    return unwrap(response);
  },

  // Xóa biến thể sản phẩm
  async xoaBienTheSanPham(variantId) {
    const response = await api.delete(`/admin/san-pham/variants/${variantId}`);
    return unwrap(response);
  },

  // Thêm ảnh cho biến thể
  async themAnhBienThe(variantId, imageData) {
    const response = await api.post(`/admin/san-pham/variants/${variantId}/images`, imageData);
    return unwrap(response);
  },

  // Cập nhật ảnh biến thể
  async capNhatAnhBienThe(imageId, imageData) {
    const response = await api.put(`/admin/san-pham/variant-images/${imageId}`, imageData);
    return unwrap(response);
  },

  // Xóa ảnh biến thể
  async xoaAnhBienThe(imageId) {
    const response = await api.delete(`/admin/san-pham/variant-images/${imageId}`);
    return unwrap(response);
  },

  // Upload file ảnh sản phẩm hoặc biến thể
  async taiLenTepSanPham(file, folder = 'aerostride/products') {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', folder);

    const response = await api.post('/storage/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return unwrap(response);
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
