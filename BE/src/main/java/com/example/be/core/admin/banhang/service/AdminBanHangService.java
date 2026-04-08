package com.example.be.core.admin.banhang.service;

import com.example.be.core.admin.banhang.model.request.AdminBanHangCheckoutRequest;
import com.example.be.core.admin.banhang.model.request.AdminBanHangHoaDonChiTietRequest;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.model.response.BanHangSanPhamResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangKhachHangResponse;
import com.example.be.entity.PhieuGiamGia;

import java.math.BigDecimal;
import java.util.List;

public interface AdminBanHangService {
    List<AdminBanHangHoaDonResponse> getHoaDonCho();
    AdminBanHangHoaDonResponse createHoaDon();
    void deleteHoaDon(String id);
    AdminBanHangHoaDonResponse addSanPham(String idHoaDon, AdminBanHangHoaDonChiTietRequest request);
    AdminBanHangHoaDonResponse updateSoLuong(String idHoaDon, String idHoaDonChiTiet, Integer soLuong);
    void removeHoaDonChiTiet(String idHoaDon, String idHoaDonChiTiet);
    AdminBanHangHoaDonResponse setKhachHang(String idHoaDon, String idKhachHang);
    AdminBanHangHoaDonResponse setPhieuGiamGia(String idHoaDon, String idPhieuGiamGia);
    void checkout(String idHoaDon, AdminBanHangCheckoutRequest request);

    List<BanHangSanPhamResponse> searchSanPham(String keyword);
    List<AdminBanHangKhachHangResponse> searchKhachHang(String keyword);
    List<PhieuGiamGia> getVouchers(BigDecimal tongTien);
}
