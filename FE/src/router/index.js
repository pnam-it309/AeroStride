import { createRouter, createWebHistory } from 'vue-router'
import setupRouteGuards from './routeGuards'
import { ROUTES } from '../constants'

import AuthLayout from '../layouts/AuthLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: ROUTES.HOME, redirect: ROUTES.AUTH.LOGIN },
    {
      path: ROUTES.AUTH.BASE,
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        { path: 'login', name: 'Login', component: () => import('../pages/Login.vue') },
        { path: 'register', name: 'Register', component: () => import('../pages/Register.vue') },
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
          meta: { title: 'Thống kê' },
        },
        {
          path: 'quan-ly-san-pham',
          name: 'AdminProducts',
          component: () => import('../pages/admin/quanlisanpham/QuanLySanPham.vue'),
          meta: { title: 'Quản lý sản phẩm' },
        },
        {
          path: 'quan-ly-thuoc-tinh',
          redirect: { name: 'AdminAttrDanhMuc' },
        },
        {
          path: 'quan-ly-thuoc-tinh/danh-muc',
          name: 'AdminAttrDanhMuc',
          component: () => import('../pages/admin/quanlithuoctinh/DanhMuc.vue'),
          meta: { title: 'Danh mục' },
        },
        {
          path: 'quan-ly-thuoc-tinh/thuong-hieu',
          name: 'AdminAttrThuongHieu',
          component: () => import('../pages/admin/quanlithuoctinh/ThuongHieu.vue'),
          meta: { title: 'Thương hiệu' },
        },
        {
          path: 'quan-ly-thuoc-tinh/mau-sac',
          name: 'AdminAttrMauSac',
          component: () => import('../pages/admin/quanlithuoctinh/MauSac.vue'),
          meta: { title: 'Màu sắc' },
        },
        {
          path: 'quan-ly-thuoc-tinh/kich-thuoc',
          name: 'AdminAttrKichThuoc',
          component: () => import('../pages/admin/quanlithuoctinh/KichThuoc.vue'),
          meta: { title: 'Kích thước' },
        },
        {
          path: 'quan-ly-thuoc-tinh/de-giay',
          name: 'AdminAttrDeGiay',
          component: () => import('../pages/admin/quanlithuoctinh/DeGiay.vue'),
          meta: { title: 'Đế giày' },
        },
        {
          path: 'quan-ly-thuoc-tinh/chat-lieu',
          name: 'AdminAttrChatLieu',
          component: () => import('../pages/admin/quanlithuoctinh/ChatLieu.vue'),
          meta: { title: 'Chất liệu' },
        },
        {
          path: 'quan-ly-thuoc-tinh/co-giay',
          name: 'AdminAttrCoGiay',
          component: () => import('../pages/admin/quanlithuoctinh/CoGiay.vue'),
          meta: { title: 'Cỡ giày' },
        },
        {
          path: 'quan-ly-hoa-don',
          name: 'AdminOrders',
          component: () => import('../pages/admin/quanlihoadon/QuanLyHoaDon.vue'),
          meta: { title: 'Quản lý hóa đơn' },
        },
        {
          path: 'quan-ly-khach-hang',
          name: 'AdminCustomers',
          component: () => import('../pages/admin/quanlikhachhang/QuanLyKhachHang.vue'),
          meta: { title: 'Quản lý khách hàng' },
        },
        {
          path: 'quan-ly-nhan-vien',
          name: 'AdminEmployees',
          component: () => import('../pages/admin/quanlinhanvien/QuanLyNhanVien.vue'),
          meta: { title: 'Quản lý nhân viên' },
        },
        {
          path: 'quan-ly-dot-giam-gia',
          name: 'AdminPromotions',
          component: () => import('../pages/admin/quanlidotgiamgia/QuanLyDotGiamGia.vue'),
          meta: { title: 'Quản lý đợt giảm giá' },
        },
        {
          path: 'quan-ly-phieu-giam-gia',
          name: 'AdminVouchers',
          component: () => import('../pages/admin/quanliphieugiamgia/QuanLyPhieuGiamGia.vue'),
          meta: { title: 'Quản lý phiếu giảm giá' },
        },
        // Error Pages
        {
          path: 'error/403',
          name: 'Error403',
          component: () => import('../pages/errors/403/Error403.vue'),
          meta: { title: '403 Forbidden' },
        },
        {
          path: 'error/404',
          name: 'Error404',
          component: () => import('../pages/errors/404/Error404.vue'),
          meta: { title: '404 Not Found' },
        },
        {
          path: 'error/500',
          name: 'Error500',
          component: () => import('../pages/errors/500/Error500.vue'),
          meta: { title: '500 Server Error' },
        },
      ],
      redirect: ROUTES.ADMIN.DASHBOARD,
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: ROUTES.ADMIN.ERRORS.E404,
    },
  ],
})

import { useLoadingStore } from '@/stores/loadingStore'

router.beforeEach(() => {
  const loadingStore = useLoadingStore()
  loadingStore.startProgress()
})

router.afterEach(() => {
  const loadingStore = useLoadingStore()
  loadingStore.finishProgress()
})

setupRouteGuards(router)

export default router
