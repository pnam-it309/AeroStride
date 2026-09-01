/**
 * Intelligent Route and Component Preloader
 * Proactively preloads chunk files during browser idle time or on hover
 * to eliminate first-load lag when switching screens.
 */

const routeModuleMap = {
    '/admin/thong-ke': () => import('@/views/modules/ThongKe.vue'),
    '/admin/ban-hang': () => import('@/views/modules/banhang/BanHang.vue'),
    '/admin/san-pham': () => import('@/views/modules/san-pham/SanPham.vue'),
    '/admin/san-pham/bien-the': () => import('@/views/modules/bien-the-san-pham/BienTheSanPham.vue'),
    '/admin/hoa-don': () => import('@/views/modules/hoa-don/HoaDon.vue'),
    '/admin/khach-hang': () => import('@/views/modules/khachhang/KhachHang.vue'),
    '/admin/nhan-vien': () => import('@/views/modules/nhanvien/NhanVien.vue'),
    '/admin/dot-giam-gia': () => import('@/views/modules/dot-giam-gia/DotGiamGia.vue'),
    '/admin/phieu-giam-gia': () => import('@/views/modules/phieu-giam-gia/PhieuGiamGia.vue'),
    '/admin/thuoc-tinh': () => import('@/views/modules/thuoctinh/ThuocTinh.vue'),
    '/admin/lich-lam-viec': () => import('@/views/modules/lich-lam-viec/LichLamViec.vue'),
    '/admin/lich-su-hoat-dong': () => import('@/views/modules/lich-lam-viec/LichSuHoatDong.vue'),
    '/admin/ban-giao-ca': () => import('@/views/modules/lich-lam-viec/BanGiaoCa.vue'),
    '/admin/chat': () => import('@/views/chat/ChatManagement.vue')
};

const preloadedRoutes = new Set();

/**
 * Preload a specific route by path
 */
export const preloadRoute = (path) => {
    if (!path || preloadedRoutes.has(path)) return;
    const loader = routeModuleMap[path];
    if (loader) {
        preloadedRoutes.add(path);
        loader().catch(() => {
            // Non-blocking catch
            preloadedRoutes.delete(path);
        });
    }
};

/**
 * Preload all major application routes during browser idle time
 */
export const initRoutePreloader = () => {
    const preloadAll = () => {
        const paths = Object.keys(routeModuleMap);
        let index = 0;

        const loadNext = () => {
            if (index >= paths.length) return;
            const path = paths[index++];
            preloadRoute(path);
            // Slight delay between chunks to keep network/UI silky smooth
            if (typeof window !== 'undefined' && 'requestIdleCallback' in window) {
                window.requestIdleCallback(loadNext, { timeout: 2000 });
            } else {
                setTimeout(loadNext, 120);
            }
        };

        if (typeof window !== 'undefined' && 'requestIdleCallback' in window) {
            window.requestIdleCallback(loadNext, { timeout: 2000 });
        } else {
            setTimeout(loadNext, 500);
        }
    };

    if (typeof window !== 'undefined') {
        if (document.readyState === 'complete') {
            preloadAll();
        } else {
            window.addEventListener('load', preloadAll, { once: true });
        }
    }
};
