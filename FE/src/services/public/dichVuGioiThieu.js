import apiService from '@/services/apiService';

let cachedStats = null;
let cachedTeam = null;
let cachedBrands = null;

export const dichVuGioiThieu = {
    async layThongKeGioiThieu(forceRefresh = false) {
        if (!forceRefresh && cachedStats) {
            return cachedStats;
        }
        const response = await apiService.get('/customer/gioi-thieu/stats');
        if (response.data) cachedStats = response.data;
        return response.data;
    },
    async layDanhSachLanhDao(forceRefresh = false) {
        if (!forceRefresh && cachedTeam) {
            return cachedTeam;
        }
        const response = await apiService.get('/customer/gioi-thieu/team');
        if (response.data) cachedTeam = response.data;
        return response.data;
    },
    async layDanhSachThuongHieu(forceRefresh = false) {
        if (!forceRefresh && cachedBrands) {
            return cachedBrands;
        }
        const response = await apiService.get('/customer/gioi-thieu/brands');
        if (response.data) cachedBrands = response.data;
        return response.data;
    }
};
