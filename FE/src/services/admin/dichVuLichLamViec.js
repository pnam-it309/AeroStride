import apiService from '@/services/apiService';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';

let cachedShifts = null;
let inFlightShifts = null;

export const dichVuCaLam = {
    // Lấy danh sách ca làm việc (có cache in-memory để chuyển đổi màn tức thì)
    async layDanhSachCaLam(forceRefresh = false) {
        if (!forceRefresh && cachedShifts) {
            return cachedShifts;
        }
        if (inFlightShifts) {
            return inFlightShifts;
        }
        inFlightShifts = (async () => {
            try {
                const response = await apiService.get(API_LICH_LAM_VIEC.SHIFTS);
                const data = response.data?.data || response.data || [];
                cachedShifts = Array.isArray(data) ? data : [];
                return cachedShifts;
            } finally {
                inFlightShifts = null;
            }
        })();
        return inFlightShifts;
    },

    invalidateCache() {
        cachedShifts = null;
    }
};
