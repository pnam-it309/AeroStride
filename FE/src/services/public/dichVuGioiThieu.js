import apiService from '@/services/apiService';

export const dichVuGioiThieu = {
    async layThongKeGioiThieu() {
        const response = await apiService.get('/customer/gioi-thieu/stats');
        return response.data;
    }
};
