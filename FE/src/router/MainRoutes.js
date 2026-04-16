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
            component: () => import('@/views/modules/banhang/BanHang.vue')
        },
        {
            name: 'SanPham',
            path: '/san-pham',
            component: () => import('@/views/modules/san-pham/SanPham.vue')
        },
        {
            name: 'SanPhamVariants',
            path: '/san-pham/bien-the',
            component: () => import('@/views/modules/san-pham/SanPhamVariants.vue')
        },
        {
            name: 'SanPhamForm',
            path: '/san-pham/form/:id?',
            component: () => import('@/views/modules/san-pham/SanPhamForm.vue')
        },
        {
            name: 'SanPhamDetail',
            path: '/san-pham/detail/:id',
            component: () => import('@/views/modules/san-pham/SanPhamForm.vue')
        },
        {
            name: 'KhachHang',
            path: '/khach-hang',
            component: () => import('@/views/modules/khachhang/KhachHang.vue')
        },
        {
            name: 'KhachHangForm',
            path: '/khach-hang/form/:id?',
            component: () => import('@/views/modules/khachhang/KhachHangForm.vue')
        },
        {
            name: 'KhachHangDetail',
            path: '/khach-hang/detail/:id',
            component: () => import('@/views/modules/khachhang/KhachHangForm.vue')
        },
        {
            name: 'HoaDon',
            path: '/hoa-don',
            component: () => import('@/views/modules/hoa-don/HoaDon.vue')
        },
        {
            name: 'HoaDonChiTiet',
            path: '/hoa-don/chi-tiet/:id',
            component: () => import('@/views/modules/hoa-don/HoaDonChiTiet.vue')
        },
        {
            name: 'NhanVien',
            path: '/nhan-vien',
            component: () => import('@/views/modules/nhanvien/NhanVien.vue')
        },
        {
            name: 'NhanVienForm',
            path: '/nhan-vien/form/:id?',
            component: () => import('@/views/modules/nhanvien/NhanVienForm.vue')
        },
        {
            name: 'NhanVienDetail',
            path: '/nhan-vien/detail/:id',
            component: () => import('@/views/modules/nhanvien/NhanVienForm.vue')
        },
        {
            name: 'DotGiamGia',
            path: '/dot-giam-gia',
            component: () => import('@/views/modules/dot-giam-gia/DotGiamGia.vue')
        },
        {
            name: 'DotGiamGiaForm',
            path: '/dot-giam-gia/form/:id?',
            component: () => import('@/views/modules/dot-giam-gia/DotGiamGiaForm.vue')
        },
        {
            name: 'DotGiamGiaDetail',
            path: '/dot-giam-gia/detail/:id',
            component: () => import('@/views/modules/dot-giam-gia/DotGiamGiaForm.vue')
        },
        {
            name: 'PhieuGiamGia',
            path: '/phieu-giam-gia',
            component: () => import('@/views/modules/phieu-giam-gia/PhieuGiamGia.vue')
        },
        {
            name: 'PhieuGiamGiaForm',
            path: '/phieu-giam-gia/form/:id?',
            component: () => import('@/views/modules/phieu-giam-gia/PhieuGiamGiaForm.vue')
        },
        {
            name: 'PhieuGiamGiaDetail',
            path: '/phieu-giam-gia/detail/:id',
            component: () => import('@/views/modules/phieu-giam-gia/PhieuGiamGiaForm.vue')
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
