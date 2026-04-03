import { createRouter, createWebHistory } from "vue-router";
import setupRouteGuards from "./routeGuards";
import { ROUTES } from "../constants";

import AuthLayout from "../layouts/AuthLayout.vue";
import AdminLayout from "../layouts/AdminLayout.vue";

const buildBreadcrumbs = (moduleLabel, modulePath, entityLabel, entityPath, terminalLabel) => {
  const items = [{ label: moduleLabel, to: modulePath }]

  if (entityLabel) {
    items.push({ label: entityLabel, to: entityPath })
  }

  if (terminalLabel) {
    items.push({ label: terminalLabel })
  }

  return items
}

const productBreadcrumbs = (terminalLabel) =>
  buildBreadcrumbs('Quản lý sản phẩm', ROUTES.ADMIN.PRODUCTS_BASE, 'Sản phẩm', ROUTES.ADMIN.PRODUCTS, terminalLabel)

const variantBreadcrumbs = (terminalLabel) =>
  buildBreadcrumbs('Quản lý sản phẩm', ROUTES.ADMIN.PRODUCTS_BASE, 'Biến thể SP', ROUTES.ADMIN.VARIANTS, terminalLabel)

const attributeBreadcrumbs = (attributeLabel, attributePath, terminalLabel) => [
  { label: 'Quản lý sản phẩm', to: ROUTES.ADMIN.PRODUCTS_BASE },
  { label: 'Thuộc tính sản phẩm', to: ROUTES.ADMIN.PRODUCT_ATTRIBUTES.BASE },
  { label: attributeLabel, to: attributePath },
  { label: terminalLabel },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: ROUTES.HOME, redirect: ROUTES.AUTH.LOGIN },
    {
      path: ROUTES.AUTH.BASE,
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        {
          path: "login",
          name: "Login",
          component: () => import("../pages/Login.vue"),
        },
        {
          path: "register",
          name: "Register",
          component: () => import("../pages/Register.vue"),
        },
      ],
      redirect: ROUTES.AUTH.LOGIN,
    },
    {
      path: ROUTES.ADMIN.BASE,
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'thong-ke',
          name: 'AdminDashboard',
          component: () => import('../pages/admin/thongke/ThongKe.vue'),
          meta: {
            title: 'Thống kê',
            breadcrumbs: [{ label: 'Dashboard' }],
          },
        },
        {
          path: 'quan-ly-san-pham',
          redirect: { name: 'AdminProductsList' },
        },
        {
          path: 'quan-ly-san-pham/san-pham',
          name: 'AdminProductsList',
          component: () => import('../pages/admin/quanlisanpham/QuanLySanPham.vue'),
          meta: {
            title: 'Sản phẩm',
            breadcrumbs: productBreadcrumbs('Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/san-pham/them-moi',
          name: 'AdminProductsCreate',
          component: () => import('../pages/admin/quanlisanpham/QuanLySanPham.vue'),
          meta: {
            title: 'Thêm sản phẩm',
            breadcrumbs: productBreadcrumbs('Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/san-pham/:id',
          name: 'AdminProductsDetail',
          redirect: (to) => ({ name: 'AdminProductsEdit', params: { id: to.params.id } }),
        },
        {
          path: 'quan-ly-san-pham/san-pham/:id/chinh-sua',
          name: 'AdminProductsEdit',
          component: () => import('../pages/admin/quanlisanpham/QuanLySanPham.vue'),
          meta: {
            title: 'Cập nhật sản phẩm',
            breadcrumbs: productBreadcrumbs('Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/bien-the-san-pham',
          name: 'AdminVariantsList',
          component: () => import('../pages/admin/quanlisanpham/QuanLyBienTheSanPham.vue'),
          meta: {
            title: 'Biến thể SP',
            breadcrumbs: variantBreadcrumbs('Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/bien-the-san-pham/them-moi',
          name: 'AdminVariantsCreate',
          component: () => import('../pages/admin/quanlisanpham/QuanLyBienTheSanPham.vue'),
          meta: {
            title: 'Thêm biến thể SP',
            breadcrumbs: variantBreadcrumbs('Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/bien-the-san-pham/:id',
          name: 'AdminVariantsDetail',
          redirect: (to) => ({ name: 'AdminVariantsEdit', params: { id: to.params.id }, query: to.query }),
        },
        {
          path: 'quan-ly-san-pham/bien-the-san-pham/:id/chinh-sua',
          name: 'AdminVariantsEdit',
          component: () => import('../pages/admin/quanlisanpham/QuanLyBienTheSanPham.vue'),
          meta: {
            title: 'Cập nhật biến thể SP',
            breadcrumbs: variantBreadcrumbs('Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham',
          redirect: { name: 'AdminAttrColorsList' },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/mau-sac',
          name: 'AdminAttrColorsList',
          component: () => import('../pages/admin/quanlithuoctinh/MauSac.vue'),
          meta: {
            title: 'Màu sắc',
            breadcrumbs: attributeBreadcrumbs('Màu sắc', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLORS, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/mau-sac/them-moi',
          name: 'AdminAttrColorsCreate',
          component: () => import('../pages/admin/quanlithuoctinh/MauSac.vue'),
          meta: {
            title: 'Thêm màu sắc',
            breadcrumbs: attributeBreadcrumbs('Màu sắc', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLORS, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/mau-sac/:id/chinh-sua',
          name: 'AdminAttrColorsEdit',
          component: () => import('../pages/admin/quanlithuoctinh/MauSac.vue'),
          meta: {
            title: 'Cập nhật màu sắc',
            breadcrumbs: attributeBreadcrumbs('Màu sắc', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLORS, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/kich-thuoc',
          name: 'AdminAttrSizesList',
          component: () => import('../pages/admin/quanlithuoctinh/KichThuoc.vue'),
          meta: {
            title: 'Kích thước',
            breadcrumbs: attributeBreadcrumbs('Kích thước', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SIZES, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/kich-thuoc/them-moi',
          name: 'AdminAttrSizesCreate',
          component: () => import('../pages/admin/quanlithuoctinh/KichThuoc.vue'),
          meta: {
            title: 'Thêm kích thước',
            breadcrumbs: attributeBreadcrumbs('Kích thước', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SIZES, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/kich-thuoc/:id/chinh-sua',
          name: 'AdminAttrSizesEdit',
          component: () => import('../pages/admin/quanlithuoctinh/KichThuoc.vue'),
          meta: {
            title: 'Cập nhật kích thước',
            breadcrumbs: attributeBreadcrumbs('Kích thước', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SIZES, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/thuong-hieu',
          name: 'AdminAttrBrandsList',
          component: () => import('../pages/admin/quanlithuoctinh/ThuongHieu.vue'),
          meta: {
            title: 'Thương hiệu',
            breadcrumbs: attributeBreadcrumbs('Thương hiệu', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.BRANDS, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/thuong-hieu/them-moi',
          name: 'AdminAttrBrandsCreate',
          component: () => import('../pages/admin/quanlithuoctinh/ThuongHieu.vue'),
          meta: {
            title: 'Thêm thương hiệu',
            breadcrumbs: attributeBreadcrumbs('Thương hiệu', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.BRANDS, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/thuong-hieu/:id/chinh-sua',
          name: 'AdminAttrBrandsEdit',
          component: () => import('../pages/admin/quanlithuoctinh/ThuongHieu.vue'),
          meta: {
            title: 'Cập nhật thương hiệu',
            breadcrumbs: attributeBreadcrumbs('Thương hiệu', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.BRANDS, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/xuat-xu',
          name: 'AdminAttrOriginsList',
          component: () => import('../pages/admin/quanlithuoctinh/XuatXu.vue'),
          meta: {
            title: 'Xuất xứ',
            breadcrumbs: attributeBreadcrumbs('Xuất xứ', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.ORIGINS, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/xuat-xu/them-moi',
          name: 'AdminAttrOriginsCreate',
          component: () => import('../pages/admin/quanlithuoctinh/XuatXu.vue'),
          meta: {
            title: 'Thêm xuất xứ',
            breadcrumbs: attributeBreadcrumbs('Xuất xứ', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.ORIGINS, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/xuat-xu/:id/chinh-sua',
          name: 'AdminAttrOriginsEdit',
          component: () => import('../pages/admin/quanlithuoctinh/XuatXu.vue'),
          meta: {
            title: 'Cập nhật xuất xứ',
            breadcrumbs: attributeBreadcrumbs('Xuất xứ', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.ORIGINS, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/muc-dich-chay',
          name: 'AdminAttrRunningPurposesList',
          component: () => import('../pages/admin/quanlithuoctinh/MucDichChay.vue'),
          meta: {
            title: 'Mục đích chạy',
            breadcrumbs: attributeBreadcrumbs('Mục đích chạy', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.RUNNING_PURPOSES, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/muc-dich-chay/them-moi',
          name: 'AdminAttrRunningPurposesCreate',
          component: () => import('../pages/admin/quanlithuoctinh/MucDichChay.vue'),
          meta: {
            title: 'Thêm mục đích chạy',
            breadcrumbs: attributeBreadcrumbs('Mục đích chạy', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.RUNNING_PURPOSES, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/muc-dich-chay/:id/chinh-sua',
          name: 'AdminAttrRunningPurposesEdit',
          component: () => import('../pages/admin/quanlithuoctinh/MucDichChay.vue'),
          meta: {
            title: 'Cập nhật mục đích chạy',
            breadcrumbs: attributeBreadcrumbs('Mục đích chạy', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.RUNNING_PURPOSES, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/co-giay',
          name: 'AdminAttrCollarTypesList',
          component: () => import('../pages/admin/quanlithuoctinh/CoGiay.vue'),
          meta: {
            title: 'Cổ giày',
            breadcrumbs: attributeBreadcrumbs('Cổ giày', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLLAR_TYPES, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/co-giay/them-moi',
          name: 'AdminAttrCollarTypesCreate',
          component: () => import('../pages/admin/quanlithuoctinh/CoGiay.vue'),
          meta: {
            title: 'Thêm cổ giày',
            breadcrumbs: attributeBreadcrumbs('Cổ giày', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLLAR_TYPES, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/co-giay/:id/chinh-sua',
          name: 'AdminAttrCollarTypesEdit',
          component: () => import('../pages/admin/quanlithuoctinh/CoGiay.vue'),
          meta: {
            title: 'Cập nhật cổ giày',
            breadcrumbs: attributeBreadcrumbs('Cổ giày', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.COLLAR_TYPES, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/chat-lieu',
          name: 'AdminAttrMaterialsList',
          component: () => import('../pages/admin/quanlithuoctinh/ChatLieu.vue'),
          meta: {
            title: 'Chất liệu',
            breadcrumbs: attributeBreadcrumbs('Chất liệu', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.MATERIALS, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/chat-lieu/them-moi',
          name: 'AdminAttrMaterialsCreate',
          component: () => import('../pages/admin/quanlithuoctinh/ChatLieu.vue'),
          meta: {
            title: 'Thêm chất liệu',
            breadcrumbs: attributeBreadcrumbs('Chất liệu', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.MATERIALS, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/chat-lieu/:id/chinh-sua',
          name: 'AdminAttrMaterialsEdit',
          component: () => import('../pages/admin/quanlithuoctinh/ChatLieu.vue'),
          meta: {
            title: 'Cập nhật chất liệu',
            breadcrumbs: attributeBreadcrumbs('Chất liệu', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.MATERIALS, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/de-giay',
          name: 'AdminAttrSoleTypesList',
          component: () => import('../pages/admin/quanlithuoctinh/DeGiay.vue'),
          meta: {
            title: 'Đế giày',
            breadcrumbs: attributeBreadcrumbs('Đế giày', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SOLE_TYPES, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/de-giay/them-moi',
          name: 'AdminAttrSoleTypesCreate',
          component: () => import('../pages/admin/quanlithuoctinh/DeGiay.vue'),
          meta: {
            title: 'Thêm đế giày',
            breadcrumbs: attributeBreadcrumbs('Đế giày', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SOLE_TYPES, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/de-giay/:id/chinh-sua',
          name: 'AdminAttrSoleTypesEdit',
          component: () => import('../pages/admin/quanlithuoctinh/DeGiay.vue'),
          meta: {
            title: 'Cập nhật đế giày',
            breadcrumbs: attributeBreadcrumbs('Đế giày', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.SOLE_TYPES, 'Cập nhật'),
          },
        },

        // Hidden category routes remain available because product data still depends on category.
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/danh-muc',
          name: 'AdminAttrCategoryList',
          component: () => import('../pages/admin/quanlithuoctinh/DanhMuc.vue'),
          meta: {
            title: 'Danh mục',
            breadcrumbs: attributeBreadcrumbs('Danh mục', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.CATEGORY, 'Danh sách'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/danh-muc/them-moi',
          name: 'AdminAttrCategoryCreate',
          component: () => import('../pages/admin/quanlithuoctinh/DanhMuc.vue'),
          meta: {
            title: 'Thêm danh mục',
            breadcrumbs: attributeBreadcrumbs('Danh mục', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.CATEGORY, 'Thêm mới'),
          },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh-san-pham/danh-muc/:id/chinh-sua',
          name: 'AdminAttrCategoryEdit',
          component: () => import('../pages/admin/quanlithuoctinh/DanhMuc.vue'),
          meta: {
            title: 'Cập nhật danh mục',
            breadcrumbs: attributeBreadcrumbs('Danh mục', ROUTES.ADMIN.PRODUCT_ATTRIBUTES.CATEGORY, 'Cập nhật'),
          },
        },

        {
          path: 'quan-ly-hoa-don',
          name: 'AdminOrders',
          component: () => import('../pages/admin/quanlihoadon/QuanLyHoaDon.vue'),
          meta: { title: 'Quản lý hóa đơn', breadcrumbs: [{ label: 'Quản lý hóa đơn' }] },
        },
        {
          path: 'quan-ly-hoa-don/chi-tiet/:id',
          name: 'AdminOrderDetail',
          component: () => import('../pages/admin/quanlihoadon/ChiTietHoaDon.vue'),
          meta: { title: 'Chi tiết hóa đơn', breadcrumbs: [{ label: 'Quản lý hóa đơn', to: '/admin/quan-ly-hoa-don' }, { label: 'Chi tiết' }] },
        },
        {
          path: 'quan-ly-khach-hang',
          name: 'AdminCustomers',
          component: () => import('../pages/admin/quanlikhachhang/QuanLyKhachHang.vue'),
          meta: { title: 'Quản lý khách hàng', breadcrumbs: [{ label: 'Quản lý khách hàng' }] },
        },
        {
          path: 'quan-ly-nhan-vien',
          name: 'AdminEmployees',
          component: () => import('../pages/admin/quanlinhanvien/QuanLyNhanVien.vue'),
          meta: { title: 'Quản lý nhân viên', breadcrumbs: [{ label: 'Quản lý nhân viên' }] },
        },
        {
          path: 'quan-ly-nhan-vien/add',
          name: 'EmployeeCreate',
          component: () => import('../pages/admin/quanlinhanvien/EmployeeCreate.vue'),
          meta: { title: 'Thêm nhân viên', breadcrumbs: [{ label: 'Quản lý nhân viên', to: '/admin/quan-ly-nhan-vien' }, { label: 'Thêm mới' }] },
        },
        {
          path: 'quan-ly-nhan-vien/edit',
          name: 'EmployeeEdit',
          component: () => import('../pages/admin/quanlinhanvien/EmployeeEdit.vue'),
          meta: { title: 'Cập nhật nhân viên', breadcrumbs: [{ label: 'Quản lý nhân viên', to: '/admin/quan-ly-nhan-vien' }, { label: 'Cập nhật' }] },
        },
        {
          path: 'quan-ly-dot-giam-gia',
          name: 'AdminPromotions',
          component: () => import('../pages/admin/quanlidotgiamgia/QuanLyDotGiamGia.vue'),
          meta: { title: 'Quản lý đợt giảm giá', breadcrumbs: [{ label: 'Quản lý đợt giảm giá' }] },
        },
        {
          path: 'quan-ly-phieu-giam-gia',
          name: 'AdminVouchers',
          component: () => import('../pages/admin/quanliphieugiamgia/QuanLyPhieuGiamGia.vue'),
          meta: { title: 'Quản lý phiếu giảm giá', breadcrumbs: [{ label: 'Quản lý phiếu giảm giá' }] },
        },
        {
          path: 'quan-ly-phuong-thuc-thanh-toan',
          name: 'AdminPaymentMethods',
          component: () => import('../pages/admin/shared/AdminModulePlaceholder.vue'),
          props: {
            title: 'Quản Lý Phương Thức Thanh Toán',
            subtitle: 'Giữ module độc lập trên menu để sẵn sàng tách CRUD theo đúng domain thanh toán.',
          },
          meta: {
            title: 'Quản lý phương thức thanh toán',
            breadcrumbs: [{ label: 'Quản lý phương thức thanh toán' }],
          },
        },
        {
          path: 'quan-ly-phan-quyen',
          name: 'AdminPermissions',
          component: () => import('../pages/admin/shared/AdminModulePlaceholder.vue'),
          props: {
            title: 'Quản Lý Phân Quyền',
            subtitle: 'Giữ module độc lập trên menu để sẵn sàng tách CRUD theo đúng domain phân quyền.',
          },
          meta: {
            title: 'Quản lý phân quyền',
            breadcrumbs: [{ label: 'Quản lý phân quyền' }],
          },
        },

        // Legacy redirects
        {
          path: 'quan-ly-san-pham/bien-the',
          redirect: { name: 'AdminVariantsList' },
        },
        {
          path: 'quan-ly-bien-the',
          redirect: { name: 'AdminVariantsList' },
        },
        {
          path: 'quan-ly-bien-the/:pathMatch(.*)*',
          redirect: { name: 'AdminVariantsList' },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh',
          redirect: { name: 'AdminAttrColorsList' },
        },
        {
          path: 'quan-ly-san-pham/thuoc-tinh/:pathMatch(.*)*',
          redirect: { name: 'AdminAttrColorsList' },
        },
        {
          path: 'thuoc-tinh-san-pham',
          redirect: { name: 'AdminAttrColorsList' },
        },
        {
          path: 'thuoc-tinh-san-pham/:pathMatch(.*)*',
          redirect: { name: 'AdminAttrColorsList' },
        },
        {
          path: 'quan-ly-thuoc-tinh',
          redirect: { name: 'AdminAttrColorsList' },
        },
        {
          path: 'quan-ly-thuoc-tinh/:pathMatch(.*)*',
          redirect: { name: 'AdminAttrColorsList' },
        },

        {
          path: "error/403",
          name: "Error403",
          component: () => import("../pages/errors/403/Error403.vue"),
          meta: { title: "403 Forbidden" },
        },
        {
          path: "error/404",
          name: "Error404",
          component: () => import("../pages/errors/404/Error404.vue"),
          meta: { title: "404 Not Found" },
        },
        {
          path: "error/500",
          name: "Error500",
          component: () => import("../pages/errors/500/Error500.vue"),
          meta: { title: "500 Server Error" },
        },
      ],
      redirect: ROUTES.ADMIN.DASHBOARD,
    },
    {
      path: "/:pathMatch(.*)*",
      redirect: ROUTES.ADMIN.ERRORS.E404,
    },
  ],
});

import { useLoadingStore } from "@/stores/loadingStore";

router.beforeEach(() => {
  const loadingStore = useLoadingStore();
  loadingStore.startProgress();
});

router.afterEach(() => {
  const loadingStore = useLoadingStore();
  loadingStore.finishProgress();
});

setupRouteGuards(router);

export default router;
