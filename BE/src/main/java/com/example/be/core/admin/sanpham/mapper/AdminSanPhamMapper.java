package com.example.be.core.admin.sanpham.mapper;

import com.example.be.core.admin.sanpham.model.response.ProductDetailResponse;
import com.example.be.core.admin.sanpham.model.response.ProductResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantImageResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.repository.ProductVariantStatisticsProjection;
import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.SanPham;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminSanPhamMapper {

    public ProductResponse toProductResponse(SanPham sanPham, ProductVariantStatisticsProjection statistics) {
        return ProductResponse.builder()
                .id(sanPham.getId())
                .maSanPham(sanPham.getMa())
                .tenSanPham(sanPham.getTen())
                .idDanhMuc(sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getId() : null)
                .tenDanhMuc(sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getTen() : null)
                .idThuongHieu(sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getId() : null)
                .tenThuongHieu(sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getTen() : null)
                .idXuatXu(sanPham.getXuatXu() != null ? sanPham.getXuatXu().getId() : null)
                .tenXuatXu(sanPham.getXuatXu() != null ? sanPham.getXuatXu().getTen() : null)
                .idMucDichChay(sanPham.getMucDichChay() != null ? sanPham.getMucDichChay().getId() : null)
                .tenMucDichChay(sanPham.getMucDichChay() != null ? sanPham.getMucDichChay().getTen() : null)
                .idCoGiay(sanPham.getCoGiay() != null ? sanPham.getCoGiay().getId() : null)
                .tenCoGiay(sanPham.getCoGiay() != null ? sanPham.getCoGiay().getTen() : null)
                .idChatLieu(sanPham.getChatLieu() != null ? sanPham.getChatLieu().getId() : null)
                .tenChatLieu(sanPham.getChatLieu() != null ? sanPham.getChatLieu().getTen() : null)
                .idDeGiay(sanPham.getDeGiay() != null ? sanPham.getDeGiay().getId() : null)
                .tenDeGiay(sanPham.getDeGiay() != null ? sanPham.getDeGiay().getTen() : null)
                .gioiTinhKhachHang(sanPham.getGioiTinhKhachHang())
                .moTaNgan(sanPham.getMoTaNgan())
                .hinhAnh(sanPham.getHinhAnh())
                .trangThai(sanPham.getTrangThai())
                .ngayTao(sanPham.getNgayTao())
                .ngayCapNhat(sanPham.getNgayCapNhat())
                .tongBienThe(statistics != null ? statistics.getTongBienThe() : 0L)
                .tongSoLuongTon(statistics != null ? statistics.getTongSoLuongTon() : 0L)
                .giaBanThapNhat(statistics != null ? statistics.getGiaBanThapNhat() : null)
                .giaBanCaoNhat(statistics != null ? statistics.getGiaBanCaoNhat() : null)
                .build();
    }

    public ProductDetailResponse toProductDetailResponse(SanPham sanPham, List<ProductVariantResponse> variants) {
        return ProductDetailResponse.builder()
                .id(sanPham.getId())
                .maSanPham(sanPham.getMa())
                .tenSanPham(sanPham.getTen())
                .idDanhMuc(sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getId() : null)
                .tenDanhMuc(sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getTen() : null)
                .idThuongHieu(sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getId() : null)
                .tenThuongHieu(sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getTen() : null)
                .idXuatXu(sanPham.getXuatXu() != null ? sanPham.getXuatXu().getId() : null)
                .tenXuatXu(sanPham.getXuatXu() != null ? sanPham.getXuatXu().getTen() : null)
                .idMucDichChay(sanPham.getMucDichChay() != null ? sanPham.getMucDichChay().getId() : null)
                .tenMucDichChay(sanPham.getMucDichChay() != null ? sanPham.getMucDichChay().getTen() : null)
                .idCoGiay(sanPham.getCoGiay() != null ? sanPham.getCoGiay().getId() : null)
                .tenCoGiay(sanPham.getCoGiay() != null ? sanPham.getCoGiay().getTen() : null)
                .idChatLieu(sanPham.getChatLieu() != null ? sanPham.getChatLieu().getId() : null)
                .tenChatLieu(sanPham.getChatLieu() != null ? sanPham.getChatLieu().getTen() : null)
                .idDeGiay(sanPham.getDeGiay() != null ? sanPham.getDeGiay().getId() : null)
                .tenDeGiay(sanPham.getDeGiay() != null ? sanPham.getDeGiay().getTen() : null)
                .gioiTinhKhachHang(sanPham.getGioiTinhKhachHang())
                .moTaNgan(sanPham.getMoTaNgan())
                .moTaChiTiet(sanPham.getMoTaChiTiet())
                .hinhAnh(sanPham.getHinhAnh())
                .trangThai(sanPham.getTrangThai())
                .ngayTao(sanPham.getNgayTao())
                .nguoiTao(sanPham.getNguoiTao())
                .ngayCapNhat(sanPham.getNgayCapNhat())
                .nguoiCapNhat(sanPham.getNguoiCapNhat())
                .variants(variants)
                .build();
    }

    public ProductVariantResponse toVariantResponse(ChiTietSanPham variant, List<ProductVariantImageResponse> images) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .idSanPham(variant.getSanPham() != null ? variant.getSanPham().getId() : null)
                .tenSanPham(variant.getSanPham() != null ? variant.getSanPham().getTen() : null)
                .tenSanPhamDayDu(variant.getSanPham() != null 
                    ? String.format("%s [%s - %s]", 
                        variant.getSanPham().getTen(), 
                        variant.getMauSac() != null ? variant.getMauSac().getTen() : "?",
                        variant.getKichThuoc() != null ? variant.getKichThuoc().getTen() : "?") 
                    : null)
                .maChiTietSanPham(variant.getMaChiTietSanPham())
                .idMauSac(variant.getMauSac() != null ? variant.getMauSac().getId() : null)
                .tenMauSac(variant.getMauSac() != null ? variant.getMauSac().getTen() : null)
                .maMauHex(variant.getMauSac() != null ? variant.getMauSac().getMaMauHex() : null)
                .idKichThuoc(variant.getKichThuoc() != null ? variant.getKichThuoc().getId() : null)
                .tenKichThuoc(variant.getKichThuoc() != null ? variant.getKichThuoc().getTen() : null)
                .giaTriKichThuoc(variant.getKichThuoc() != null ? variant.getKichThuoc().getGiaTriKichThuoc() : null)
                .soLuong(variant.getSoLuong())
                .giaNhap(variant.getGiaNhap())
                .giaBan(variant.getGiaBan())
                .trangThai(variant.getTrangThai())
                .ngayTao(variant.getNgayTao())
                .ngayCapNhat(variant.getNgayCapNhat())
                .images(images)
                .build();
    }

    public ProductVariantImageResponse toVariantImageResponse(AnhChiTietSanPham image) {
        return ProductVariantImageResponse.builder()
                .id(image.getId())
                .duongDanAnh(image.getDuongDanAnh())
                .moTa(image.getMoTa())
                .hinhAnhDaiDien(image.getHinhAnhDaiDien())
                .trangThai(image.getTrangThai())
                .ngayTao(image.getNgayTao())
                .ngayCapNhat(image.getNgayCapNhat())
                .build();
    }
}
