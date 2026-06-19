import api from '../apiService';

export const dichVuCart = {
    async syncCart(items) {
        const payload = {
            items: items.map(i => ({
                idChiTietSanPham: i.idChiTietSanPham,
                soLuong: i.soLuong
            }))
        };
        const response = await api.post('/customer/cart/sync', payload);
        return response.data?.data;
    }
};
