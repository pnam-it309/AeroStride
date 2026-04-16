import api from '../apiService';

export const dichVuBienThe = {
  // Lấy danh sách biến thể theo ID sản phẩm
  async layBienTheTheoSanPham(productId) {
    const response = await api.get(`/admin/san-pham/${productId}/variants`);
    return response.data.data;
  },

  // Tạo biến thể mới
  async taoBienThe(productId, variantData) {
    const response = await api.post(`/admin/san-pham/${productId}/variants`, variantData);
    return response.data.data;
  },

  // Cập nhật biến thể
  async capNhatBienThe(variantId, variantData) {
    const response = await api.put(`/admin/san-pham/variants/${variantId}`, variantData);
    return response.data.data;
  },

  // Xóa (mềm) biến thể
  async xoaBienThe(variantId) {
    const response = await api.delete(`/admin/san-pham/variants/${variantId}`);
    return response.data;
  },

  // QUẢN LÝ ẢNH BIẾN THỂ
  
  // Thêm ảnh cho biến thể
  async themAnh(variantId, imageData) {
    const response = await api.post(`/admin/san-pham/variants/${variantId}/images`, imageData);
    return response.data.data;
  },

  // Cập nhật thông tin ảnh
  async capNhatAnh(imageId, imageData) {
    const response = await api.put(`/admin/san-pham/variant-images/${imageId}`, imageData);
    return response.data.data;
  },

  // Xóa ảnh
  async xoaAnh(imageId) {
    const response = await api.delete(`/admin/san-pham/variant-images/${imageId}`);
    return response.data;
  },

  // Đặt làm ảnh chính
  // TODO: Kiểm tra lại endpoint này trên backend nếu cần
  async datAnhChinh(variantId, imageId) {
    const response = await api.put(`/admin/san-pham/variant-images/${imageId}/main`);
    return response.data;
  }
};
