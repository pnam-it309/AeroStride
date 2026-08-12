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

    // String representations of Spring Security roles (without ROLE_ prefix)
    public static final String ADMIN = "QUAN_LY";
    public static final String STAFF = "NHAN_VIEN";
    public static final String CUSTOMER = "KHACH_HANG";

    // Standard database role codes & legacy compatibility constants
    public static final String ROLE_CODE_QUAN_LY = "QUAN_LY";
    public static final String ROLE_CODE_ADMIN = "ADMIN";
    public static final String ROLE_CODE_QUAN_TRI_VIEN = "QUAN_TRI_VIEN";
    public static final String ROLE_CODE_MANAGER = "MANAGER";

    public static final String ACCESS_FULL = "FULL_ACCESS";
    public static final String ACCESS_MANAGEMENT = "MANAGEMENT_ACCESS";
    public static final String ACCESS_MANAGER = "MANAGER_ACCESS";

    // SpEL expressions for @PreAuthorize annotations
    public static final String PRE_AUTH_ADMIN_ONLY = "hasRole('" + ADMIN + "')";
    public static final String PRE_AUTH_ADMIN_STAFF = "hasAnyRole('" + ADMIN + "', '" + STAFF + "')";

    /**
     * Kiểm tra xem thông tin phân quyền hoặc nhân viên có thuộc nhóm Quản lý / Admin hay không.
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
            String upperMa = ma.toUpperCase();
            if (upperMa.contains(ROLE_CODE_QUAN_LY)
                    || upperMa.contains(ROLE_CODE_ADMIN)
                    || upperMa.contains(ROLE_CODE_QUAN_TRI_VIEN)
                    || upperMa.contains(ROLE_CODE_MANAGER)) {
                return true;
            }
        }
        if (quyen != null) {
            String upperQuyen = quyen.toUpperCase();
            if (upperQuyen.contains(ACCESS_FULL)
                    || upperQuyen.contains(ACCESS_MANAGEMENT)
                    || upperQuyen.contains(ACCESS_MANAGER)) {
                return true;
            }
        }
        if (ten != null) {
            String lowerTen = ten.toLowerCase();
            if (lowerTen.contains("quản lý") || lowerTen.contains("quản trị") || lowerTen.contains("admin") || lowerTen.contains("manager")) {
                return true;
            }
        }
        return false;
    }
}
