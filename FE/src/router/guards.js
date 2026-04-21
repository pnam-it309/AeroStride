import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { PATH } from './routePaths';

export function requireAuth(to, from, next) {
  if (dichVuXacThuc.daDangNhap()) {
    next();
  } else {
    next(PATH.LOGIN);
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
      next(PATH.LOGIN);
    }
  };
}
