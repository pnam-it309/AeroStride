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
  // Raw WebSocket endpoint of the SockJS `/ws-chat` endpoint, used by @stomp/stompjs
  // directly (SockJS protocol itself is not React Native friendly).
  CHAT_WS_URL: __DEV__
    ? `ws://${DEV_HOST}:8080/ws-chat/websocket`
    : `${PROD_ORIGIN.replace(/^http/, 'ws')}/ws-chat/websocket`,
  TIMEOUT: 15000,
};

// API Endpoints - mirrors BE RoutesConstant.java
export const API_PATHS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh-token',
  },
  CHAT: {
    SEND: '/customer/chat/send',
    HISTORY: '/customer/chat/history',
    WELCOME_SUGGESTIONS: '/customer/chat/welcome-suggestions',
  },
  CUSTOMER: {
    PRODUCTS: '/customer/san-pham/hien-thi',
    PRODUCT_FILTERS: '/customer/san-pham/filters',
    PRODUCT_DETAIL: (id: string) => `/customer/san-pham/detail/${id}`,
    LANDING_PRODUCTS: '/customer/landing/products',
  },
  PROFILE: {
    ME: '/customer/profile/me',
    UPDATE: '/customer/profile/update',
    CHANGE_PASSWORD: '/customer/profile/change-password',
  },
  ORDER: {
    CHECKOUT: '/customer/order/checkout',
    MY_ORDERS: '/customer/order/my-orders',
    DETAIL: (id: string) => `/customer/order/${id}`,
    CANCEL: (id: string) => `/customer/order/${id}/cancel`,
    VOUCHERS: '/customer/order/vouchers',
    VNPAY_URL: (id: string) => `/customer/order/${id}/vnpay-url`,
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
