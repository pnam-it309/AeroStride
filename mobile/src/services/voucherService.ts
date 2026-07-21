/**
 * Voucher Service - public voucher listing for mobile account/checkout surfaces
 */

import apiClient, { ApiResponse } from './apiClient';
import { API_PATHS } from '@/config/api';
import type { Voucher } from './orderService';

export const voucherService = {
  async getPublicVouchers(): Promise<Voucher[]> {
    const response = await apiClient.get<ApiResponse<Voucher[]>>(API_PATHS.VOUCHER.LIST);
    return response.data.data || [];
  },
};
