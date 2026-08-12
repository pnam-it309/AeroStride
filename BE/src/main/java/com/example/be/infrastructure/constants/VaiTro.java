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

    /**
     * Kiểm tra xem thông tin phân quyền hoặc nhân viên có thuộc nhóm Quản lý hay không.
     */
    public static boolean isManagementRole(PhanQuyen phanQuyen) {
        if (phanQuyen == null) return false;
        return isManagementRoleCode(phanQuyen.getMa(), phanQuyen.getQuyenHan(), phanQuyen.getTen());
    }

    public static boolean isManagementRole(NhanVien nv) {
        if (nv == null) return false;
        return isManagementRole(nv.getPhanQuyen());
    }

    public static boolean isManagementRoleCode(String ma, String quyen, String ten) {
        if (ma != null) {
            String upper = ma.toUpperCase();
            if (upper.equals(ADMIN) || upper.contains("QUAN_LY")) {
                return true;
            }
        }
        if (quyen != null) {
            String upper = quyen.toUpperCase();
            if (upper.contains("MANAGEMENT")) {
                return true;
            }
        }
        if (ten != null) {
            String lower = ten.toLowerCase();
            if (lower.contains("quản lý")) {
                return true;
            }
        }
        return false;
    }
}
