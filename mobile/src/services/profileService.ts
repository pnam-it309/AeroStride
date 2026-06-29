/**
 * Profile Service - matches BE CustomerProfileController endpoints
 */

import apiClient, { ApiResponse } from './apiClient';
import { API_PATHS } from '@/config/api';

// Matches BE CustomerProfileResponse
export interface CustomerProfile {
  id: string;
  tenTaiKhoan: string;
  email: string;
  ten: string;
  sdt: string;
  diaChiChiTiet?: string;
  phuongXa?: string;
  quanHuyen?: string;
  tinhThanh?: string;
  hinhAnh?: string;
}

// Matches BE CustomerUpdateProfileRequest
export interface UpdateProfileRequest {
  ten: string;
  sdt: string;
  hinhAnh?: string;
}

// Matches BE CustomerChangePasswordRequest
export interface ChangePasswordRequest {
  matKhauCu: string;
  matKhauMoi: string;
  xacNhanMatKhau: string;
}

export const profileService = {
  async getMyProfile(): Promise<CustomerProfile> {
    const response = await apiClient.get<ApiResponse<CustomerProfile>>(
      API_PATHS.PROFILE.ME
    );
    return response.data.data;
  },

  async updateProfile(data: UpdateProfileRequest): Promise<string> {
    const response = await apiClient.put<ApiResponse<string>>(
      API_PATHS.PROFILE.UPDATE,
      data
    );
    return response.data.data;
  },

  async changePassword(data: ChangePasswordRequest): Promise<string> {
    const response = await apiClient.put<ApiResponse<string>>(
      API_PATHS.PROFILE.CHANGE_PASSWORD,
      data
    );
    return response.data.data;
  },
};
