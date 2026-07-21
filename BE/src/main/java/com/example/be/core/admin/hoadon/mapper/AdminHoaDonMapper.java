package com.example.be.core.admin.hoadon.mapper;

import com.example.be.core.admin.hoadon.model.response.*;
import com.example.be.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminHoaDonMapper {

    @Mapping(target = "tenKhachHang", source = "khachHang.ten")
    @Mapping(target = "maKhachHang", source = "khachHang.ma")
    @Mapping(target = "soDienThoaiKhachHang", source = "khachHang.sdt")
    @Mapping(target = "emailKhachHang", source = "khachHang.email")
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
    @Mapping(target = "hinhAnh", source = "chiTietSanPham", qualifiedByName = "getThumbnail")
    AdminHoaDonChiTietResponse toChiTietResponse(HoaDonChiTiet detail);

    AdminLichSuHoaDonResponse toLichSuResponse(LichSuTrangThaiHoaDon history);

    @Mapping(target = "tenPhuongThuc", source = "phuongThucThanhToan.ten")
    @Mapping(target = "trangThai", source = "trangThai", qualifiedByName = "mapTrangThai")
    @Mapping(target = "nguoiXacNhan", source = "nguoiTao")
    AdminGiaoDichThanhToanResponse toGiaoDichResponse(GiaoDichThanhToan payment);

    @Named("getThumbnail")
    default String getThumbnail(ChiTietSanPham ctsp) {
        if (ctsp == null || ctsp.getAnhChiTietSanPhams() == null || ctsp.getAnhChiTietSanPhams().isEmpty()) {
            return null;
        }
        return ctsp.getAnhChiTietSanPhams().iterator().next().getDuongDanAnh();
    }

    @Named("mapTrangThai")
    default Integer mapTrangThai(com.example.be.infrastructure.constants.TrangThai status) {
        if (status == null) {
            return null;
        }
        return status.ordinal();
    }

    // Đơn cần xác nhận hoàn phí: đã hủy + trả trước (không phải COD/tiền mặt) + chưa hoàn phí
    default boolean canHoanPhi(HoaDon hoaDon) {
        if (hoaDon.getTrangThai() != com.example.be.infrastructure.constants.OrderStatus.DA_HUY) {
            return false;
        }
        if (Boolean.TRUE.equals(hoaDon.getDaHoanPhi())) {
            return false;
        }
        if (hoaDon.getListsGiaoDichThanhToan() == null) {
            return false;
        }
        for (GiaoDichThanhToan gd : hoaDon.getListsGiaoDichThanhToan()) {
            String loai = gd.getLoaiGiaoDich();
            boolean isCod = loai != null && loai.equalsIgnoreCase("COD");
            String tenPt = gd.getPhuongThucThanhToan() != null ? gd.getPhuongThucThanhToan().getTen() : null;
            boolean tenIsCod = tenPt != null && (tenPt.toUpperCase().contains("COD") || tenPt.toUpperCase().contains("TIEN_MAT"));
            // Có ít nhất 1 giao dịch trả trước (không phải COD/tiền mặt) => cần hoàn tiền
            if (!isCod && !tenIsCod) {
                return true;
            }
        }
        return false;
    }
}
