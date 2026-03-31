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
    DASHBOARD: '/admin/dashboard',
    PRODUCTS: '/admin/products',
    ORDERS: '/admin/orders',
    CUSTOMERS: '/admin/customers',
    PAYMENT_RESULT: '/admin/payment-result',
    EXAMPLE: '/admin/example',
    ERRORS: {
      E403: '/admin/error/403',
      E404: '/admin/error/404',
      E500: '/admin/error/500',
    },
  },
}

export default ROUTES
