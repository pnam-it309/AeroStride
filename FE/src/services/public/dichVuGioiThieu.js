import apiService from '@/services/apiService';

export const dichVuGioiThieu = {
    async layThongKeGioiThieu() {
        const response = await apiService.get('/customer/gioi-thieu/stats');
        return response.data;
    },
    async layDanhSachLanhDao() {
        const response = await apiService.get('/customer/gioi-thieu/team');
        return response.data;
    },
    async layDanhSachThuongHieu() {
        const response = await apiService.get('/customer/gioi-thieu/brands');
        return response.data;
    }
};
