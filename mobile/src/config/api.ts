/**
 * API Configuration for AeroStride Mobile
 * Matches BE routes: /api/v1/*
 */

import { Platform } from 'react-native';

// Android emulator uses 10.0.2.2 to access host machine's localhost.
const DEV_HOST = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const DEV_ORIGIN = `http://${DEV_HOST}:8080`;
const PROD_ORIGIN = 'https://aerostride.example.com';

export const API_CONFIG = {
  ORIGIN: __DEV__ ? DEV_ORIGIN : PROD_ORIGIN,
  BASE_URL: __DEV__ ? `${DEV_ORIGIN}/api/v1` : `${PROD_ORIGIN}/api/v1`,
  WS_URL: __DEV__
    ? `ws://${DEV_HOST}:8080/ws`
    : `${PROD_ORIGIN.replace(/^http/, 'ws')}/ws`,
  TIMEOUT: 15000,
};

// API Endpoints - mirrors BE RoutesConstant.java
export const API_PATHS = {
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh-token',
  },
  CUSTOMER: {
    PRODUCTS: '/customer/san-pham/hien-thi',
    PRODUCT_FILTERS: '/customer/san-pham/filters',
    PRODUCT_DETAIL: (id: string) => `/customer/san-pham/detail/${id}`,
    LANDING_PRODUCTS: '/customer/landing/products',
  },
  ORDER: {
    CHECKOUT: '/customer/order/checkout',
    MY_ORDERS: '/customer/order/my-orders',
    DETAIL: (id: string) => `/customer/order/${id}`,
    CANCEL: (id: string) => `/customer/order/${id}/cancel`,
    VOUCHERS: '/customer/order/vouchers',
  },
  VOUCHER: {
    LIST: '/customer/phieu-giam-gia/hien-thi',
  },
  PAYMENT: {
    CREATE: '/payment/create',
  },
  STORAGE: {
    FILES: '/storage/files',
    PROXY: '/storage/proxy',
  },
} as const;
