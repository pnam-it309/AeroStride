import api from '../apiService';
import { dichVuFile } from '@/services/core/dichVuFile';

const isAbsoluteUrl = (v) => /^(https?:)?\/\//i.test(v) || v?.startsWith('data:') || v?.startsWith('blob:');
const resolveImg = (v) => {
    if (!v) return '';
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

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
    },

    async timKiemNhanh(keyword, size = 6) {
        const response = await api.get('/customer/san-pham/hien-thi', {
            params: { keyword, page: 1, size }
        });
        const content = response.data?.data?.content || [];
        return content.map((p) => ({
            ...p,
            hinhAnh: resolveImg(p.hinhAnh)
        }));
    }
};
