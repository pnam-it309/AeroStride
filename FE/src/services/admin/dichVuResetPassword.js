import api from '../apiService';
import { API_COMMON } from '@/constants/apiPaths';

export const dichVuResetPassword = {
    async getPendingRequests() {
        const res = await api.get(`${API_COMMON.RESET_PASSWORD}/pending`);
        return res.data.data;
    },
    async approveReset(id) {
        const res = await api.post(`${API_COMMON.RESET_PASSWORD}/approve/${id}`);
        return res.data;
    }
};
