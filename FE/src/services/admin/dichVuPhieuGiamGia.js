import api from '../apiService';

export const dichVuPhieuGiamGia = {
  // Lấy tất cả phiếu giảm giá
  async layTatCaPhieuGiamGia() {
    const response = await api.get('/admin/phieu-giam-gia');
    return response.data.data;
  },

  // Lấy phiếu giảm giá có phân trang
  async layPhieuGiamGiaPhanTrang(params) {
    const response = await api.get('/admin/phieu-giam-gia/phan-trang', { params });
    return response.data.data;
  },

  // Lấy chi tiết phiếu giảm giá
  async layChiTietPhieuGiamGia(id) {
    const response = await api.get(`/admin/phieu-giam-gia/detail/${id}`);
    return response.data.data;
  },

  // Tạo phiếu giảm giá mới
  async taoPhieuGiamGia(voucherData) {
    const response = await api.post('/admin/phieu-giam-gia/add', voucherData);
    return response.data.data;
  },

  // Cập nhật phiếu giảm giá
  async capNhatPhieuGiamGia(id, voucherData) {
    const response = await api.put(`/admin/phieu-giam-gia/update/${id}`, voucherData);
    return response.data.data;
  },

  // Xóa phiếu giảm giá
  async xoaPhieuGiamGia(id) {
    const response = await api.delete(`/admin/phieu-giam-gia/delete/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái phiếu giảm giá
  async thayDoiTrangThaiPhieuGiamGia(id, status) {
    const response = await api.put(`/admin/phieu-giam-gia/status/${id}`, { status });
    return response.data.data;
  },

  // Tạo mã phiếu giảm giá
  async taoMaPhieuGiamGia(voucherId, quantity) {
    const response = await api.post(`/admin/phieu-giam-gia/${voucherId}/generate-codes`, { quantity });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelPhieuGiamGia() {
    const response = await api.get('/admin/phieu-giam-gia/export-excel', { responseType: 'blob' });
    return response.data;
  }
};
