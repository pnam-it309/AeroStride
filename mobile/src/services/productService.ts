/**
 * Product Service - matches BE CustomerSanPhamController + CustomerLandingController
 */

import apiClient, { ApiResponse, PageResponse } from './apiClient';
import { API_PATHS } from '@/config/api';

// Matches BE CustomerProductResponse
export interface Product {
  id: string;
  maSanPham: string;
  tenSanPham: string;
  idDanhMuc: string;
  tenDanhMuc: string;
  idThuongHieu: string;
  tenThuongHieu: string;
  tenXuatXu: string;
  tenMucDichChay: string;
  tenCoGiay: string;
  tenChatLieu: string;
  tenDeGiay: string;
  gioiTinhKhachHang: string;
  moTaNgan: string;
  hinhAnh: string;
  trangThai: string;
  ngayTao: number;
  tongBienThe: number;
  tongSoLuongTon: number;
  giaBanThapNhat: number;
  giaBanCaoNhat: number;
}

// Matches BE CustomerProductVariantImageResponse
export interface VariantImage {
  id: string;
  duongDanAnh: string;
  moTa?: string;
  hinhAnhDaiDien?: boolean;
  trangThai?: string;
  ngayTao?: number;
  ngayCapNhat?: number;
  url?: string;
  isMain?: boolean;
}

// Matches BE CustomerProductVariantResponse
export interface ProductVariant {
  id: string;
  idSanPham: string;
  maSanPham: string;
  tenSanPham: string;
  tenSanPhamDayDu: string;
  tenThuongHieu: string;
  tenChatLieu: string;
  maChiTietSanPham: string;
  idMauSac: string;
  tenMauSac: string;
  maMauHex: string;
  idKichThuoc: string;
  tenKichThuoc: string;
  giaTriKichThuoc: string;
  soLuong: number;
  giaNhap: number;
  giaBan: number;
  phanTramGiam: number;
  trangThai: string;
  ngayTao: number;
  images: VariantImage[];
}

// Matches BE CustomerProductDetailResponse
export interface ProductDetail {
  id: string;
  maSanPham: string;
  tenSanPham: string;
  tenDanhMuc: string;
  tenThuongHieu: string;
  tenXuatXu: string;
  tenMucDichChay: string;
  tenCoGiay: string;
  tenChatLieu: string;
  tenDeGiay: string;
  gioiTinhKhachHang: string;
  moTaNgan: string;
  moTaChiTiet: string;
  hinhAnh: string;
  trangThai: string;
  ngayTao: number;
  variants: ProductVariant[];
}

// Matches BE CustomerProductFormOptionsResponse
export interface FilterOption {
  id: string;
  ma?: string;
  ten: string;
  moTa?: string | null;
}

export interface ProductFilters {
  thuongHieus: FilterOption[];
  danhMucs: FilterOption[];
  mauSacs: FilterOption[];
  kichThuocs: FilterOption[];
  chatLieus: FilterOption[];
  deGiays: FilterOption[];
  coGiays: FilterOption[];
  xuatXus: FilterOption[];
  mucDichChays: FilterOption[];
  gioiTinhKhachHangs?: string[];
  trangThais?: string[];
  maxPrice?: number;
}

// Matches BE CustomerLandingProductResponse
export interface LandingProduct {
  id: string;
  tenSanPham: string;
  tenDanhMuc: string;
  tenThuongHieu: string;
  moTaNgan: string;
  hinhAnh: string;
  giaBanThapNhat: number;
  giaBanCaoNhat: number;
}

export interface ProductSearchParams {
  page?: number;
  size?: number;
  keyword?: string;
  danhMucId?: string;
  thuongHieuId?: string;
  gioiTinhKhachHang?: string;
  xuatXuId?: string;
  mucDichChayId?: string;
  chatLieuId?: string;
  idDanhMuc?: string;
  idThuongHieu?: string;
  idMauSac?: string;
  idKichThuoc?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  sortDir?: string;
  sortDirection?: string;
}

const normalizeSortBy = (sortBy?: string): string => {
  if (sortBy === 'tenSanPham') return 'ten';
  if (sortBy === 'giaBanThapNhat' || sortBy === 'giaBanCaoNhat') return 'ngayTao';
  return sortBy || 'ngayTao';
};

const compactParams = (params: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );

const toBackendSearchParams = (params: ProductSearchParams = {}) =>
  compactParams({
    page: params.page ?? 0,
    size: params.size ?? 12,
    keyword: params.keyword,
    sortBy: normalizeSortBy(params.sortBy),
    sortDirection: params.sortDirection ?? params.sortDir ?? 'desc',
    danhMucId: params.danhMucId ?? params.idDanhMuc,
    thuongHieuId: params.thuongHieuId ?? params.idThuongHieu,
    gioiTinhKhachHang: params.gioiTinhKhachHang,
    xuatXuId: params.xuatXuId,
    mucDichChayId: params.mucDichChayId,
    chatLieuId: params.chatLieuId,
  });

export const productService = {
  async getProducts(
    params: ProductSearchParams = {}
  ): Promise<PageResponse<Product>> {
    const response = await apiClient.get<ApiResponse<PageResponse<Product>>>(
      API_PATHS.CUSTOMER.PRODUCTS,
      { params: toBackendSearchParams(params) }
    );
    return response.data.data;
  },

  async getFilters(): Promise<ProductFilters> {
    const response = await apiClient.get<ApiResponse<ProductFilters>>(
      API_PATHS.CUSTOMER.PRODUCT_FILTERS
    );
    return response.data.data;
  },

  async getProductDetail(id: string): Promise<ProductDetail> {
    const response = await apiClient.get<ApiResponse<ProductDetail>>(
      API_PATHS.CUSTOMER.PRODUCT_DETAIL(id)
    );
    return response.data.data;
  },

  async getLandingProducts(size: number = 6): Promise<LandingProduct[]> {
    const response = await apiClient.get<ApiResponse<LandingProduct[]>>(
      API_PATHS.CUSTOMER.LANDING_PRODUCTS,
      { params: { size } }
    );
    return response.data.data;
  },
};
