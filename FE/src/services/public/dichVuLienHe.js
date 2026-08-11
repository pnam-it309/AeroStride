import apiService from '@/services/apiService';

export const dichVuLienHe = {
    async guiLienHe(payload) {
        const response = await apiService.post('/customer/lien-he', payload);
        return response.data;
    }
};
