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
    name: 'Bán Hàng',
    icon: 'Store',
    path: ROUTES.ADMIN.POS,
    description: 'Bán hàng tại quầy',
  },
  {
    name: 'Hóa đơn',
    icon: 'ShoppingBag',
    path: ROUTES.ADMIN.ORDERS,
    description: 'Quản lý hóa đơn',
  },
  {
    name: 'Sản Phẩm',
    icon: 'Box',
    path: ROUTES.ADMIN.PRODUCTS,
    description: 'Quản lý sản phẩm',
  },
  {
    name: 'Thuộc Tính',
    icon: 'Layers',
    path: ROUTES.ADMIN.ATTRIBUTES.BASE,
    description: 'Quản lý thuộc tính sản phẩm',
    children: [
      { name: 'Danh mục', path: ROUTES.ADMIN.ATTRIBUTES.DANH_MUC },
      { name: 'Thương hiệu', path: ROUTES.ADMIN.ATTRIBUTES.THUONG_HIEU },
      { name: 'Màu sắc', path: ROUTES.ADMIN.ATTRIBUTES.MAU_SAC },
      { name: 'Kích thước', path: ROUTES.ADMIN.ATTRIBUTES.KICH_THUOC },
      { name: 'Đế giày', path: ROUTES.ADMIN.ATTRIBUTES.DE_GIAY },
      { name: 'Chất liệu', path: ROUTES.ADMIN.ATTRIBUTES.CHAT_LIEU },
      { name: 'Cở giày', path: ROUTES.ADMIN.ATTRIBUTES.CO_GIAY },
    ],
  },
  {
    name: 'Giảm Giá',
    icon: 'Tag',
    path: ROUTES.ADMIN.PROMOTIONS,
    description: 'Quản lý đợt giảm giá',
  },
  {
    name: 'Phiếu Giảm Giá',
    icon: 'Ticket',
    path: ROUTES.ADMIN.VOUCHERS,
    description: 'Quản lý voucher',
  },
  {
    name: 'Khách Hàng',
    icon: 'Users',
    path: ROUTES.ADMIN.CUSTOMERS,
    description: 'Quản lý khách hàng',
  },
  {
    name: 'Nhân Viên',
    icon: 'UserRoundCog',
    path: ROUTES.ADMIN.EMPLOYEES,
    description: 'Quản lý nhân viên',
  },
]

export default {
  ADMIN_NAV_ITEMS,
}
