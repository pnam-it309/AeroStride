/**
 * Order Service - matches BE CustomerOrderController endpoints
 */

import apiClient, { ApiResponse } from './apiClient';
import { API_PATHS } from '@/config/api';

// Matches BE CustomerOrderResponse.OrderItemResponse
export interface OrderItem {
  id: string;
  tenSanPham: string;
  hinhAnh: string;
  tenMauSac: string;
  tenKichThuoc: string;
  donGia: number;
  soLuong: number;
  thanhTien: number;
}

// Matches BE CustomerOrderResponse.OrderStatusHistory
export interface OrderStatusHistory {
  trangThai: string;
  trangThaiDisplay: string;
  thoiGian: number;
  ghiChu: string;
}

// Matches BE CustomerOrderResponse
export interface Order {
  id: string;
  maHoaDon: string;
  trangThai: string;
  trangThaiDisplay: string;
  tongTien: number;
  phiVanChuyen: number;
  tongTienSauGiam: number;
  tienGiam: number;
  diaChiNguoiNhan: string;
  soDienThoaiNguoiNhan: string;
  tenNguoiNhan: string;
  ghiChu: string;
  loaiDon: string;
  phuongThucThanhToan: string;
  ngayTao: number;
  ngayCapNhat: number;
  ngayDuKienNhan: number;
  tenVoucher: string;
  maVoucher: string;
  items: OrderItem[];
  lichSuTrangThai: OrderStatusHistory[];
}

// Matches BE CustomerOrderCheckoutRequest
export interface CheckoutRequest {
  items: {
    idChiTietSanPham: string;
    soLuong: number;
  }[];
  tenNguoiNhan: string;
  soDienThoai: string;
  diaChi: string;
  tinhThanh?: string;
  quanHuyen?: string;
  phuongXa?: string;
  idPhieuGiamGia?: string;
  phuongThucThanhToan: string;
  ghiChu?: string;
}

export interface Voucher {
  id: string;
  ma: string;
  ten: string;
  loaiPhieu: string;
  hinhThuc: string;
  phanTramGiamGia?: number;
  soTienGiam?: number;
  ngayBatDau: number;
  ngayKetThuc: number;
  soLuong: number;
  donHangToiThieu?: number;
  giamToiDa?: number;
  trangThai: string;
}

export const orderService = {
  async checkout(data: CheckoutRequest): Promise<Order> {
    const response = await apiClient.post<ApiResponse<Order>>(
      API_PATHS.ORDER.CHECKOUT,
      data
    );
    return response.data.data;
  },

  async getMyOrders(trangThai?: string): Promise<Order[]> {
    const response = await apiClient.get<ApiResponse<Order[]>>(
      API_PATHS.ORDER.MY_ORDERS,
      { params: trangThai ? { trangThai } : {} }
    );
    return response.data.data;
  },

  async getOrderDetail(id: string): Promise<Order> {
    const response = await apiClient.get<ApiResponse<Order>>(
      API_PATHS.ORDER.DETAIL(id)
    );
    return response.data.data;
  },

  async cancelOrder(id: string): Promise<void> {
    await apiClient.put(API_PATHS.ORDER.CANCEL(id));
  },

  async getAvailableVouchers(tongTien?: number): Promise<Voucher[]> {
    const response = await apiClient.get<ApiResponse<Voucher[]>>(
      API_PATHS.ORDER.VOUCHERS,
      { params: tongTien ? { tongTien } : {} }
    );
    return response.data.data;
  },
};
