import api from '../apiService';

export const dichVuResetPassword = {
    async getPendingRequests() {
        const res = await api.get('/reset-password-requests/pending');
        return res.data.data;
    },
    async approveReset(id) {
        const res = await api.post(`/reset-password-requests/approve/${id}`);
        return res.data;
    }
};
