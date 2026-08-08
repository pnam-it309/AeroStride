/**
 * Review Service for AeroStride Mobile
 */

import apiClient, { ApiResponse } from './apiClient';
import { API_PATHS } from '@/config/api';

export interface ReviewRequest {
  idHoaDon?: string;
  idSanPham: string;
  idKhachHang: string;
  diemDanhGia: number;
  noiDung: string;
  hinhAnh?: string;
  video?: string;
}

export interface ReviewResponse {
  id: string;
  diemDanhGia: number;
  noiDung: string;
  hinhAnh?: string;
  video?: string;
  trangThai: string;
  ngayTao: number;
  tenKhachHang: string;
  avatarKhachHang?: string;
}

export const reviewService = {
  async submitReview(data: ReviewRequest): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      API_PATHS.REVIEW.SUBMIT,
      data
    );
  },

  async getProductReviews(
    idSanPham: string,
    page = 0,
    size = 10
  ): Promise<{ content: ReviewResponse[]; totalElements: number }> {
    const response = await apiClient.get<ApiResponse<any>>(
      API_PATHS.REVIEW.PRODUCT_REVIEWS(idSanPham),
      { params: { page, size } }
    );
    const data = response.data?.data || response.data;
    return {
      content: data?.content || (Array.isArray(data) ? data : []),
      totalElements: data?.totalElements || (Array.isArray(data) ? data.length : 0),
    };
  },

  async checkEligibility(
    idHoaDon: string,
    idSanPham: string,
    idKhachHang: string
  ): Promise<boolean> {
    const response = await apiClient.get<ApiResponse<boolean>>(
      API_PATHS.REVIEW.CHECK_ELIGIBILITY,
      { params: { idHoaDon, idSanPham, idKhachHang } }
    );
    return !!response.data?.data;
  },
};
