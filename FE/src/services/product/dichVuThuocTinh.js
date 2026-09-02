import api from '../apiService';
import { API_THUOC_TINH } from '@/constants/apiPaths';

// In-memory cache for static attribute options
const attributeCache = new Map();

const getWithCache = async (url, params) => {
    // Cache các request lấy danh sách thuộc tính không phân trang hoặc có size lớn
    const isGenericOptionRequest = !params || (!params.keyword && !params.ten && (!params.page || params.size >= 100));
    const cacheKey = isGenericOptionRequest ? `${url}?generic` : `${url}?${JSON.stringify(params || {})}`;

    if (attributeCache.has(cacheKey)) {
        return attributeCache.get(cacheKey);
    }

    const response = await api.get(url, { params });
    const data = response.data?.data ?? response.data;

    if (data) {
        attributeCache.set(cacheKey, data);
    }
    return data;
};

const invalidateCache = (url) => {
    for (const key of attributeCache.keys()) {
        if (key.startsWith(url)) {
            attributeCache.delete(key);
        }
    }
};

// Service thương hiệu
export const dichVuThuongHieu = {
    async layThuongHieu(params) {
        return getWithCache(API_THUOC_TINH.THUONG_HIEU, params);
    },
    async taoThuongHieu(data) {
        invalidateCache(API_THUOC_TINH.THUONG_HIEU);
        const response = await api.post(`${API_THUOC_TINH.THUONG_HIEU}/add`, data);
        return response.data.data;
    },
    async capNhatThuongHieu(id, data) {
        invalidateCache(API_THUOC_TINH.THUONG_HIEU);
        const response = await api.put(`${API_THUOC_TINH.THUONG_HIEU}/${id}`, data);
        return response.data.data;
    },
    async xoaThuongHieu(id) {
        invalidateCache(API_THUOC_TINH.THUONG_HIEU);
        const response = await api.delete(`${API_THUOC_TINH.THUONG_HIEU}/${id}`);
        return response.data;
    }
};

// Service màu sắc
export const dichVuMauSac = {
    async layMauSac(params) {
        return getWithCache(API_THUOC_TINH.MAU_SAC, params);
    },
    async taoMauSac(data) {
        invalidateCache(API_THUOC_TINH.MAU_SAC);
        const response = await api.post(`${API_THUOC_TINH.MAU_SAC}/add`, data);
        return response.data.data;
    },
    async capNhatMauSac(id, data) {
        invalidateCache(API_THUOC_TINH.MAU_SAC);
        const response = await api.put(`${API_THUOC_TINH.MAU_SAC}/${id}`, data);
        return response.data.data;
    },
    async xoaMauSac(id) {
        invalidateCache(API_THUOC_TINH.MAU_SAC);
        const response = await api.delete(`${API_THUOC_TINH.MAU_SAC}/${id}`);
        return response.data;
    }
};

// Service kích thước
export const dichVuKichThuoc = {
    async layKichThuoc(params) {
        return getWithCache(API_THUOC_TINH.KICH_THUOC, params);
    },
    async taoKichThuoc(data) {
        invalidateCache(API_THUOC_TINH.KICH_THUOC);
        const response = await api.post(`${API_THUOC_TINH.KICH_THUOC}/add`, data);
        return response.data.data;
    },
    async capNhatKichThuoc(id, data) {
        invalidateCache(API_THUOC_TINH.KICH_THUOC);
        const response = await api.put(`${API_THUOC_TINH.KICH_THUOC}/${id}`, data);
        return response.data.data;
    },
    async xoaKichThuoc(id) {
        invalidateCache(API_THUOC_TINH.KICH_THUOC);
        const response = await api.delete(`${API_THUOC_TINH.KICH_THUOC}/${id}`);
        return response.data;
    }
};

