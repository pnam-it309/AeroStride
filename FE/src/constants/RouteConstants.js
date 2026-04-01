/**
 * RouteConstants.js
 * Centralized route paths for the application to ensure consistency and easy management.
 */

export const ROUTES = {
  HOME: '/',
  AUTH: {
    BASE: '/auth',
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
  },
  ADMIN: {
    BASE: '/admin',
    DASHBOARD: '/admin/thong-ke',
    PRODUCTS: '/admin/quan-ly-san-pham',
    ATTRIBUTES: {
      BASE: '/admin/quan-ly-thuoc-tinh',
      DANH_MUC: '/admin/quan-ly-thuoc-tinh/danh-muc',
      THUONG_HIEU: '/admin/quan-ly-thuoc-tinh/thuong-hieu',
      MAU_SAC: '/admin/quan-ly-thuoc-tinh/mau-sac',
      KICH_THUOC: '/admin/quan-ly-thuoc-tinh/kich-thuoc',
      DE_GIAY: '/admin/quan-ly-thuoc-tinh/de-giay',
      CHAT_LIEU: '/admin/quan-ly-thuoc-tinh/chat-lieu',
      CO_GIAY: '/admin/quan-ly-thuoc-tinh/co-giay',
    },
    ORDERS: '/admin/quan-ly-hoa-don',
    CUSTOMERS: '/admin/quan-ly-khach-hang',
    EMPLOYEES: '/admin/quan-ly-nhan-vien',
    PROMOTIONS: '/admin/quan-ly-dot-giam-gia',
    VOUCHERS: '/admin/quan-ly-phieu-giam-gia',
    PAYMENT_RESULT: '/admin/ket-qua-thanh-toan',
    EXAMPLE: '/admin/vi-du',
    ERRORS: {
      E403: '/admin/error/403',
      E404: '/admin/error/404',
      E500: '/admin/error/500',
    },
  },
}

export default ROUTES
