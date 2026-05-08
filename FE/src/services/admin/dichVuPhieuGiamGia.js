import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuPhieuGiamGia = {
  // Lấy tất cả phiếu giảm giá
  async layTatCaPhieuGiamGia() {
    const response = await api.get(API_ADMIN.PHIEU_GIAM_GIA);
    return response.data.data;
  },

  // Lấy phiếu giảm giá có phân trang
  async layPhieuGiamGiaPhanTrang(params) {
    const response = await api.get(`${API_ADMIN.PHIEU_GIAM_GIA}/phan-trang`, { params });
    return response.data;
  },

  // Lấy chi tiết phiếu giảm giá
  async layChiTietPhieuGiamGia(id) {
    const response = await api.get(`${API_ADMIN.PHIEU_GIAM_GIA}/detail/${id}`);
    return response.data.data;
  },

  // Tạo phiếu giảm giá mới
  async taoPhieuGiamGia(voucherData) {
    const response = await api.post(`${API_ADMIN.PHIEU_GIAM_GIA}/add`, voucherData);
    return response.data.data;
  },

  // Cập nhật phiếu giảm giá
  async capNhatPhieuGiamGia(id, voucherData) {
    const response = await api.put(`${API_ADMIN.PHIEU_GIAM_GIA}/update/${id}`, voucherData);
    return response.data.data;
  },

  // Xóa phiếu giảm giá
  async xoaPhieuGiamGia(id) {
    const response = await api.delete(`${API_ADMIN.PHIEU_GIAM_GIA}/delete/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái phiếu giảm giá
  async thayDoiTrangThaiPhieuGiamGia(id, status) {
    const response = await api.put(`${API_ADMIN.PHIEU_GIAM_GIA}/${id}/status`, { status });
    return response.data.data;
  },

  // Tạo mã phiếu giảm giá
  async taoMaPhieuGiamGia(voucherId, quantity) {
    const response = await api.post(`${API_ADMIN.PHIEU_GIAM_GIA}/${voucherId}/generate-codes`, { quantity });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelPhieuGiamGia() {
    const response = await api.get(`${API_ADMIN.PHIEU_GIAM_GIA}/export-excel`, { responseType: 'blob' });
    return response.data;
  }
};
