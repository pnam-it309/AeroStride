package com.example.be.core.admin.sanpham.mapper;

import com.example.be.core.admin.sanpham.model.response.ProductDetailResponse;
import com.example.be.core.admin.sanpham.model.response.ProductResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantImageResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.repository.ProductVariantStatisticsProjection;
import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.SanPham;
import com.example.be.utils.DiscountPriceUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AdminSanPhamMapper {

    /** Map san pham cha cho man danh sach, kem thong ke tong bien the/ton kho/gia. */
    public ProductResponse toProductResponse(SanPham sanPham, ProductVariantStatisticsProjection statistics) {
        return ProductResponse.builder()
                .id(sanPham.getId())
                .maSanPham(sanPham.getMa())
                .tenSanPham(sanPham.getTen())
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

    /** Map chi tiet san pham cha va danh sach bien the cho man them/sua san pham. */
    public ProductDetailResponse toProductDetailResponse(SanPham sanPham, List<ProductVariantResponse> variants) {
        return ProductDetailResponse.builder()
                .id(sanPham.getId())
                .maSanPham(sanPham.getMa())
                .tenSanPham(sanPham.getTen())
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

    /** Map bien the: giaGoc la gia luu DB, giaBan la gia sau dot giam gia dang hieu luc. */
    public ProductVariantResponse toVariantResponse(ChiTietSanPham variant, List<ProductVariantImageResponse> images) {
        BigDecimal originalPrice = variant.getGiaBan() != null ? variant.getGiaBan() : BigDecimal.ZERO;
        BigDecimal discountedPrice = DiscountPriceUtils.calculateDiscountedPrice(originalPrice, variant.getChiTietDotGiamGias());
        BigDecimal activeDiscountPercent = DiscountPriceUtils.getActiveDiscountPercent(variant.getChiTietDotGiamGias());

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .idSanPham(variant.getSanPham() != null ? variant.getSanPham().getId() : null)
                .maSanPham(variant.getSanPham() != null ? variant.getSanPham().getMa() : null)
                .tenSanPham(variant.getSanPham() != null ? variant.getSanPham().getTen() : null)
                .tenSanPhamDayDu(variant.getSanPham() != null 
                    ? String.format("%s [%s - %s]", 
                        variant.getSanPham().getTen(), 
                        variant.getMauSac() != null ? variant.getMauSac().getTen() : "?",
                        variant.getKichThuoc() != null ? variant.getKichThuoc().getTen() : "?") 
                    : null)
                .tenThuongHieu(variant.getSanPham() != null && variant.getSanPham().getThuongHieu() != null ? variant.getSanPham().getThuongHieu().getTen() : null)
                .tenChatLieu(variant.getSanPham() != null && variant.getSanPham().getChatLieu() != null ? variant.getSanPham().getChatLieu().getTen() : null)
                .hinhAnh(variant.getSanPham() != null ? variant.getSanPham().getHinhAnh() : null)
                .maChiTietSanPham(variant.getMaChiTietSanPham())
                .idMauSac(variant.getMauSac() != null ? variant.getMauSac().getId() : null)
                .tenMauSac(variant.getMauSac() != null ? variant.getMauSac().getTen() : null)
                .maMauHex(variant.getMauSac() != null ? variant.getMauSac().getMaMauHex() : null)
                .idKichThuoc(variant.getKichThuoc() != null ? variant.getKichThuoc().getId() : null)
                .tenKichThuoc(variant.getKichThuoc() != null ? variant.getKichThuoc().getTen() : null)
                .giaTriKichThuoc(variant.getKichThuoc() != null ? variant.getKichThuoc().getGiaTriKichThuoc() : null)
                .soLuong(variant.getSoLuong())
                .giaNhap(variant.getGiaNhap())
                .giaGoc(activeDiscountPercent.compareTo(BigDecimal.ZERO) > 0 ? originalPrice : null)
                .giaBan(discountedPrice)
                .phanTramGiam(activeDiscountPercent)
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
