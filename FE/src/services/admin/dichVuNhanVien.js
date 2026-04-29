import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const dichVuNhanVien = {
  // Lấy tất cả nhân viên
  async layTatCaNhanVien() {
    const response = await api.get(`${API_ADMIN.NHAN_VIEN}/hien-thi`);
    return response.data;
  },

  // Lấy nhân viên có phân trang
  async layNhanVienPhanTrang(params) {
    const response = await api.get(`${API_ADMIN.NHAN_VIEN}/phan-trang`, { params });
    return response.data.data;
  },

  // Tìm kiếm nhân viên
  async timKiemNhanVien(params) {
    const response = await api.get(`${API_ADMIN.NHAN_VIEN}/tim-kiem`, { params });
    return response.data.data;
  },

  // Lấy chi tiết nhân viên
  async layChiTietNhanVien(id) {
    const response = await api.get(`${API_ADMIN.NHAN_VIEN}/detail/${id}`);
    return response.data.data;
  },

  // Tạo nhân viên mới
  async taoNhanVien(employeeData) {
    try {
      const response = await api.post(`${API_ADMIN.NHAN_VIEN}/add`, employeeData);
      return response.data.data;
    } catch (error) {
      if (error.response?.status === 404) {
        const response = await api.post(API_ADMIN.NHAN_VIEN, employeeData);
        return response.data.data;
      }
      throw error;
    }
  },

  // Cập nhật nhân viên
  async capNhatNhanVien(id, employeeData) {
    try {
      // Thử đường dẫn hiện tại
      const response = await api.put(`${API_ADMIN.NHAN_VIEN}/update/${id}`, employeeData);
      return response.data.data;
    } catch (error) {
      if (error.response?.status === 404) {
        // Nếu 404, thử đường dẫn chuẩn RESTful (không có /update)
        const response = await api.put(`${API_ADMIN.NHAN_VIEN}/${id}`, employeeData);
        return response.data.data;
      }
      throw error;
    }
  },

  // Xóa nhân viên
  async xoaNhanVien(id) {
    const response = await api.delete(`${API_ADMIN.NHAN_VIEN}/delete/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái nhân viên
  async thayDoiTrangThaiNhanVien(id, status) {
    const response = await api.put(`${API_ADMIN.NHAN_VIEN}/status/${id}`, { status });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelNhanVien() {
    const response = await api.get(`${API_ADMIN.NHAN_VIEN}/export-excel`, { responseType: 'blob' });
    return response.data;
  },

  // Lấy danh sách phân quyền
  async layDanhSachPhanQuyen() {
    const response = await api.get(`${API_ADMIN.NHAN_VIEN}/phan-quyen`);
    return response.data.data;
  }
};
