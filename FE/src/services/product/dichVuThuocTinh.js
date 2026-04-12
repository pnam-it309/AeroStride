import api from '../apiService';

const getWithParams = async (url, params) => {
  const response = await api.get(url, { params });
  return response.data.data;
};

// Service thương hiệu
export const dichVuThuongHieu = {
  async layThuongHieu(params) {
    return getWithParams('/admin/thuoc-tinh/thuong-hieu', params);
  },
  async taoThuongHieu(data) {
    const response = await api.post('/admin/thuoc-tinh/thuong-hieu/add', data);
    return response.data.data;
  },
  async capNhatThuongHieu(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/thuong-hieu/${id}`, data);
    return response.data.data;
  },
  async xoaThuongHieu(id) {
    const response = await api.delete(`/admin/thuoc-tinh/thuong-hieu/delete/${id}`);
    return response.data;
  }
};

// Service danh mục
export const dichVuDanhMuc = {
  async layDanhMuc(params) {
    return getWithParams('/admin/thuoc-tinh/danh-muc', params);
  },
  async taoDanhMuc(data) {
    const response = await api.post('/admin/thuoc-tinh/danh-muc/add', data);
    return response.data.data;
  },
  async capNhatDanhMuc(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/danh-muc/${id}`, data);
    return response.data.data;
  },
  async xoaDanhMuc(id) {
    const response = await api.delete(`/admin/thuoc-tinh/danh-muc/delete/${id}`);
    return response.data;
  }
};

// Service màu sắc
export const dichVuMauSac = {
  async layMauSac(params) {
    return getWithParams('/admin/thuoc-tinh/mau-sac', params);
  },
  async taoMauSac(data) {
    const response = await api.post('/admin/thuoc-tinh/mau-sac/add', data);
    return response.data.data;
  },
  async capNhatMauSac(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/mau-sac/${id}`, data);
    return response.data.data;
  },
  async xoaMauSac(id) {
    const response = await api.delete(`/admin/thuoc-tinh/mau-sac/delete/${id}`);
    return response.data;
  }
};

// Service kích thước
export const dichVuKichThuoc = {
  async layKichThuoc(params) {
    return getWithParams('/admin/thuoc-tinh/kich-thuoc', params);
  },
  async taoKichThuoc(data) {
    const response = await api.post('/admin/thuoc-tinh/kich-thuoc/add', data);
    return response.data.data;
  },
  async capNhatKichThuoc(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/kich-thuoc/${id}`, data);
    return response.data.data;
  },
  async xoaKichThuoc(id) {
    const response = await api.delete(`/admin/thuoc-tinh/kich-thuoc/delete/${id}`);
    return response.data;
  }
};

// Service chất liệu
export const dichVuChatLieu = {
  async layChatLieu(params) {
    return getWithParams('/admin/thuoc-tinh/chat-lieu', params);
  },
  async taoChatLieu(data) {
    const response = await api.post('/admin/thuoc-tinh/chat-lieu/add', data);
    return response.data.data;
  },
  async capNhatChatLieu(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/chat-lieu/${id}`, data);
    return response.data.data;
  },
  async xoaChatLieu(id) {
    const response = await api.delete(`/admin/thuoc-tinh/chat-lieu/delete/${id}`);
    return response.data;
  }
};

// Service đế giày
export const dichVuDeGiay = {
  async layDeGiay(params) {
    return getWithParams('/admin/thuoc-tinh/de-giay', params);
  },
  async taoDeGiay(data) {
    const response = await api.post('/admin/thuoc-tinh/de-giay/add', data);
    return response.data.data;
  },
  async capNhatDeGiay(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/de-giay/${id}`, data);
    return response.data.data;
  },
  async xoaDeGiay(id) {
    const response = await api.delete(`/admin/thuoc-tinh/de-giay/delete/${id}`);
    return response.data;
  }
};

// Service cổ giày
export const dichVuCoGiay = {
  async layCoGiay(params) {
    return getWithParams('/admin/thuoc-tinh/co-giay', params);
  },
  async taoCoGiay(data) {
    const response = await api.post('/admin/thuoc-tinh/co-giay/add', data);
    return response.data.data;
  },
  async capNhatCoGiay(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/co-giay/${id}`, data);
    return response.data.data;
  },
  async xoaCoGiay(id) {
    const response = await api.delete(`/admin/thuoc-tinh/co-giay/delete/${id}`);
    return response.data;
  }
};

// Service xuất xứ
export const dichVuXuatXu = {
  async layXuatXu(params) {
    return getWithParams('/admin/thuoc-tinh/xuat-xu', params);
  },
  async taoXuatXu(data) {
    const response = await api.post('/admin/thuoc-tinh/xuat-xu/add', data);
    return response.data.data;
  },
  async capNhatXuatXu(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/xuat-xu/${id}`, data);
    return response.data.data;
  },
  async xoaXuatXu(id) {
    const response = await api.delete(`/admin/thuoc-tinh/xuat-xu/delete/${id}`);
    return response.data;
  }
};

// Service mục đích chạy
export const dichVuMucDichChay = {
  async layMucDichChay(params) {
    return getWithParams('/admin/thuoc-tinh/muc-dich-chay', params);
  },
  async taoMucDichChay(data) {
    const response = await api.post('/admin/thuoc-tinh/muc-dich-chay/add', data);
    return response.data.data;
  },
  async capNhatMucDichChay(id, data) {
    const response = await api.put(`/admin/thuoc-tinh/muc-dich-chay/${id}`, data);
    return response.data.data;
  },
  async xoaMucDichChay(id) {
    const response = await api.delete(`/admin/thuoc-tinh/muc-dich-chay/delete/${id}`);
    return response.data;
  }
};
