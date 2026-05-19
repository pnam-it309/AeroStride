import api from '../apiService';

export const dichVuSanPhamPublic = {
    async layDanhSachSanPham(params) {
        const response = await api.get('/customer/san-pham/hien-thi', { params });
        return response.data.data;
    },

    async layBoLoc() {
        const response = await api.get('/customer/san-pham/filters');
        return response.data.data;
    },

    async layChiTietSanPham(id) {
        const response = await api.get(`/customer/san-pham/detail/${id}`);
        return response.data.data;
    }
};
