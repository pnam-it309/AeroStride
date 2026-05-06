import api from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';
import {
  dichVuThuongHieu,
  dichVuDanhMuc,
  dichVuXuatXu,
  dichVuMucDichChay,
  dichVuChatLieu,
  dichVuDeGiay,
  dichVuCoGiay,
  dichVuMauSac,
  dichVuKichThuoc
} from './dichVuThuocTinh';

const toOptionList = (response) => response?.content || response || [];

const loadOptionSafely = async (request) => {
  try {
    return toOptionList(await request({ size: 1000 }));
  } catch (error) {
    return [];
  }
};

export const dichVuSanPham = {
  // Lấy options cho form sản phẩm
  async layOptionsForm() {
    const [
      thuongHieus,
      danhMucs,
      xuatXus,
      mucDichChays,
      chatLieus,
      deGiays,
      coGiays,
      mauSacs,
      kichThuocs
    ] = await Promise.all([
      loadOptionSafely(dichVuThuongHieu.layThuongHieu),
      loadOptionSafely(dichVuDanhMuc.layDanhMuc),
      loadOptionSafely(dichVuXuatXu.layXuatXu),
      loadOptionSafely(dichVuMucDichChay.layMucDichChay),
      loadOptionSafely(dichVuChatLieu.layChatLieu),
      loadOptionSafely(dichVuDeGiay.layDeGiay),
      loadOptionSafely(dichVuCoGiay.layCoGiay),
      loadOptionSafely(dichVuMauSac.layMauSac),
      loadOptionSafely(dichVuKichThuoc.layKichThuoc)
    ]);

    return {
      thuongHieus,
      danhMucs,
      xuatXus,
      mucDichChays,
      chatLieus,
      deGiays,
      coGiays,
      mauSacs,
      kichThuocs,
      trangThais: ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'],
      gioiTinhKhachHangs: ['NAM', 'NU', 'TRE_EM', 'UNISEX']
    };
  },

  // Lấy danh sách sản phẩm
  async layDanhSachSanPham(params) {
    const response = await api.get(API_ADMIN.SAN_PHAM, { params });
    return response.data.data;
  },

  // Lấy chi tiết sản phẩm
  async layChiTietSanPham(id) {
    const response = await api.get(`${API_ADMIN.SAN_PHAM}/${id}`);
    return response.data.data;
  },

  // Tạo sản phẩm mới
  async taoSanPham(productData) {
    const response = await api.post(API_ADMIN.SAN_PHAM, productData);
    return response.data.data;
  },

  // Cập nhật sản phẩm
  async capNhatSanPham(id, productData) {
    const response = await api.put(`${API_ADMIN.SAN_PHAM}/${id}`, productData);
    return response.data.data;
  },

  // Xóa sản phẩm
  async xoaSanPham(id) {
    const response = await api.delete(`${API_ADMIN.SAN_PHAM}/${id}`);
    return response.data;
  },

  // Thay đổi trạng thái sản phẩm
  async thayDoiTrangThai(id, status) {
    const response = await api.put(`${API_ADMIN.SAN_PHAM}/${id}/status`, { status });
    return response.data.data;
  },

  // Xuất Excel
  async xuatExcelSanPham() {
    const response = await api.get(`${API_ADMIN.SAN_PHAM}/export-excel`, { responseType: 'blob' });
    return response.data;
  },

  // Tải template Excel
  async taiTemplateExcel() {
    const response = await api.get(`${API_ADMIN.SAN_PHAM}/download-template`, { responseType: 'blob' });
    return response.data;
  },

  // Nhập Excel
  async nhapExcelSanPham(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post(`${API_ADMIN.SAN_PHAM}/import-excel`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },
  
  // Lấy giá lớn nhất
  async layGiaLonNhat() {
    const response = await api.get(`${API_ADMIN.SAN_PHAM}/max-price`);
    return response.data.data;
  }
};
