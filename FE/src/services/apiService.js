import axios from 'axios';
import { useUIStore } from '@/stores/ui';

let API_BASE_URL = import.meta.env.VITE_API_URL;

// Remove trailing slash if present
if (API_BASE_URL.endsWith('/')) {
  API_BASE_URL = API_BASE_URL.slice(0, -1);
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Phân loại Loader
api.interceptors.request.use(
  (config) => {
    try {
        const uiStore = useUIStore();
        
        // Mặc định mọi request đều dùng Progress Bar ở trên
        // Chỉ dùng Overlay khi flag 'bigOp' được bật hoặc là tác vụ hệ thống (đã được gọi thủ công)
        if (config.bigOp) {
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
    const uiStore = useUIStore();
    uiStore.stopProgress();
    uiStore.hideLoading();
    return Promise.reject(error);
  }
);

// Response interceptor: Tắt Loader và Xử lý lỗi tập trung
api.interceptors.response.use(
  (response) => {
    try {
        const uiStore = useUIStore();
        uiStore.stopProgress();
        uiStore.hideLoading();
    } catch (e) {}
    return response;
  },
  async (error) => {
    try {
        const uiStore = useUIStore();
        uiStore.stopProgress();
        uiStore.hideLoading();
    } catch (e) {}

    if (error.response) {
      const status = error.response.status;
      const isLoginRequest = error.config.url.includes('/auth/login');

      if (status === 401 && !isLoginRequest) {
        sessionStorage.clear(); // Xóa sạch để đảm bảo an toàn
        window.location.href = '/auth/login';
      } else if (status === 403) {
        if (import.meta.env.DEV) {
          console.error('Bạn không có quyền thực hiện hành động này');
        }
      } else if (status === 500) {
        if (import.meta.env.DEV) {
          console.error('Lỗi máy chủ (500). Vui lòng liên hệ Admin.');
        }
      }
    }

    return Promise.reject(error);
  }
);

export default api;
