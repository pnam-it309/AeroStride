import apiService from '../apiService';
import { API_ADMIN } from '@/constants/apiPaths';

let inFlightConfigPromise = null;

export const dichVuDanhGia = {
    getAll: async (params) => {
        const response = await apiService.get(API_ADMIN.DANH_GIA, { params });
        return response.data;
    },

    getConfig: async () => {
        if (inFlightConfigPromise) return inFlightConfigPromise;
        inFlightConfigPromise = apiService
            .get(`${API_ADMIN.DANH_GIA}/config`)
            .then((res) => res.data)
            .finally(() => {
                inFlightConfigPromise = null;
            });
        return inFlightConfigPromise;
    },

    updateConfig: async (autoApprove) => {
        const response = await apiService.put(`${API_ADMIN.DANH_GIA}/config`, { autoApprove });
        return response.data;
    },

    updateStatus: async (id, trangThai) => {
        const response = await apiService.put(`${API_ADMIN.DANH_GIA}/${id}/status`, null, {
            params: { trangThai }
        });
        return response.data;
    },

    delete: async (id) => {
        const response = await apiService.delete(`${API_ADMIN.DANH_GIA}/${id}`);
        return response.data;
    }
};
