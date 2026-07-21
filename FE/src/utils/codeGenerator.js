import api from '@/services/apiService';
import { API_COMMON } from '@/constants/apiPaths';

/**
 * Frontend utility for generating suggested codes using Backend API.
 */

/**
 * Generates a random code using backend.
 */
export const generateRandomCode = async (type) => {
    try {
        const response = await api.get(`${API_COMMON.CODE_GENERATE}?type=${type}`, { silent: true });
        return response.data;
    } catch (e) {
        console.error('Lỗi khi tạo mã từ BE', e);
        return '';
    }
};


