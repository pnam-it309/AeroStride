package com.example.be.core.admin.hoadon.mapper;

import com.example.be.core.admin.hoadon.model.response.*;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.constants.PaymentConstants;
import com.example.be.infrastructure.constants.TrangThai;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminHoaDonMapper {

    @Mapping(target = "tenKhachHang", expression = "java(hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getTen() != null && !hoaDon.getKhachHang().getTen().isBlank() ? hoaDon.getKhachHang().getTen() : hoaDon.getTenNguoiNhan())")
    @Mapping(target = "maKhachHang", source = "khachHang.ma")
    @Mapping(target = "soDienThoaiKhachHang", expression = "java(hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getSdt() != null && !hoaDon.getKhachHang().getSdt().isBlank() ? hoaDon.getKhachHang().getSdt() : hoaDon.getSoDienThoaiNguoiNhan())")
    @Mapping(target = "emailKhachHang", expression = "java(hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getEmail() != null && !hoaDon.getKhachHang().getEmail().isBlank() ? hoaDon.getKhachHang().getEmail() : hoaDon.getEmailNguoiNhan())")
    @Mapping(target = "ngayTaoKhachHang", source = "khachHang.ngayTao")
    @Mapping(target = "tenNhanVien", source = "nhanVien.ten")
    @Mapping(target = "maNhanVien", source = "nhanVien.ma")
    @Mapping(target = "listsHoaDonChiTiet", source = "listsHoaDonChiTiet")
    @Mapping(target = "listsLichSuHoaDon", source = "listsLichSuHoaDon")
    @Mapping(target = "listsGiaoDichThanhToan", source = "listsGiaoDichThanhToan")
    @Mapping(target = "maPhieuGiamGia", expression = "java(hoaDon.getPhieuGiamGia() != null ? hoaDon.getPhieuGiamGia().getMa() : (hoaDon.getPhieuGiamGiaCaNhan() != null ? hoaDon.getPhieuGiamGiaCaNhan().getPhieuGiamGia().getMa() : null))")
    @Mapping(target = "tenPhieuGiamGia", expression = "java(hoaDon.getPhieuGiamGia() != null ? hoaDon.getPhieuGiamGia().getTen() : (hoaDon.getPhieuGiamGiaCaNhan() != null ? hoaDon.getPhieuGiamGiaCaNhan().getPhieuGiamGia().getTen() : null))")
    @Mapping(target = "canHoanPhi", expression = "java(canHoanPhi(hoaDon))")
    AdminHoaDonDetailResponse toDetailResponse(HoaDon hoaDon);

    @Mapping(target = "idCtsp", source = "chiTietSanPham.id")
    @Mapping(target = "tenSanPham", source = "chiTietSanPham.sanPham.ten")
    @Mapping(target = "maSanPham", source = "chiTietSanPham.sanPham.ma")
    @Mapping(target = "maChiTietSanPham", source = "chiTietSanPham.maChiTietSanPham")
    @Mapping(target = "mauSac", source = "chiTietSanPham.mauSac.ten")
    @Mapping(target = "kichThuoc", source = "chiTietSanPham.kichThuoc.ten")
    @Mapping(target = "giaHienTai", source = "chiTietSanPham.giaBan")
    @Mapping(target = "giaGoc", expression = "java(detail.getChiTietSanPham() != null ? detail.getChiTietSanPham().getGiaBan() : null)")
    @Mapping(target = "phanTramGiam", expression = "java(calculatePhanTramGiam(detail))")
    @Mapping(target = "tenDotGiamGia", expression = "java(getTenDotGiamGia(detail))")
    @Mapping(target = "hinhAnh", source = "chiTietSanPham", qualifiedByName = "getThumbnail")
    AdminHoaDonChiTietResponse toChiTietResponse(HoaDonChiTiet detail);

    AdminLichSuHoaDonResponse toLichSuResponse(LichSuTrangThaiHoaDon history);

    @Mapping(target = "tenPhuongThuc", expression = "java(getTenPhuongThuc(payment))")
    @Mapping(target = "trangThai", source = "trangThai", qualifiedByName = "mapTrangThai")
    @Mapping(target = "nguoiXacNhan", source = "nguoiTao")
    AdminGiaoDichThanhToanResponse toGiaoDichResponse(GiaoDichThanhToan payment);

    default String getTenPhuongThuc(GiaoDichThanhToan payment) {
        if (payment == null) return "Chưa xác định";
        if (payment.getPhuongThucThanhToan() != null && payment.getPhuongThucThanhToan().getTen() != null && !payment.getPhuongThucThanhToan().getTen().isBlank()) {
            return payment.getPhuongThucThanhToan().getTen();
        }
        String loai = payment.getLoaiGiaoDich();
        if (loai != null && !loai.isBlank()) {
            if (PaymentConstants.METHOD_TIEN_MAT.equalsIgnoreCase(loai)) return "Tiền mặt";
            if (PaymentConstants.METHOD_COD.equalsIgnoreCase(loai)) return "Thanh toán khi nhận hàng (COD)";
            if (PaymentConstants.METHOD_VNPAY.equalsIgnoreCase(loai)) return "VNPay";
            if (PaymentConstants.METHOD_CHUYEN_KHOAN.equalsIgnoreCase(loai) || PaymentConstants.METHOD_ONLINE.equalsIgnoreCase(loai)) {
                if (payment.getGhiChu() != null && payment.getGhiChu().toUpperCase().contains(PaymentConstants.METHOD_VNPAY)) {
                    return "VNPay";
                }
                return "Chuyển khoản";
            }
            return loai;
        }
        if (payment.getGhiChu() != null) {
            String noteUpper = payment.getGhiChu().toUpperCase();
            if (noteUpper.contains(PaymentConstants.METHOD_VNPAY)) return "VNPay";
            if (noteUpper.contains("TIỀN MẶT") || noteUpper.contains(PaymentConstants.METHOD_TIEN_MAT) || noteUpper.contains(PaymentConstants.METHOD_COD)) return "Tiền mặt";
            if (noteUpper.contains("CHUYỂN KHOẢN") || noteUpper.contains(PaymentConstants.METHOD_CHUYEN_KHOAN)) return "Chuyển khoản";
        }
        return "Chuyển khoản";
    }

    @Named("getThumbnail")
    default String getThumbnail(ChiTietSanPham ctsp) {
        if (ctsp == null) {
            return null;
        }
        if (ctsp.getAnhChiTietSanPhams() != null && !ctsp.getAnhChiTietSanPhams().isEmpty()) {
            for (AnhChiTietSanPham img : ctsp.getAnhChiTietSanPhams()) {
                if (Boolean.TRUE.equals(img.getHinhAnhDaiDien()) && !Boolean.TRUE.equals(img.getXoaMem()) && img.getDuongDanAnh() != null && !img.getDuongDanAnh().trim().isEmpty()) {
                    return img.getDuongDanAnh();
                }
            }
            for (AnhChiTietSanPham img : ctsp.getAnhChiTietSanPhams()) {
                if (!Boolean.TRUE.equals(img.getXoaMem()) && img.getDuongDanAnh() != null && !img.getDuongDanAnh().trim().isEmpty()) {
                    return img.getDuongDanAnh();
                }
            }
        }
        return ctsp.getSanPham() != null ? ctsp.getSanPham().getHinhAnh() : null;
    }

    @Named("mapTrangThai")
    default Integer mapTrangThai(TrangThai status) {
        if (status == null) {
            return null;
        }
        return status.ordinal();
    }

    default boolean isOnlineOrder(HoaDon hoaDon) {
        if (hoaDon.getOrderType() != null) {
            return hoaDon.getOrderType() == OrderType.ONLINE;
        }
        return hoaDon.getNhanVien() == null && OrderType.ONLINE.name().equalsIgnoreCase(hoaDon.getLoaiDon());
    }

    // Đơn cần xác nhận hoàn phí: đã hủy + (đơn online hoặc trả trước không phải COD) + chưa hoàn phí
    default boolean canHoanPhi(HoaDon hoaDon) {
        if (hoaDon.getTrangThai() != OrderStatus.DA_HUY || Boolean.TRUE.equals(hoaDon.getDaHoanPhi())) {
            return false;
        }
        if (isOnlineOrder(hoaDon)) {
            return true;
        }
        if (hoaDon.getListsGiaoDichThanhToan() != null) {
            for (GiaoDichThanhToan gd : hoaDon.getListsGiaoDichThanhToan()) {
                // Có ít nhất 1 giao dịch trả trước (không phải COD/tiền mặt) => cần hoàn tiền
                if (!PaymentConstants.isCashOrCod(gd)) {
                    return true;
                }
            }
        }
        return false;
    }

    default Integer calculatePhanTramGiam(HoaDonChiTiet detail) {
        if (detail == null || detail.getChiTietSanPham() == null || detail.getChiTietSanPham().getGiaBan() == null || detail.getDonGia() == null) {
            return null;
        }
        java.math.BigDecimal giaGoc = detail.getChiTietSanPham().getGiaBan();
        java.math.BigDecimal donGia = detail.getDonGia();
        if (giaGoc.compareTo(donGia) > 0 && giaGoc.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return giaGoc.subtract(donGia)
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .divide(giaGoc, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }
        return null;
    }

    default String getTenDotGiamGia(HoaDonChiTiet detail) {
        if (detail == null || detail.getChiTietSanPham() == null) {
            return null;
        }
        if (detail.getChiTietSanPham().getChiTietDotGiamGias() != null && !detail.getChiTietSanPham().getChiTietDotGiamGias().isEmpty()) {
            return com.example.be.utils.DiscountPriceUtils.getActiveDiscountName(detail.getChiTietSanPham().getChiTietDotGiamGias());
        }
        return null;
    }
}
