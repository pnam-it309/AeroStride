import api from '../apiService';

export const dichVuNhanVien = {
  // Lấy tất cả nhân viên
  async layTatCaNhanVien() {
    const response = await api.get('/admin/nhan-vien/hien-thi');
    return response.data;
  },

  // Lấy nhân viên có phân trang
  async layNhanVienPhanTrang(params) {
    const response = await api.get('/admin/nhan-vien/phan-trang', { params });
    return response.data.data;
  },

  // Tìm kiếm nhân viên
  async timKiemNhanVien(params) {
    const response = await api.get('/admin/nhan-vien/tim-kiem', { params });
    return response.data.data;
  },

  // Lấy chi tiết nhân viên
  async layChiTietNhanVien(id) {
    const response = await api.get(`/admin/nhan-vien/detail/${id}`);
    return response.data.data;
  },

  // Tạo nhân viên mới
  async taoNhanVien(employeeData) {
    const response = await api.post('/admin/nhan-vien/add', employeeData);
    return response.data.data;
  },

  // Cập nhật nhân viên
  async capNhatNhanVien(id, employeeData) {
    const response = await api.put(`/admin/nhan-vien/update/${id}`, employeeData);
    return response.data.data;
  },

  // Xóa nhân viên
  async xoaNhanVien(id) {
    const response = await api.delete(`/admin/nhan-vien/delete/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái nhân viên
  async thayDoiTrangThaiNhanVien(id, status) {
    const response = await api.put(`/admin/nhan-vien/status/${id}`, { status });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelNhanVien() {
    const response = await api.get('/admin/nhan-vien/export-excel', { responseType: 'blob' });
    return response.data;
  },
 
  // Lấy danh sách phân quyền
  async layDanhSachPhanQuyen() {
    const response = await api.get('/admin/nhan-vien/phan-quyen');
    return response.data.data;
  }
};
