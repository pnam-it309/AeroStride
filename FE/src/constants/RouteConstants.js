/**
 * RouteConstants.js
 * Centralized route paths for the application to ensure consistency and easy management.
 */

const PRODUCTS_BASE = '/admin/quan-ly-san-pham'
const PRODUCT_VARIANT_BASE = `${PRODUCTS_BASE}/bien-the-san-pham`
const PRODUCT_ATTRIBUTE_BASE = `${PRODUCTS_BASE}/thuoc-tinh-san-pham`

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

    PRODUCTS_BASE,
    PRODUCTS: `${PRODUCTS_BASE}/san-pham`,
    PRODUCTS_CREATE: `${PRODUCTS_BASE}/san-pham/them-moi`,
    PRODUCTS_DETAIL: `${PRODUCTS_BASE}/san-pham/:id`,
    PRODUCTS_EDIT: `${PRODUCTS_BASE}/san-pham/:id/chinh-sua`,

    VARIANTS_BASE: PRODUCT_VARIANT_BASE,
    VARIANTS: PRODUCT_VARIANT_BASE,
    VARIANTS_CREATE: `${PRODUCT_VARIANT_BASE}/them-moi`,
    VARIANTS_DETAIL: `${PRODUCT_VARIANT_BASE}/:id`,
    VARIANTS_EDIT: `${PRODUCT_VARIANT_BASE}/:id/chinh-sua`,

    PRODUCT_ATTRIBUTES: {
      BASE: PRODUCT_ATTRIBUTE_BASE,
      COLORS: `${PRODUCT_ATTRIBUTE_BASE}/mau-sac`,
      COLORS_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/mau-sac/them-moi`,
      COLORS_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/mau-sac/:id/chinh-sua`,

      SIZES: `${PRODUCT_ATTRIBUTE_BASE}/kich-thuoc`,
      SIZES_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/kich-thuoc/them-moi`,
      SIZES_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/kich-thuoc/:id/chinh-sua`,

      BRANDS: `${PRODUCT_ATTRIBUTE_BASE}/thuong-hieu`,
      BRANDS_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/thuong-hieu/them-moi`,
      BRANDS_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/thuong-hieu/:id/chinh-sua`,

      ORIGINS: `${PRODUCT_ATTRIBUTE_BASE}/xuat-xu`,
      ORIGINS_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/xuat-xu/them-moi`,
      ORIGINS_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/xuat-xu/:id/chinh-sua`,

      RUNNING_PURPOSES: `${PRODUCT_ATTRIBUTE_BASE}/muc-dich-chay`,
      RUNNING_PURPOSES_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/muc-dich-chay/them-moi`,
      RUNNING_PURPOSES_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/muc-dich-chay/:id/chinh-sua`,

      COLLAR_TYPES: `${PRODUCT_ATTRIBUTE_BASE}/co-giay`,
      COLLAR_TYPES_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/co-giay/them-moi`,
      COLLAR_TYPES_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/co-giay/:id/chinh-sua`,

      MATERIALS: `${PRODUCT_ATTRIBUTE_BASE}/chat-lieu`,
      MATERIALS_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/chat-lieu/them-moi`,
      MATERIALS_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/chat-lieu/:id/chinh-sua`,

      SOLE_TYPES: `${PRODUCT_ATTRIBUTE_BASE}/de-giay`,
      SOLE_TYPES_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/de-giay/them-moi`,
      SOLE_TYPES_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/de-giay/:id/chinh-sua`,

      CATEGORY: `${PRODUCT_ATTRIBUTE_BASE}/danh-muc`,
      CATEGORY_CREATE: `${PRODUCT_ATTRIBUTE_BASE}/danh-muc/them-moi`,
      CATEGORY_EDIT: `${PRODUCT_ATTRIBUTE_BASE}/danh-muc/:id/chinh-sua`,
    },

    ORDERS: '/admin/quan-ly-hoa-don',
    CUSTOMERS: '/admin/quan-ly-khach-hang',
    EMPLOYEES: '/admin/quan-ly-nhan-vien',
    PROMOTIONS: '/admin/quan-ly-dot-giam-gia',
    VOUCHERS: '/admin/quan-ly-phieu-giam-gia',
    PAYMENT_METHODS: '/admin/quan-ly-phuong-thuc-thanh-toan',
    PERMISSIONS: '/admin/quan-ly-phan-quyen',

    // Backward-compatible aliases used by existing code.
    PRODUCT_VARIANTS: PRODUCT_VARIANT_BASE,
    ATTRIBUTES: {
      BASE: PRODUCT_ATTRIBUTE_BASE,
      DANH_MUC: `${PRODUCT_ATTRIBUTE_BASE}/danh-muc`,
      THUONG_HIEU: `${PRODUCT_ATTRIBUTE_BASE}/thuong-hieu`,
      XUAT_XU: `${PRODUCT_ATTRIBUTE_BASE}/xuat-xu`,
      MUC_DICH_CHAY: `${PRODUCT_ATTRIBUTE_BASE}/muc-dich-chay`,
      MAU_SAC: `${PRODUCT_ATTRIBUTE_BASE}/mau-sac`,
      KICH_THUOC: `${PRODUCT_ATTRIBUTE_BASE}/kich-thuoc`,
      DE_GIAY: `${PRODUCT_ATTRIBUTE_BASE}/de-giay`,
      CHAT_LIEU: `${PRODUCT_ATTRIBUTE_BASE}/chat-lieu`,
      CO_GIAY: `${PRODUCT_ATTRIBUTE_BASE}/co-giay`,
    },

    ERRORS: {
      E403: '/admin/error/403',
      E404: '/admin/error/404',
      E500: '/admin/error/500',
    },
  },
}

export default ROUTES
