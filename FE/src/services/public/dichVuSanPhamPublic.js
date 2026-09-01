import api from '../apiService';
import { dichVuFile } from '@/services/core/dichVuFile';

const isAbsoluteUrl = (v) => /^(https?:)?\/\//i.test(v) || v?.startsWith('data:') || v?.startsWith('blob:');
const resolveImg = (v) => {
    if (!v) return '';
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

let cachedFilters = null;
const productDetailCache = new Map();

export const dichVuSanPhamPublic = {
    async layDanhSachSanPham(params) {
        const response = await api.get('/customer/san-pham/hien-thi', { params });
        return response.data?.data ?? response.data ?? {};
    },

    async layBoLoc(forceRefresh = false) {
        if (!forceRefresh && cachedFilters) {
            return cachedFilters;
        }
        const response = await api.get('/customer/san-pham/filters');
        const res = response.data?.data ?? response.data ?? {};
        if (res && Object.keys(res).length > 0) {
            cachedFilters = res;
        }
        return res;
    },

    async layChiTietSanPham(id, forceRefresh = false) {
        if (!forceRefresh && productDetailCache.has(id)) {
            return productDetailCache.get(id);
        }
        const response = await api.get(`/customer/san-pham/detail/${id}`);
        const res = response.data?.data ?? response.data ?? {};
        if (res && res.id) {
            productDetailCache.set(id, res);
        }
        return res;
    },

    xoaCache() {
        cachedFilters = null;
        productDetailCache.clear();
    },

    async timKiemNhanh(keyword, size = 6) {
        const response = await api.get('/customer/san-pham/hien-thi', {
            params: { keyword, page: 1, size }
        });
        const resData = response.data?.data ?? response.data;
        const content = resData?.content || [];
        return content.map((p) => ({
            ...p,
            hinhAnh: resolveImg(p.hinhAnh)
        }));
    },

    async layGoiYQuiz(answers) {
        const response = await api.post('/customer/san-pham/recommend-quiz', { answers });
        return response.data?.data ?? response.data ?? {};
    }
};
