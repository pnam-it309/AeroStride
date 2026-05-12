import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { PATH } from './routePaths';

export function requireAuth(to, from, next) {
    if (dichVuXacThuc.daDangNhap()) {
        next();
    } else {
        // Chỉ bắt đăng nhập nếu cố tình truy cập vào vùng quản trị (admin hoặc main)
        if (to.path.startsWith('/admin') || to.path.startsWith('/main')) {
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
    next(PATH.DASHBOARD);
  }
}

export function requireRole(role) {
  return (to, from, next) => {
    if (dichVuXacThuc.daDangNhap() && dichVuXacThuc.coVaiTro(role)) {
      next();
    } else {
      if (to.path.startsWith('/admin') || to.path.startsWith('/main')) {
        next(PATH.ADMIN_LOGIN);
      } else {
        next(PATH.LOGIN);
      }
    }
  };
}