// Service chất liệu
export const dichVuChatLieu = {
    async layChatLieu(params) {
        return getWithCache(API_THUOC_TINH.CHAT_LIEU, params);
    },
    async taoChatLieu(data) {
        invalidateCache(API_THUOC_TINH.CHAT_LIEU);
        const response = await api.post(`${API_THUOC_TINH.CHAT_LIEU}/add`, data);
        return response.data.data;
    },
    async capNhatChatLieu(id, data) {
        invalidateCache(API_THUOC_TINH.CHAT_LIEU);
        const response = await api.put(`${API_THUOC_TINH.CHAT_LIEU}/${id}`, data);
        return response.data.data;
    },
    async xoaChatLieu(id) {
        invalidateCache(API_THUOC_TINH.CHAT_LIEU);
        const response = await api.delete(`${API_THUOC_TINH.CHAT_LIEU}/${id}`);
        return response.data;
    }
};

// Service đế giày
export const dichVuDeGiay = {
    async layDeGiay(params) {
        return getWithCache(API_THUOC_TINH.DE_GIAY, params);
    },
    async taoDeGiay(data) {
        invalidateCache(API_THUOC_TINH.DE_GIAY);
        const response = await api.post(`${API_THUOC_TINH.DE_GIAY}/add`, data);
        return response.data.data;
    },
    async capNhatDeGiay(id, data) {
        invalidateCache(API_THUOC_TINH.DE_GIAY);
        const response = await api.put(`${API_THUOC_TINH.DE_GIAY}/${id}`, data);
        return response.data.data;
    },
    async xoaDeGiay(id) {
        invalidateCache(API_THUOC_TINH.DE_GIAY);
        const response = await api.delete(`${API_THUOC_TINH.DE_GIAY}/${id}`);
        return response.data;
    }
};

// Service cổ giày
export const dichVuCoGiay = {
    async layCoGiay(params) {
        return getWithCache(API_THUOC_TINH.CO_GIAY, params);
    },
    async taoCoGiay(data) {
        invalidateCache(API_THUOC_TINH.CO_GIAY);
        const response = await api.post(`${API_THUOC_TINH.CO_GIAY}/add`, data);
        return response.data.data;
    },
    async capNhatCoGiay(id, data) {
        invalidateCache(API_THUOC_TINH.CO_GIAY);
        const response = await api.put(`${API_THUOC_TINH.CO_GIAY}/${id}`, data);
        return response.data.data;
    },
    async xoaCoGiay(id) {
        invalidateCache(API_THUOC_TINH.CO_GIAY);
        const response = await api.delete(`${API_THUOC_TINH.CO_GIAY}/${id}`);
        return response.data;
    }
};

// Service xuất xứ
export const dichVuXuatXu = {
    async layXuatXu(params) {
        return getWithCache(API_THUOC_TINH.XUAT_XU, params);
    },
    async taoXuatXu(data) {
        invalidateCache(API_THUOC_TINH.XUAT_XU);
        const response = await api.post(`${API_THUOC_TINH.XUAT_XU}/add`, data);
        return response.data.data;
    },
    async capNhatXuatXu(id, data) {
        invalidateCache(API_THUOC_TINH.XUAT_XU);
        const response = await api.put(`${API_THUOC_TINH.XUAT_XU}/${id}`, data);
        return response.data.data;
    },
    async xoaXuatXu(id) {
        invalidateCache(API_THUOC_TINH.XUAT_XU);
        const response = await api.delete(`${API_THUOC_TINH.XUAT_XU}/${id}`);
        return response.data;
    }
};

// Service mục đích chạy
export const dichVuMucDichChay = {
    async layMucDichChay(params) {
        return getWithCache(API_THUOC_TINH.MUC_DICH_CHAY, params);
    },
    async taoMucDichChay(data) {
        invalidateCache(API_THUOC_TINH.MUC_DICH_CHAY);
        const response = await api.post(`${API_THUOC_TINH.MUC_DICH_CHAY}/add`, data);
        return response.data.data;
    },
    async capNhatMucDichChay(id, data) {
        invalidateCache(API_THUOC_TINH.MUC_DICH_CHAY);
        const response = await api.put(`${API_THUOC_TINH.MUC_DICH_CHAY}/${id}`, data);
        return response.data.data;
    },
    async xoaMucDichChay(id) {
        invalidateCache(API_THUOC_TINH.MUC_DICH_CHAY);
        const response = await api.delete(`${API_THUOC_TINH.MUC_DICH_CHAY}/${id}`);
        return response.data;
    }
};
