import apiService from '@/services/apiService';

export const dichVuTinTuc = {
    async layDanhSachTinTuc(params = {}) {
        const response = await apiService.get('/customer/tin-tuc', { params });
        return response.data;
    },

    async layChiTietTinTuc(id) {
        const response = await apiService.get(`/customer/tin-tuc/${id}`);
        return response.data;
    },

    async likeTinTuc(id) {
        const response = await apiService.post(`/customer/tin-tuc/${id}/like`);
        return response.data;
    },

    async binhLuanTinTuc(id, payload) {
        const response = await apiService.post(`/customer/tin-tuc/${id}/comment`, payload);
        return response.data;
    }
};
