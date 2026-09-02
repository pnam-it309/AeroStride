import axios from 'axios';
import { useUIStore } from '@/stores/ui';
import { API_DEFAULTS } from '@/constants/apiPaths';
import { logDevError, getBackendErrorMessage } from '@/utils/errorUtils';

let API_BASE_URL = import.meta.env.VITE_API_URL || API_DEFAULTS.PREFIX;

// Remove trailing slash if present
if (API_BASE_URL && API_BASE_URL.endsWith('/')) {
    API_BASE_URL = API_BASE_URL.slice(0, -1);
}

const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    }
});

// Request interceptor: Phân loại Loader
api.interceptors.request.use(
    (config) => {
        try {
            const uiStore = useUIStore();
            const method = (config.method || 'get').toLowerCase();
            const url = config.url || '';

            // Không hiển thị thanh bar trên đỉnh cho:
            // 1. Chat, POS, GHN, sinh mã code
            // 2. Các truy vấn GET dữ liệu bảng/tìm kiếm/lọc/phân trang/counts (vì AdminTable đã có skeleton & fetching bar cục bộ)
            const isSilentUrl = url.includes('/chat') ||
                url.includes('/admin/ban-hang') ||
                url.includes('/admin/ghn') ||
                url.includes('/common/code/generate');

            const isTableOrQueryGet = method === 'get' && (
                url.includes('/phan-trang') ||
                url.includes('/search') ||
                url.includes('/counts') ||
                url.includes('/form-options') ||
                url.includes('/max-price') ||
                url.includes('/options') ||
                url.includes('/thuoc-tinh') ||
                url.includes('/variants') ||
                url.includes('/thong-ke') ||
                url.includes('/dia-chi') ||
                url.includes('/hien-thi') ||
                url.includes('/san-pham') ||
                url.includes('/hoa-don') ||
                url.includes('/khach-hang') ||
                url.includes('/nhan-vien') ||
                url.includes('/dot-giam-gia') ||
                url.includes('/phieu-giam-gia')
            );

            if (config.silent || isSilentUrl || isTableOrQueryGet) {
                // Không kích hoạt progress bar toàn cục
            } else if (config.bigOp) {
                uiStore.showLoading(config.loadingMessage || 'Đang xử lý...');
            } else {
                uiStore.startProgress();
            }
        } catch (e) {
            if (import.meta.env.DEV) {
                console.warn('LoaderStore not ready');
            }
        }

        const token = sessionStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        try {
            const uiStore = useUIStore();
            uiStore.stopProgress();
            uiStore.hideLoading();
        } catch (e) {}
        return Promise.reject(error);
    }
);

// Response interceptor: Tắt Loader và Xử lý lỗi tập trung
api.interceptors.response.use(
    (response) => {
        try {
            const uiStore = useUIStore();
            const isChat = response.config?.url && response.config.url.includes('/chat');
            if (!response.config?.silent && !isChat) {
                uiStore.stopProgress();
                uiStore.hideLoading();
            }
        } catch (e) {}
        return response;
    },
    async (error) => {
        try {
            const uiStore = useUIStore();
            const isChat = error.config?.url && error.config.url.includes('/chat');
            if (!error.config?.silent && !isChat) {
                uiStore.stopProgress();
                uiStore.hideLoading();
            }
        } catch (e) {}

        // Ghi log chi tiết cho Developer kiểm tra
        logDevError('API Service', error);

        // Gắn thông điệp tiếng Việt thân thiện với người dùng vào object error
        error.userMessage = getBackendErrorMessage(error);

        if (error.response) {
            const status = error.response.status;
            const isLoginRequest = error.config?.url?.includes('/auth/login');

            if (status === 401 && !isLoginRequest) {
                sessionStorage.clear(); // Xóa sạch để đảm bảo an toàn

                const currentPath = window.location.pathname;
                const isAdminPath = currentPath.startsWith('/admin');
                const isCustomerAuthPath = ['/profile', '/my-orders', '/favorites'].some((path) => currentPath.startsWith(path));

                if (isAdminPath) {
                    window.location.href = '/admin/login';
                } else if (isCustomerAuthPath) {
                    window.location.href = '/user/login';
                }
            }
        }

        return Promise.reject(error);
    }
);

export default api;
