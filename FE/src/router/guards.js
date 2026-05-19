import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { PATH } from './routePaths';
import { APP_ROLES } from '@/constants/appConstants';

export function requireAuth(to, from, next) {
    if (dichVuXacThuc.daDangNhap()) {
        const user = dichVuXacThuc.layUserHienTai();
        
        // Nếu là khách hàng cố tình truy cập vào vùng quản trị (admin)
        if (to.path.startsWith('/admin') && user && user.role === APP_ROLES.CUSTOMER) {
            next('/'); // Chuyển hướng về trang chủ của khách hàng
        } else if (!to.path.startsWith('/admin') && user && (user.role === APP_ROLES.ADMIN || user.role === APP_ROLES.STAFF)) {
            // Nếu là admin hoặc nhân viên mà cố tình truy cập các trang cá nhân/đơn hàng riêng biệt của khách hàng
            const customerOnlyPaths = ['/my-orders', '/profile', '/thanh-toan'];
            if (customerOnlyPaths.some(p => to.path.startsWith(p))) {
                next(PATH.DASHBOARD); // Chuyển hướng về trang dashboard quản trị
            } else {
                next();
            }
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
    if (user && user.role === APP_ROLES.CUSTOMER) {
      next('/'); // Đã đăng nhập với tư cách khách hàng thì đưa về trang chủ
    } else if (user && (user.role === APP_ROLES.ADMIN || user.role === APP_ROLES.STAFF)) {
      next(PATH.DASHBOARD); // Đã đăng nhập với tư cách admin/staff thì đưa về trang dashboard quản trị
    } else {
      next('/');
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
