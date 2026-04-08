const MainRoutes = {
    path: '/main',
    meta: {
        requiresAuth: true
    },
    component: () => import('@/layouts/full/FullLayout.vue'),
    children: [
        {
            name: 'Dashboard',
            path: '',
            component: () => import('@/views/dashboard/index.vue')
        },
        // AeroStride Module Routes
        {
            name: 'BanHang',
            path: '/ban-hang',
            component: () => import('@/views/modules/BanHang.vue')
        },
        {
            name: 'SanPham',
            path: '/san-pham',
            component: () => import('@/views/modules/SanPham.vue')
        },
        {
            name: 'KhachHang',
            path: '/khach-hang',
            component: () => import('@/views/modules/KhachHang.vue')
        },
        {
            name: 'HoaDon',
            path: '/hoa-don',
            component: () => import('@/views/modules/HoaDon.vue')
        },
        {
            name: 'NhanVien',
            path: '/nhan-vien',
            component: () => import('@/views/modules/NhanVien.vue')
        },
        {
            name: 'DotGiamGia',
            path: '/dot-giam-gia',
            component: () => import('@/views/modules/DotGiamGia.vue')
        },
        {
            name: 'PhieuGiamGia',
            path: '/phieu-giam-gia',
            component: () => import('@/views/modules/PhieuGiamGia.vue')
        },
        {
            name: 'ThuocTinh',
            path: '/thuoc-tinh/:tab?',
            component: () => import('@/views/modules/ThuocTinh.vue')
        },
        {
            name: 'ThanhToan',
            path: '/thanh-toan',
            component: () => import('@/views/modules/ThanhToan.vue')
        },
        {
            name: 'QuanLyFile',
            path: '/quan-ly-file',
            component: () => import('@/views/modules/QuanLyFile.vue')
        },
        {
            name: 'ThongKe',
            path: '/thong-ke',
            component: () => import('@/views/modules/ThongKe.vue')
        },
        // Original UI Components (can be removed later if not needed)
        {
            name: 'Alert',
            path: '/ui/alerts',
            component: () => import('@/views/ui-components/Alerts.vue')
        },
        {
            name: 'Buttons',
            path: '/ui/buttons',
            component: () => import('@/views/ui-components/Buttons.vue')
        },
        {
            name: 'Cards',
            path: '/ui/cards',
            component: () => import('@/views/ui-components/Cards.vue')
        },
        {
            name: 'Tables',
            path: '/ui/tables',
            component: () => import('@/views/ui-components/Tables.vue')
        },
        {
            name: 'Icons',
            path: '/icons',
            component: () => import('@/views/pages/Icons.vue')
        },
        {
            name: 'Starter',
            path: '/sample-page',
            component: () => import('@/views/pages/SamplePage.vue')
        },
    ]
};

export default MainRoutes;
