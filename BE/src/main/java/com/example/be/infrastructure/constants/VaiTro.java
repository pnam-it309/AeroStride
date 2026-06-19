package com.example.be.infrastructure.constants;

/**
 * VaiTro - Role enum for the AeroStride system.
 * - KHACH_HANG: Default role for web customers (self-registration)
 * - NHAN_VIEN:  Staff role - managed by admin via CMS
 * - QUAN_TRI_VIEN: Super admin role - full system access
 */
public enum VaiTro {
    KHACH_HANG,
    NHAN_VIEN,
    QUAN_TRI_VIEN;

    // String representations of the roles for method parameters
    public static final String ADMIN = "QUAN_TRI_VIEN";
    public static final String STAFF = "NHAN_VIEN";
    public static final String CUSTOMER = "KHACH_HANG";

    // SpEL expressions for @PreAuthorize annotations
    public static final String PRE_AUTH_ADMIN_ONLY = "hasRole('" + ADMIN + "')";
    public static final String PRE_AUTH_ADMIN_STAFF = "hasAnyRole('" + ADMIN + "', '" + STAFF + "')";
}
