import { DEFAULT_PAGE_REQUEST } from '../types/pagination'
/**
 * AppConstants.js
 * Centralized application constants including API endpoints and UI mappings.
 */

export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH_TOKEN: '/auth/refresh',
    LOGOUT: '/auth/logout',
    ME: '/auth/me',
  },
  ADMIN: {},
  BATCH: {
    EXCEL_IMPORT: '/batch/import-excel',
  },
  PAYMENT: {
    VNPAY_CREATE: '/payment/vnpay/create',
    VNPAY_CALLBACK: '/payment/vnpay/callback',
  },
  FILE: {
    UPLOAD: '/files/upload',
    IMAGE_UPLOAD: '/files/upload/image',
  },
}

export const PAGINATION_DEFAULTS = {
  PAGE: DEFAULT_PAGE_REQUEST.page,
  LIMIT: DEFAULT_PAGE_REQUEST.size,
  LIMIT_OPTIONS: DEFAULT_PAGE_REQUEST.limitOptions,
}

export const ORDER_STATUS = {
  PENDING_PAYMENT: {
    text: 'Chờ thanh toán',
    color: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    icon: 'ph-clock-bold',
  },
  PROCESSING: {
    text: 'Đang chuẩn bị',
    color: 'bg-blue-100 text-blue-800 border-blue-200',
    icon: 'ph-spinner-bold',
  },
  SHIPPED: {
    text: 'Đang giao',
    color: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    icon: 'ph-truck-bold',
  },
  DELIVERED: {
    text: 'Đã giao',
    color: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    icon: 'ph-check-circle-bold',
  },
  CANCELLED: {
    text: 'Đã hủy',
    color: 'bg-red-100 text-red-800 border-red-200',
    icon: 'ph-x-circle-bold',
  },
  REFUNDED: {
    text: 'Đã hoàn tiền',
    color: 'bg-gray-100 text-gray-800 border-gray-200',
    icon: 'ph-arrow-counter-clockwise-bold',
  },
}

export default {
  API_ENDPOINTS,
  PAGINATION_DEFAULTS,
  ORDER_STATUS,
}
