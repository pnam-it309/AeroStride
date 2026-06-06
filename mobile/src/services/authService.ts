/**
 * Auth Service - matches BE AuthController endpoints
 */

import apiClient, { ApiResponse } from './apiClient';
import { API_PATHS } from '@/config/api';
import { storage } from '@/utils/storage';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  loginType?: string;
}

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>(
      API_PATHS.AUTH.LOGIN,
      data
    );
    const authData = response.data.data;

    // Store tokens and user info
    await storage.setAccessToken(authData.accessToken);
    await storage.setRefreshToken(authData.refreshToken);
    await storage.setUser({
      username: authData.username,
      role: authData.role,
    });

    return authData;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post(API_PATHS.AUTH.LOGOUT);
    } catch (error) {
      console.warn('Logout API error (ignored):', error);
    } finally {
      await storage.clearAuth();
    }
  },

  async refreshToken(): Promise<AuthResponse | null> {
    const refreshToken = await storage.getRefreshToken();
    if (!refreshToken) return null;

    const response = await apiClient.post<ApiResponse<AuthResponse>>(
      API_PATHS.AUTH.REFRESH,
      { refreshToken }
    );
    const authData = response.data.data;

    await storage.setAccessToken(authData.accessToken);
    if (authData.refreshToken) {
      await storage.setRefreshToken(authData.refreshToken);
    }

    return authData;
  },

  async getCurrentUser(): Promise<{ username: string; role: string } | null> {
    return storage.getUser();
  },

  async isAuthenticated(): Promise<boolean> {
    const token = await storage.getAccessToken();
    return !!token;
  },
};
