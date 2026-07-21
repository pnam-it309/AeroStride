import api from '../apiService';

export const dichVuVoucher = {
    async layDanhSachVoucher() {
        const response = await api.get('/customer/phieu-giam-gia/hien-thi');
        return response.data.data;
    }
};
