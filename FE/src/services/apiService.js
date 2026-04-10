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

// Request interceptor to add auth token and start progress bar
api.interceptors.request.use(
  (config) => {
    // Start progress bar
    try {
        const loaderStore = useLoaderStore();
        loaderStore.startProgress();
    } catch (e) {
        console.warn('LoaderStore not ready yet');
    }

    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    try {
        const loaderStore = useLoaderStore();
        loaderStore.stopProgress();
    } catch (e) {}
    return Promise.reject(error);
  }
);

// Response interceptor to handle common errors and stop progress bar
api.interceptors.response.use(
  (response) => {
    try {
        const loaderStore = useLoaderStore();
        loaderStore.stopProgress();
    } catch (e) {}
    return response;
  },
  async (error) => {
    try {
        const loaderStore = useLoaderStore();
        loaderStore.stopProgress();
    } catch (e) {}

    if (error.response?.status === 401) {
      const isLoginRequest = error.config.url.includes('/auth/login');
      if (!isLoginRequest) {
        // Token expired or invalid, redirect to login
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/auth/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
