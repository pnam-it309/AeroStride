import { ROUTES } from './RouteConstants'

/**
 * NavigationConstants.js
 * Centralized navigation configuration for the sidebar and other navigation components.
 */
export const ADMIN_NAV_ITEMS = [
  {
    name: 'Dashboard',
    icon: 'LayoutDashboard',
    path: ROUTES.ADMIN.DASHBOARD,
    description: 'Tổng quan hệ thống',
  },
  {
    name: 'Quản lý sản phẩm',
    icon: 'Box',
    path: ROUTES.ADMIN.PRODUCTS_BASE,
    description: 'Quản trị sản phẩm, biến thể và thuộc tính',
    children: [
      { name: 'Sản phẩm', path: ROUTES.ADMIN.PRODUCTS },
      { name: 'Biến thể SP', path: ROUTES.ADMIN.VARIANTS },
      {
        name: 'Thuộc tính sản phẩm',
        path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.BASE,
        children: [
          { name: 'Màu sắc', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLORS },
          { name: 'Kích thước', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SIZES },
          { name: 'Thương hiệu', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.BRANDS },
          { name: 'Xuất xứ', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.ORIGINS },
          { name: 'Mục đích chạy', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.RUNNING_PURPOSES },
          { name: 'Cổ giày', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLLAR_TYPES },
          { name: 'Chất liệu', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.MATERIALS },
          { name: 'Đế giày', path: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SOLE_TYPES },
        ],
      },
    ],
  },
  {
    name: 'Quản lý đợt giảm giá',
    icon: 'Tag',
    path: ROUTES.ADMIN.PROMOTIONS,
    description: 'Quản lý đợt giảm giá',
  },
  {
    name: 'Quản lý phiếu giảm giá',
    icon: 'Ticket',
    path: ROUTES.ADMIN.VOUCHERS,
    description: 'Quản lý voucher',
  },
  {
    name: 'Quản lý khách hàng',
    icon: 'Users',
    path: ROUTES.ADMIN.CUSTOMERS,
    description: 'Quản lý khách hàng',
  },
  {
    name: 'Quản lý nhân viên',
    icon: 'UserRoundCog',
    path: ROUTES.ADMIN.EMPLOYEES,
    description: 'Quản lý nhân viên',
  },
  {
    name: 'Quản lý hóa đơn',
    icon: 'ShoppingBag',
    path: ROUTES.ADMIN.ORDERS,
    description: 'Quản lý hóa đơn',
  },
  {
    name: 'Quản lý phương thức thanh toán',
    icon: 'WalletCards',
    path: ROUTES.ADMIN.PAYMENT_METHODS,
    description: 'Quản lý phương thức thanh toán',
  },
  {
    name: 'Quản lý phân quyền',
    icon: 'ShieldCheck',
    path: ROUTES.ADMIN.PERMISSIONS,
    description: 'Quản lý phân quyền',
  },
]

export default {
  ADMIN_NAV_ITEMS,
}
