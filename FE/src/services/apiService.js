import axios from 'axios';
import { useUIStore } from '@/stores/ui';
import { API_DEFAULTS } from '@/constants/apiPaths';

let API_BASE_URL = import.meta.env.VITE_API_URL || API_DEFAULTS.PREFIX;

// Remove trailing slash if present
if (API_BASE_URL && API_BASE_URL.endsWith('/')) {
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
        
        // Bỏ qua thanh loading/progress bar cho các yêu cầu chat để đảm bảo trải nghiệm real-time mượt mà
        const isChat = config.url && config.url.includes('/chat');
        
        if (config.silent || isChat) {
            // Không hiển thị loading/progress bar
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
    const uiStore = useUIStore();
    const isChat = error.config?.url && error.config.url.includes('/chat');
    if (!error.config?.silent && !isChat) {
        uiStore.stopProgress();
        uiStore.hideLoading();
    }
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

    if (error.response) {
      const status = error.response.status;
      const isLoginRequest = error.config.url.includes('/auth/login');

      if (status === 401 && !isLoginRequest) {
        sessionStorage.clear(); // Xóa sạch để đảm bảo an toàn
        
        const currentPath = window.location.pathname;
        const publicPaths = ['/shoes', '/product', '/user', '/auth'];
        const isPublicPath = currentPath === '/' || publicPaths.some(path => currentPath.startsWith(path));
        const isAdminPath = currentPath.startsWith('/admin') || !isPublicPath;
        
        // Chỉ tự động nhảy về Login nếu đang ở trong vùng quản trị
        // Nếu ở Landing Page hoặc các trang public, ta cứ ở lại đó
        if (isAdminPath) {
          window.location.href = '/admin/login';
        } else if (currentPath === '/user/login') {
          // Đã ở trang login rồi thì thôi
        } else if (currentPath.startsWith('/auth')) {
          // Các trang auth khác
        }
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
