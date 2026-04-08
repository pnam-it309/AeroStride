import api from '../apiService';

export const dichVuFile = {
  // Tải lên file
  async taiLenFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await api.post('/storage/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  // Lấy danh sách file
  async layDanhSachFile(params = {}) {
    const response = await api.get('/storage/files', { params });
    return response.data.data;
  },

  // Xóa file
  async xoaFile(fileId) {
    const response = await api.delete(`/storage/files/${fileId}`);
    return response.data;
  },

  // Lấy URL file
  layUrlFile(filePath) {
    return `${import.meta.env.VITE_API_URL}/storage/files/${filePath}`;
  }
};
