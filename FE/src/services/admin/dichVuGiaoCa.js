import apiService from '../apiService';

const API_PATH = '/admin/giao-ca';

export const dichVuGiaoCa = {
    // Mở ca
    moCa: async (payload) => {
        const response = await apiService.post(`${API_PATH}/mo-ca`, payload);
        return response.data;
    },

    // Kiểm tra ca hiện tại
    getCaHienTai: async () => {
        const response = await apiService.get(`${API_PATH}/hien-tai`);
        return response.data;
    },

    // Chốt ca
    chotCa: async (payload) => {
        const response = await apiService.put(`${API_PATH}/chot-ca`, payload);
        return response.data;
    },

    // Lịch sử giao ca
    getAllLichSu: async () => {
        const response = await apiService.get(`${API_PATH}`);
        return response.data;
    }
};
