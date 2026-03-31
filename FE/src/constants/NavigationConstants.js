import { ROUTES } from './RouteConstants'

/**
 * NavigationConstants.js
 * Centralized navigation configuration for the sidebar and other navigation components.
 */
export const ADMIN_NAV_ITEMS = [
  {
    name: 'Dashboard',
    icon: 'chart-pie-slice',
    path: ROUTES.ADMIN.DASHBOARD,
    description: 'Tổng quan hệ thống',
  },
  {
    name: 'Products',
    icon: 'package',
    path: ROUTES.ADMIN.PRODUCTS,
    description: 'Quản lý sản phẩm',
  },
  {
    name: 'Orders',
    icon: 'shopping-cart',
    path: ROUTES.ADMIN.ORDERS,
    description: 'Quản lý đơn hàng',
  },
  {
    name: 'Customers',
    icon: 'users',
    path: ROUTES.ADMIN.CUSTOMERS,
    description: 'Quản lý khách hàng',
  },
]

export default {
  ADMIN_NAV_ITEMS,
}
