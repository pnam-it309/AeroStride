import axios from 'axios';
import { useLoaderStore } from '@/stores/loader';

let API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

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
        const loaderStore = useLoaderStore();
        const method = config.method.toLowerCase();
        
        if (['post', 'put', 'delete'].includes(method)) {
            // Tác vụ quan trọng -> Bật Full-page Loader
            loaderStore.showLoader();
        } else {
            // Tác vụ đọc dữ liệu -> Chỉ chạy Progress Bar
            loaderStore.startProgress();
        }
    } catch (e) {
        console.warn('LoaderStore not ready');
    }

    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    const loaderStore = useLoaderStore();
    loaderStore.stopProgress();
    loaderStore.hideLoader();
    return Promise.reject(error);
  }
);

// Response interceptor: Tắt Loader và Xử lý lỗi tập trung
api.interceptors.response.use(
  (response) => {
    try {
        const loaderStore = useLoaderStore();
        loaderStore.stopProgress();
        loaderStore.hideLoader();
    } catch (e) {}
    return response;
  },
  async (error) => {
    try {
        const loaderStore = useLoaderStore();
        loaderStore.stopProgress();
        loaderStore.hideLoader();
    } catch (e) {}

    if (error.response) {
      const status = error.response.status;
      const isLoginRequest = error.config.url.includes('/auth/login');

      if (status === 401 && !isLoginRequest) {
        localStorage.clear(); // Xóa sạch để đảm bảo an toàn
        window.location.href = '/auth/login';
      } else if (status === 403) {
        console.error('Bạn không có quyền thực hiện hành động này');
      } else if (status === 500) {
        console.error('Lỗi máy chủ (500). Vui lòng liên hệ Admin.');
      }
    }

    return Promise.reject(error);
  }
);

export default api;
