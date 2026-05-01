import api from '../apiService';
import { API_PRODUCT } from '@/constants/apiPaths';

export const dichVuBienThe = {
  // Lấy danh sách biến thể theo ID sản phẩm
  async layBienTheTheoSanPham(productId) {
    const response = await api.get(`${API_PRODUCT.SAN_PHAM}/${productId}/variants`);
    return response.data.data;
  },

  // Lấy tất cả biến thể
  async layTatCaBienThe() {
    const response = await api.get(API_PRODUCT.BIEN_THE);
    return response.data.data;
  },

  // Tạo biến thể mới
  async taoBienThe(productId, variantData) {
    const response = await api.post(`${API_PRODUCT.SAN_PHAM}/${productId}/variants`, variantData);
    return response.data.data;
  },

  // Cập nhật biến thể
  async capNhatBienThe(variantId, variantData) {
    const response = await api.put(`${API_PRODUCT.BIEN_THE}/${variantId}`, variantData);
    return response.data.data;
  },

  // Xóa (mềm) biến thể
  async xoaBienThe(variantId) {
    const response = await api.delete(`${API_PRODUCT.BIEN_THE}/${variantId}`);
    return response.data;
  },

  // QUẢN LÝ ẢNH BIẾN THỂ
  
  // Thêm ảnh cho biến thể
  async themAnh(variantId, imageData) {
    const response = await api.post(`${API_PRODUCT.BIEN_THE}/${variantId}/images`, imageData);
    return response.data.data;
  },

  // Cập nhật thông tin ảnh
  async capNhatAnh(imageId, imageData) {
    const response = await api.put(`${API_PRODUCT.ANH_BIEN_THE}/${imageId}`, imageData);
    return response.data.data;
  },

  // Xóa ảnh
  async xoaAnh(imageId) {
    const response = await api.delete(`${API_PRODUCT.ANH_BIEN_THE}/${imageId}`);
    return response.data;
  },

  // Đặt làm ảnh chính
  async datAnhChinh(variantId, imageId) {
    const response = await api.put(`${API_PRODUCT.ANH_BIEN_THE}/${imageId}/main`);
    return response.data;
  }
};
