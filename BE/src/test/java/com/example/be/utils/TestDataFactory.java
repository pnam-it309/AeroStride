package com.example.be.utils;

import com.example.be.entity.KhachHang;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import java.util.UUID;

/**
 * Enterprise standard Data Factory for Testing.
 * Eliminates boilerplate code across test files (DRY).
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    public static NhanVien createMockNhanVien() {
        NhanVien nv = new NhanVien();
        nv.setId(UUID.randomUUID().toString());
        nv.setMa("NV-TEST-" + System.currentTimeMillis());
        nv.setTen("Test Employee");
        nv.setEmail("test" + System.currentTimeMillis() + "@gmail.com");
        nv.setMatKhau("encrypted_pass");
        nv.setTrangThai(TrangThai.DANG_HOAT_DONG);
        nv.setXoaMem(false);
        return nv;
    }

    public static KhachHang createMockKhachHang() {
        KhachHang kh = new KhachHang();
        kh.setId(UUID.randomUUID().toString());
        kh.setMa("KH-TEST-" + System.currentTimeMillis());
        kh.setTen("Test Customer");
        kh.setEmail("customer" + System.currentTimeMillis() + "@gmail.com");
        kh.setTrangThai(TrangThai.DANG_HOAT_DONG);
        kh.setXoaMem(false);
        return kh;
    }

    public static com.example.be.entity.SanPham createMockSanPham() {
        com.example.be.entity.SanPham sp = new com.example.be.entity.SanPham();
        sp.setId(UUID.randomUUID().toString());
        sp.setMa("SP-TEST-" + System.currentTimeMillis());
        sp.setTen("Giày Chạy Aero");
        sp.setTrangThai(TrangThai.DANG_HOAT_DONG);
        sp.setXoaMem(false);
        return sp;
    }

    public static com.example.be.entity.ChiTietSanPham createMockVariant(com.example.be.entity.SanPham sp) {
        com.example.be.entity.ChiTietSanPham ct = new com.example.be.entity.ChiTietSanPham();
        ct.setId(UUID.randomUUID().toString());
        ct.setSanPham(sp);
        ct.setMaChiTietSanPham(sp.getMa() + "-VAR");
        ct.setGiaBan(new java.math.BigDecimal("1200000"));
        ct.setSoLuong(100);
        ct.setTrangThai(TrangThai.DANG_HOAT_DONG);
        ct.setXoaMem(false);
        return ct;
    }
}
