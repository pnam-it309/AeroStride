import api from '../apiService';

export const dichVuKhachHang = {
  // Lấy tất cả khách hàng
  async layTatCaKhachHang() {
    const response = await api.get('/admin/khach-hang/hien-thi');
    return response.data?.data || response.data;
  },

  // Lấy khách hàng có phân trang
  async layKhachHangPhanTrang(params) {
    const response = await api.get('/admin/khach-hang/phan-trang', { params });
    return response.data.data;
  },

  // Tìm kiếm khách hàng
  async timKiemKhachHang(params) {
    const response = await api.get('/admin/khach-hang/tim-kiem', { params });
    return response.data.data;
  },

  // Lọc khách hàng
  async locKhachHang(params) {
    const response = await api.get('/admin/khach-hang/filter', { params });
    return response.data.data;
  },

  // Lấy chi tiết khách hàng
  async layChiTietKhachHang(id) {
    const response = await api.get(`/admin/khach-hang/detail/${id}`);
    return response.data.data;
  },

  // Tạo khách hàng mới
  async taoKhachHang(customerData) {
    const response = await api.post('/admin/khach-hang/add', customerData);
    return response.data.data;
  },

  // Cập nhật khách hàng
  async capNhatKhachHang(id, customerData) {
    const response = await api.put(`/admin/khach-hang/update/${id}`, customerData);
    return response.data.data;
  },

  // Xóa khách hàng
  async xoaKhachHang(id) {
    const response = await api.delete(`/admin/khach-hang/delete/${id}`);
    return response.data;
  },

  // Xuất Excel
  async xuatExcelKhachHang() {
    const response = await api.get('/admin/khach-hang/export-excel', { responseType: 'blob' });
    return response.data;
  },

  // --- DIA CHI ---
  async layDanhSachDiaChi(khId) {
    const response = await api.get(`/admin/dia-chi/khach-hang/${khId}`);
    return response.data;
  },

  async taoDiaChi(data) {
    const response = await api.post('/admin/dia-chi/add', data);
    return response.data;
  },

  async capNhatDiaChi(id, data) {
    const response = await api.put(`/admin/dia-chi/update/${id}`, data);
    return response.data;
  },

  async xoaDiaChi(id) {
    const response = await api.delete(`/admin/dia-chi/delete/${id}`);
    return response.data;
  },

  async datDiaChiMacDinh(id) {
    const response = await api.patch(`/admin/dia-chi/set-default/${id}`);
    return response.data;
  }
};
