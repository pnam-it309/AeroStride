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
    @Mapping(target = "soDienThoaiKhachHang", source = "khachHang.sdt")
    @Mapping(target = "emailKhachHang", source = "khachHang.email")
    @Mapping(target = "tenNhanVien", source = "nhanVien.ten")
    @Mapping(target = "maNhanVien", source = "nhanVien.ma")
    @Mapping(target = "listsHoaDonChiTiet", source = "listsHoaDonChiTiet")
    @Mapping(target = "listsLichSuHoaDon", source = "listsLichSuHoaDon")
    @Mapping(target = "listsGiaoDichThanhToan", source = "listsGiaoDichThanhToan")
    AdminHoaDonDetailResponse toDetailResponse(HoaDon hoaDon);

    @Mapping(target = "idCtsp", source = "chiTietSanPham.id")
    @Mapping(target = "tenSanPham", source = "chiTietSanPham.sanPham.ten")
    @Mapping(target = "maSanPham", source = "chiTietSanPham.sanPham.ma")
    @Mapping(target = "mauSac", source = "chiTietSanPham.mauSac.ten")
    @Mapping(target = "kichThuoc", source = "chiTietSanPham.kichThuoc.ten")
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
}
