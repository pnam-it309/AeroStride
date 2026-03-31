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
    QUAN_TRI_VIEN
}
