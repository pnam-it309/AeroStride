/**
 * Payment Service - VNPay URL creation
 */

import apiClient, { ApiResponse } from './apiClient';
import { API_PATHS } from '@/config/api';

export interface PaymentRequest {
  amount: number;
  orderId: string;
  orderInfo: string;
}

export interface PaymentResponse {
  paymentUrl: string;
  transactionId?: string;
  status: string;
}

export const paymentService = {
  async createPayment(data: PaymentRequest): Promise<PaymentResponse> {
    const response = await apiClient.post<ApiResponse<PaymentResponse>>(
      API_PATHS.PAYMENT.CREATE,
      data
    );
    return response.data.data;
  },
};
