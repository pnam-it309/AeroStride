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

    // String representations of Spring Security roles (without ROLE_ prefix) & DB Role Codes
    public static final String ADMIN = "QUAN_LY";
    public static final String STAFF = "NHAN_VIEN";
    public static final String CUSTOMER = "KHACH_HANG";

    // SpEL expressions for @PreAuthorize annotations
    public static final String PRE_AUTH_ADMIN_ONLY = "hasRole('QUAN_LY')";
    public static final String PRE_AUTH_ADMIN_STAFF = "hasAnyRole('QUAN_LY', 'NHAN_VIEN')";

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
            if (ROLE_CODE_ADMIN.equalsIgnoreCase(ma)
                    || ROLE_CODE_QUAN_TRI_VIEN.equalsIgnoreCase(ma)
                    || ROLE_CODE_MANAGER.equalsIgnoreCase(ma)
                    || ROLE_CODE_QUAN_LY.equalsIgnoreCase(ma)
                    || ADMIN.equalsIgnoreCase(ma)) {
                return true;
            }
        }
        if (quyen != null) {
            if ("FULL_ACCESS".equalsIgnoreCase(quyen) || "MANAGER_ACCESS".equalsIgnoreCase(quyen)) {
                return true;
            }
        }
        if (ten != null) {
            String lowerTen = ten.toLowerCase();
            if (lowerTen.contains("quản trị") || lowerTen.contains("quản lý") || lowerTen.contains("admin") || lowerTen.contains("manager")) {
                return true;
            }
        }
        return false;
    }
}
