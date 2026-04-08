import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

export function requireAuth(to, from, next) {
  if (dichVuXacThuc.daDangNhap()) {
    next();
  } else {
    next('/auth/login');
  }
}

export function requireGuest(to, from, next) {
  if (!dichVuXacThuc.daDangNhap()) {
    next();
  } else {
    next('/main');
  }
}

export function requireRole(role) {
  return (to, from, next) => {
    if (dichVuXacThuc.daDangNhap() && dichVuXacThuc.coVaiTro(role)) {
      next();
    } else {
      next('/auth/login');
    }
  };
}
