package com.example.be.infrastructure.constants;

import com.example.be.entity.NhanVien;
import com.example.be.entity.PhanQuyen;

/**
 * VaiTro - Role enum and constants for the AeroStride system.
 * Strictly 3 roles:
 * - KHACH_HANG: Khách hàng (Customer)
 * - NHAN_VIEN:  Nhân viên (Staff)
 * - QUAN_LY:    Quản lý (Management role)
 */
public enum VaiTro {
    KHACH_HANG,
    NHAN_VIEN,
    QUAN_LY;

    // String representations of Spring Security roles & DB Role Codes
    public static final String ADMIN = "QUAN_LY";
    public static final String STAFF = "NHAN_VIEN";
    public static final String CUSTOMER = "KHACH_HANG";

    // SpEL expressions for @PreAuthorize annotations
    public static final String PRE_AUTH_ADMIN_ONLY = "hasRole('" + ADMIN + "')";
    public static final String PRE_AUTH_ADMIN_STAFF = "hasAnyRole('" + ADMIN + "', '" + STAFF + "')";
    public static final String PRE_AUTH_CUSTOMER = "hasRole('" + CUSTOMER + "')";

    /**
     * Kiểm tra xem thông tin phân quyền hoặc nhân viên có thuộc nhóm Quản lý hay không.
     */
    public static boolean isManagementRole(PhanQuyen phanQuyen) {
        if (phanQuyen == null) return false;
        if (isManagementRoleCode(phanQuyen.getMa())) return true;
        if (phanQuyen.getTen() != null) {
            String lower = phanQuyen.getTen().toLowerCase();
            if (lower.contains("quản lý") || lower.contains("quan ly") || lower.contains("admin") || lower.contains("manager") || lower.contains("quản trị")) {
                return true;
            }
        }
        if (phanQuyen.getQuyenHan() != null) {
            String upper = phanQuyen.getQuyenHan().toUpperCase();
            if (upper.contains("MANAGEMENT") || upper.contains("ADMIN") || upper.contains("FULL_ACCESS") || upper.contains("QUAN_LY")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isManagementRole(NhanVien nv) {
        if (nv == null) return false;
        if (nv.getPhanQuyen() != null && isManagementRole(nv.getPhanQuyen())) {
            return true;
        }
        // Fallback kiểm tra trực tiếp mã nhân viên, tên tài khoản hoặc id của 5 tài khoản admin mẫu
        if (nv.getMa() != null) {
            String ma = nv.getMa().toUpperCase();
            if (ma.equals("NV001") || ma.equals("NV002") || ma.equals("NV003") || ma.equals("NV004") || ma.equals("NV005")) {
                return true;
            }
        }
        if (nv.getTenTaiKhoan() != null) {
            String user = nv.getTenTaiKhoan().toLowerCase();
            if (user.equals("admin") || user.startsWith("admin")) {
                return true;
            }
        }
        if (nv.getId() != null && (nv.getId().equals("nv1") || nv.getId().equals("nv2") || nv.getId().equals("nv3") || nv.getId().equals("nv4") || nv.getId().equals("nv5"))) {
            return true;
        }
        return false;
    }

    public static boolean isManagementRoleCode(String ma) {
        if (ma == null) return false;
        String upper = ma.trim().toUpperCase();
        return upper.equals(ADMIN) || upper.equals("ROLE_" + ADMIN) || upper.contains("QUAN_LY") || upper.contains("ADMIN") || upper.contains("MANAGER") || upper.contains("QUAN_TRI");
    }
}
