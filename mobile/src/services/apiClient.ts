/**
 * Axios API Client for AeroStride Mobile
 * Mirrors FE/src/services/apiService.js patterns
 */

import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { API_CONFIG, API_PATHS } from '@/config/api';
import { storage } from '@/utils/storage';

const apiClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach JWT token
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = await storage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle 401 and errors
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      const isLoginRequest = originalRequest.url?.includes('/auth/login');
      if (isLoginRequest) {
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      try {
        const refreshToken = await storage.getRefreshToken();
        if (!refreshToken) {
          await storage.clearAuth();
          return Promise.reject(error);
        }

        const response = await axios.post(
          `${API_CONFIG.BASE_URL}${API_PATHS.AUTH.REFRESH}`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        await storage.setAccessToken(accessToken);
        if (newRefreshToken) {
          await storage.setRefreshToken(newRefreshToken);
        }

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        await storage.clearAuth();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// Type for BE ApiResponse<T>
export interface ApiResponse<T> {
  success: boolean;
  status: number;
  message: string;
  data: T;
  code?: string;
  error?: string;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
  last: boolean;
  currentPage?: number;
}

export function getApiErrorMessage(error: any, fallback = 'Đã có lỗi xảy ra. Vui lòng thử lại.'): string {
  if (!error) return fallback;
  if (typeof error === 'string') return error;
  if (error.response?.data?.message) return error.response.data.message;
  if (error.response?.data?.error) return error.response.data.error;
  if (error.message === 'Network Error') return 'Lỗi kết nối mạng. Vui lòng kiểm tra internet.';
  if (error.code === 'ECONNABORTED') return 'Quá thời gian kết nối server.';
  return error.message || fallback;
}

export default apiClient;
