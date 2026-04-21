import api from '../apiService';
import { API_THUOC_TINH } from '@/constants/apiPaths';

const getWithParams = async (url, params) => {
  const response = await api.get(url, { params });
  return response.data.data;
};

// Service thương hiệu
export const dichVuThuongHieu = {
  async layThuongHieu(params) {
    return getWithParams(API_THUOC_TINH.THUONG_HIEU, params);
  },
  async taoThuongHieu(data) {
    const response = await api.post(`${API_THUOC_TINH.THUONG_HIEU}/add`, data);
    return response.data.data;
  },
  async capNhatThuongHieu(id, data) {
    const response = await api.put(`${API_THUOC_TINH.THUONG_HIEU}/${id}`, data);
    return response.data.data;
  },
  async xoaThuongHieu(id) {
    const response = await api.delete(`${API_THUOC_TINH.THUONG_HIEU}/${id}`);
    return response.data;
  }
};

// Service danh mục
export const dichVuDanhMuc = {
  async layDanhMuc(params) {
    return getWithParams(API_THUOC_TINH.DANH_MUC, params);
  },
  async taoDanhMuc(data) {
    const response = await api.post(`${API_THUOC_TINH.DANH_MUC}/add`, data);
    return response.data.data;
  },
  async capNhatDanhMuc(id, data) {
    const response = await api.put(`${API_THUOC_TINH.DANH_MUC}/${id}`, data);
    return response.data.data;
  },
  async xoaDanhMuc(id) {
    const response = await api.delete(`${API_THUOC_TINH.DANH_MUC}/${id}`);
    return response.data;
  }
};

// Service màu sắc
export const dichVuMauSac = {
  async layMauSac(params) {
    return getWithParams(API_THUOC_TINH.MAU_SAC, params);
  },
  async taoMauSac(data) {
    const response = await api.post(`${API_THUOC_TINH.MAU_SAC}/add`, data);
    return response.data.data;
  },
  async capNhatMauSac(id, data) {
    const response = await api.put(`${API_THUOC_TINH.MAU_SAC}/${id}`, data);
    return response.data.data;
  },
  async xoaMauSac(id) {
    const response = await api.delete(`${API_THUOC_TINH.MAU_SAC}/${id}`);
    return response.data;
  }
};

// Service kích thước
export const dichVuKichThuoc = {
  async layKichThuoc(params) {
    return getWithParams(API_THUOC_TINH.KICH_THUOC, params);
  },
  async taoKichThuoc(data) {
    const response = await api.post(`${API_THUOC_TINH.KICH_THUOC}/add`, data);
    return response.data.data;
  },
  async capNhatKichThuoc(id, data) {
    const response = await api.put(`${API_THUOC_TINH.KICH_THUOC}/${id}`, data);
    return response.data.data;
  },
  async xoaKichThuoc(id) {
    const response = await api.delete(`${API_THUOC_TINH.KICH_THUOC}/${id}`);
    return response.data;
  }
};

// Service chất liệu
export const dichVuChatLieu = {
  async layChatLieu(params) {
    return getWithParams(API_THUOC_TINH.CHAT_LIEU, params);
  },
  async taoChatLieu(data) {
    const response = await api.post(`${API_THUOC_TINH.CHAT_LIEU}/add`, data);
    return response.data.data;
  },
  async capNhatChatLieu(id, data) {
    const response = await api.put(`${API_THUOC_TINH.CHAT_LIEU}/${id}`, data);
    return response.data.data;
  },
  async xoaChatLieu(id) {
    const response = await api.delete(`${API_THUOC_TINH.CHAT_LIEU}/${id}`);
    return response.data;
  }
};

// Service đế giày
export const dichVuDeGiay = {
  async layDeGiay(params) {
    return getWithParams(API_THUOC_TINH.DE_GIAY, params);
  },
  async taoDeGiay(data) {
    const response = await api.post(`${API_THUOC_TINH.DE_GIAY}/add`, data);
    return response.data.data;
  },
  async capNhatDeGiay(id, data) {
    const response = await api.put(`${API_THUOC_TINH.DE_GIAY}/${id}`, data);
    return response.data.data;
  },
  async xoaDeGiay(id) {
    const response = await api.delete(`${API_THUOC_TINH.DE_GIAY}/${id}`);
    return response.data;
  }
};

// Service cổ giày
export const dichVuCoGiay = {
  async layCoGiay(params) {
    return getWithParams(API_THUOC_TINH.CO_GIAY, params);
  },
  async taoCoGiay(data) {
    const response = await api.post(`${API_THUOC_TINH.CO_GIAY}/add`, data);
    return response.data.data;
  },
  async capNhatCoGiay(id, data) {
    const response = await api.put(`${API_THUOC_TINH.CO_GIAY}/${id}`, data);
    return response.data.data;
  },
  async xoaCoGiay(id) {
    const response = await api.delete(`${API_THUOC_TINH.CO_GIAY}/${id}`);
    return response.data;
  }
};

// Service xuất xứ
export const dichVuXuatXu = {
  async layXuatXu(params) {
    return getWithParams(API_THUOC_TINH.XUAT_XU, params);
  },
  async taoXuatXu(data) {
    const response = await api.post(`${API_THUOC_TINH.XUAT_XU}/add`, data);
    return response.data.data;
  },
  async capNhatXuatXu(id, data) {
    const response = await api.put(`${API_THUOC_TINH.XUAT_XU}/${id}`, data);
    return response.data.data;
  },
  async xoaXuatXu(id) {
    const response = await api.delete(`${API_THUOC_TINH.XUAT_XU}/${id}`);
    return response.data;
  }
};

// Service mục đích chạy
export const dichVuMucDichChay = {
  async layMucDichChay(params) {
    return getWithParams(API_THUOC_TINH.MUC_DICH_CHAY, params);
  },
  async taoMucDichChay(data) {
    const response = await api.post(`${API_THUOC_TINH.MUC_DICH_CHAY}/add`, data);
    return response.data.data;
  },
  async capNhatMucDichChay(id, data) {
    const response = await api.put(`${API_THUOC_TINH.MUC_DICH_CHAY}/${id}`, data);
    return response.data.data;
  },
  async xoaMucDichChay(id) {
    const response = await api.delete(`${API_THUOC_TINH.MUC_DICH_CHAY}/${id}`);
    return response.data;
  }
};
