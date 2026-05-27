import { PATH } from './routePaths';

const MainRoutes = {
    path: '/',
    meta: {
        requiresAuth: true
    },
    component: () => import('@/layouts/full/FullLayout.vue'),
    children: [
        {
            name: 'Thống kê',
            path: PATH.THONG_KE,
            component: () => import('@/views/modules/ThongKe.vue')
        },
        {
            name: 'BanHang',
            path: PATH.BAN_HANG,
            component: () => import('@/views/modules/banhang/BanHang.vue')
        },
        {
            name: 'SanPham',
            path: PATH.SAN_PHAM,
            component: () => import('@/views/modules/san-pham/SanPham.vue')
        },
        {
            name: 'BienTheSanPham',
            path: PATH.BIEN_THE_SAN_PHAM,
            component: () => import('@/views/modules/bien-the-san-pham/BienTheSanPham.vue')
        },
        {
            name: 'SanPhamForm',
            path: `${PATH.SAN_PHAM_FORM}/:id?`,
            component: () => import('@/views/modules/san-pham/SanPhamForm.vue')
        },
        {
            name: 'KhachHang',
            path: PATH.KHACH_HANG,
            component: () => import('@/views/modules/khachhang/KhachHang.vue')
        },
        {
            name: 'KhachHangForm',
            path: `${PATH.KHACH_HANG_FORM}/:id?`,
            component: () => import('@/views/modules/khachhang/KhachHangForm.vue')
        },
        {
            name: 'KhachHangDetail',
            path: `${PATH.KHACH_HANG_DETAIL}/:id`,
            component: () => import('@/views/modules/khachhang/KhachHangForm.vue')
        },
        {
            name: 'HoaDon',
            path: PATH.HOA_DON,
            component: () => import('@/views/modules/hoa-don/HoaDon.vue')
        },
        {
            name: 'HoaDonChiTiet',
            path: `${PATH.HOA_DON_CHI_TIET}/:id`,
            component: () => import('@/views/modules/hoa-don/HoaDonChiTiet.vue')
        },
        {
            name: 'NhanVien',
            path: PATH.NHAN_VIEN,
            component: () => import('@/views/modules/nhanvien/NhanVien.vue')
        },
        {
            name: 'NhanVienForm',
            path: `${PATH.NHAN_VIEN_FORM}/:id?`,
            component: () => import('@/views/modules/nhanvien/NhanVienForm.vue')
        },
        {
            name: 'NhanVienDetail',
            path: `${PATH.NHAN_VIEN_DETAIL}/:id`,
            component: () => import('@/views/modules/nhanvien/NhanVienForm.vue')
        },
        {
            name: 'DotGiamGia',
            path: PATH.DOT_GIAM_GIA,
            component: () => import('@/views/modules/dot-giam-gia/DotGiamGia.vue')
        },
        {
            name: 'DotGiamGiaForm',
            path: `${PATH.DOT_GIAM_GIA_FORM}/:id?`,
            component: () => import('@/views/modules/dot-giam-gia/DotGiamGiaForm.vue')
        },
        {
            name: 'DotGiamGiaDetail',
            path: `${PATH.DOT_GIAM_GIA_DETAIL}/:id`,
            component: () => import('@/views/modules/dot-giam-gia/DotGiamGiaForm.vue')
        },
        {
            name: 'PhieuGiamGia',
            path: PATH.PHIEU_GIAM_GIA,
            component: () => import('@/views/modules/phieu-giam-gia/PhieuGiamGia.vue')
        },
        {
            name: 'PhieuGiamGiaForm',
            path: `${PATH.PHIEU_GIAM_GIA_FORM}/:id?`,
            component: () => import('@/views/modules/phieu-giam-gia/PhieuGiamGiaForm.vue')
        },
        {
            name: 'PhieuGiamGiaDetail',
            path: `${PATH.PHIEU_GIAM_GIA_DETAIL}/:id`,
            component: () => import('@/views/modules/phieu-giam-gia/PhieuGiamGiaForm.vue')
        },
        {
            name: 'ThuocTinh',
            path: `${PATH.THUOC_TINH}/:tab?`,
            component: () => import('@/views/modules/thuoctinh/ThuocTinh.vue')
        },
        {
            name: 'ThanhToan',
            path: PATH.THANH_TOAN,
            component: () => import('@/views/modules/ThanhToan.vue')
        },
        {
            name: 'QuanLyFile',
            path: '/quan-ly-file',
            component: () => import('@/views/modules/QuanLyFile.vue')
        },

        {
            name: 'QuanLyChat',
            path: PATH.QUAN_LY_CHAT,
            component: () => import('@/views/chat/ChatManagement.vue')
        },
        {
            name: 'LichLamViec',
            path: PATH.LICH_LAM_VIEC,
            component: () => import('@/views/modules/lich-lam-viec/LichLamViec.vue')
        },
        {
            name: 'LichCaLam',
            path: PATH.LICH_CA_LAM,
            component: () => import('@/views/modules/lich-lam-viec/LichCaLam.vue')
        },
        {
            name: 'LichSuHoatDong',
            path: PATH.LICH_SU_HOAT_DONG,
            component: () => import('@/views/modules/lich-lam-viec/LichSuHoatDong.vue')
        }
    ]
};

export default MainRoutes;
