import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { PATH } from './routePaths';

export function requireAuth(to, from, next) {
    if (dichVuXacThuc.daDangNhap()) {
        const user = dichVuXacThuc.layUserHienTai();
        // Nếu là khách hàng (ROLE_KHACH_HANG) cố tình truy cập vào vùng quản trị (admin)
        if (to.path.startsWith('/admin') && user && user.role === 'ROLE_KHACH_HANG') {
            next('/'); // Chuyển hướng về trang chủ của khách hàng
        } else {
            next();
        }
    } else {
        // Chỉ bắt đăng nhập nếu cố tình truy cập vào vùng quản trị (admin)
        if (to.path.startsWith('/admin')) {
            next(PATH.ADMIN_LOGIN);
        } else {
            // Các trang công khai (Landing, Shoes, Detail...) luôn cho phép vào mà không cần login
            next();
        }
    }
}

export function requireGuest(to, from, next) {
  if (!dichVuXacThuc.daDangNhap()) {
    next();
  } else {
    const user = dichVuXacThuc.layUserHienTai();
    if (user && user.role === 'ROLE_KHACH_HANG') {
      next('/'); // Đã đăng nhập với tư cách khách hàng thì đưa về trang chủ
    } else {
      next(PATH.DASHBOARD); // Đã đăng nhập với tư cách admin/staff thì đưa về trang dashboard quản trị
    }
  }
}

export function requireRole(role) {
  return (to, from, next) => {
    if (dichVuXacThuc.daDangNhap() && dichVuXacThuc.coVaiTro(role)) {
      next();
    } else {
      if (to.path.startsWith('/admin')) {
        next(PATH.ADMIN_LOGIN);
      } else {
        next(PATH.LOGIN);
      }
    }
  };
}
