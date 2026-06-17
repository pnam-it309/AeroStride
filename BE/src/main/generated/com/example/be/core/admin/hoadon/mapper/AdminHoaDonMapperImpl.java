package com.example.be.core.admin.hoadon.mapper;

import com.example.be.core.admin.hoadon.model.response.AdminGiaoDichThanhToanResponse;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonChiTietResponse;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonDetailResponse;
import com.example.be.core.admin.hoadon.model.response.AdminLichSuHoaDonResponse;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.GiaoDichThanhToan;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.entity.KhachHang;
import com.example.be.entity.KichThuoc;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.entity.MauSac;
import com.example.be.entity.NhanVien;
import com.example.be.entity.PhuongThucThanhToan;
import com.example.be.entity.SanPham;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-09T10:37:18+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class AdminHoaDonMapperImpl implements AdminHoaDonMapper {

    @Override
    public AdminHoaDonDetailResponse toDetailResponse(HoaDon hoaDon) {
        if ( hoaDon == null ) {
            return null;
        }

        AdminHoaDonDetailResponse.AdminHoaDonDetailResponseBuilder adminHoaDonDetailResponse = AdminHoaDonDetailResponse.builder();

        adminHoaDonDetailResponse.tenKhachHang( hoaDonKhachHangTen( hoaDon ) );
        adminHoaDonDetailResponse.soDienThoaiKhachHang( hoaDonKhachHangSdt( hoaDon ) );
        adminHoaDonDetailResponse.emailKhachHang( hoaDonKhachHangEmail( hoaDon ) );
        adminHoaDonDetailResponse.tenNhanVien( hoaDonNhanVienTen( hoaDon ) );
        adminHoaDonDetailResponse.maNhanVien( hoaDonNhanVienMa( hoaDon ) );
        adminHoaDonDetailResponse.listsHoaDonChiTiet( hoaDonChiTietSetToAdminHoaDonChiTietResponseList( hoaDon.getListsHoaDonChiTiet() ) );
        adminHoaDonDetailResponse.listsLichSuHoaDon( lichSuTrangThaiHoaDonSetToAdminLichSuHoaDonResponseList( hoaDon.getListsLichSuHoaDon() ) );
        adminHoaDonDetailResponse.listsGiaoDichThanhToan( giaoDichThanhToanSetToAdminGiaoDichThanhToanResponseList( hoaDon.getListsGiaoDichThanhToan() ) );
        adminHoaDonDetailResponse.id( hoaDon.getId() );
        adminHoaDonDetailResponse.maHoaDon( hoaDon.getMaHoaDon() );
        adminHoaDonDetailResponse.trangThai( hoaDon.getTrangThai() );
        adminHoaDonDetailResponse.loaiDon( hoaDon.getLoaiDon() );
        adminHoaDonDetailResponse.phiVanChuyen( hoaDon.getPhiVanChuyen() );
        adminHoaDonDetailResponse.tongTien( hoaDon.getTongTien() );
        adminHoaDonDetailResponse.tongTienSauGiam( hoaDon.getTongTienSauGiam() );
        adminHoaDonDetailResponse.tienNguoiMua( hoaDon.getTienNguoiMua() );
        adminHoaDonDetailResponse.diaChiNguoiNhan( hoaDon.getDiaChiNguoiNhan() );
        adminHoaDonDetailResponse.soDienThoaiNguoiNhan( hoaDon.getSoDienThoaiNguoiNhan() );
        adminHoaDonDetailResponse.ngayDuKienNhan( hoaDon.getNgayDuKienNhan() );
        adminHoaDonDetailResponse.ghiChu( hoaDon.getGhiChu() );
        adminHoaDonDetailResponse.ngayTao( hoaDon.getNgayTao() );
        adminHoaDonDetailResponse.ngayCapNhat( hoaDon.getNgayCapNhat() );

        adminHoaDonDetailResponse.maPhieuGiamGia( hoaDon.getPhieuGiamGia() != null ? hoaDon.getPhieuGiamGia().getMa() : (hoaDon.getPhieuGiamGiaCaNhan() != null ? hoaDon.getPhieuGiamGiaCaNhan().getPhieuGiamGia().getMa() : null) );
        adminHoaDonDetailResponse.tenPhieuGiamGia( hoaDon.getPhieuGiamGia() != null ? hoaDon.getPhieuGiamGia().getTen() : (hoaDon.getPhieuGiamGiaCaNhan() != null ? hoaDon.getPhieuGiamGiaCaNhan().getPhieuGiamGia().getTen() : null) );

        return adminHoaDonDetailResponse.build();
    }

    @Override
    public AdminHoaDonChiTietResponse toChiTietResponse(HoaDonChiTiet detail) {
        if ( detail == null ) {
            return null;
        }

        AdminHoaDonChiTietResponse.AdminHoaDonChiTietResponseBuilder adminHoaDonChiTietResponse = AdminHoaDonChiTietResponse.builder();

        adminHoaDonChiTietResponse.idCtsp( detailChiTietSanPhamId( detail ) );
        adminHoaDonChiTietResponse.tenSanPham( detailChiTietSanPhamSanPhamTen( detail ) );
        adminHoaDonChiTietResponse.maSanPham( detailChiTietSanPhamSanPhamMa( detail ) );
        adminHoaDonChiTietResponse.maChiTietSanPham( detailChiTietSanPhamMaChiTietSanPham( detail ) );
        adminHoaDonChiTietResponse.mauSac( detailChiTietSanPhamMauSacTen( detail ) );
        adminHoaDonChiTietResponse.kichThuoc( detailChiTietSanPhamKichThuocTen( detail ) );
        adminHoaDonChiTietResponse.hinhAnh( getThumbnail( detail.getChiTietSanPham() ) );
        adminHoaDonChiTietResponse.id( detail.getId() );
        adminHoaDonChiTietResponse.soLuong( detail.getSoLuong() );
        adminHoaDonChiTietResponse.donGia( detail.getDonGia() );

        return adminHoaDonChiTietResponse.build();
    }

    @Override
    public AdminLichSuHoaDonResponse toLichSuResponse(LichSuTrangThaiHoaDon history) {
        if ( history == null ) {
            return null;
        }

        AdminLichSuHoaDonResponse.AdminLichSuHoaDonResponseBuilder adminLichSuHoaDonResponse = AdminLichSuHoaDonResponse.builder();

        adminLichSuHoaDonResponse.id( history.getId() );
        adminLichSuHoaDonResponse.trangThaiCu( history.getTrangThaiCu() );
        adminLichSuHoaDonResponse.trangThaiMoi( history.getTrangThaiMoi() );
        adminLichSuHoaDonResponse.ghiChu( history.getGhiChu() );
        adminLichSuHoaDonResponse.nguoiThucHien( history.getNguoiThucHien() );
        adminLichSuHoaDonResponse.ngayTao( history.getNgayTao() );

        return adminLichSuHoaDonResponse.build();
    }

    @Override
    public AdminGiaoDichThanhToanResponse toGiaoDichResponse(GiaoDichThanhToan payment) {
        if ( payment == null ) {
            return null;
        }

        AdminGiaoDichThanhToanResponse.AdminGiaoDichThanhToanResponseBuilder adminGiaoDichThanhToanResponse = AdminGiaoDichThanhToanResponse.builder();

        adminGiaoDichThanhToanResponse.tenPhuongThuc( paymentPhuongThucThanhToanTen( payment ) );
        adminGiaoDichThanhToanResponse.trangThai( mapTrangThai( payment.getTrangThai() ) );
        adminGiaoDichThanhToanResponse.nguoiXacNhan( payment.getNguoiTao() );
        adminGiaoDichThanhToanResponse.id( payment.getId() );
        adminGiaoDichThanhToanResponse.soTien( payment.getSoTien() );
        adminGiaoDichThanhToanResponse.loaiGiaoDich( payment.getLoaiGiaoDich() );
        adminGiaoDichThanhToanResponse.maGiaoDichNgoai( payment.getMaGiaoDichNgoai() );
        adminGiaoDichThanhToanResponse.ghiChu( payment.getGhiChu() );
        adminGiaoDichThanhToanResponse.ngayTao( payment.getNgayTao() );

        return adminGiaoDichThanhToanResponse.build();
    }

    private String hoaDonKhachHangTen(HoaDon hoaDon) {
        if ( hoaDon == null ) {
            return null;
        }
        KhachHang khachHang = hoaDon.getKhachHang();
        if ( khachHang == null ) {
            return null;
        }
        String ten = khachHang.getTen();
        if ( ten == null ) {
            return null;
        }
        return ten;
    }

    private String hoaDonKhachHangSdt(HoaDon hoaDon) {
        if ( hoaDon == null ) {
            return null;
        }
        KhachHang khachHang = hoaDon.getKhachHang();
        if ( khachHang == null ) {
            return null;
        }
        String sdt = khachHang.getSdt();
        if ( sdt == null ) {
            return null;
        }
        return sdt;
    }

    private String hoaDonKhachHangEmail(HoaDon hoaDon) {
        if ( hoaDon == null ) {
            return null;
        }
        KhachHang khachHang = hoaDon.getKhachHang();
        if ( khachHang == null ) {
            return null;
        }
        String email = khachHang.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    private String hoaDonNhanVienTen(HoaDon hoaDon) {
        if ( hoaDon == null ) {
            return null;
        }
        NhanVien nhanVien = hoaDon.getNhanVien();
        if ( nhanVien == null ) {
            return null;
        }
        String ten = nhanVien.getTen();
        if ( ten == null ) {
            return null;
        }
        return ten;
    }

    private String hoaDonNhanVienMa(HoaDon hoaDon) {
        if ( hoaDon == null ) {
            return null;
        }
        NhanVien nhanVien = hoaDon.getNhanVien();
        if ( nhanVien == null ) {
            return null;
        }
        String ma = nhanVien.getMa();
        if ( ma == null ) {
            return null;
        }
        return ma;
    }

    protected List<AdminHoaDonChiTietResponse> hoaDonChiTietSetToAdminHoaDonChiTietResponseList(Set<HoaDonChiTiet> set) {
        if ( set == null ) {
            return null;
        }

        List<AdminHoaDonChiTietResponse> list = new ArrayList<AdminHoaDonChiTietResponse>( set.size() );
        for ( HoaDonChiTiet hoaDonChiTiet : set ) {
            list.add( toChiTietResponse( hoaDonChiTiet ) );
        }

        return list;
    }

    protected List<AdminLichSuHoaDonResponse> lichSuTrangThaiHoaDonSetToAdminLichSuHoaDonResponseList(Set<LichSuTrangThaiHoaDon> set) {
        if ( set == null ) {
            return null;
        }

        List<AdminLichSuHoaDonResponse> list = new ArrayList<AdminLichSuHoaDonResponse>( set.size() );
        for ( LichSuTrangThaiHoaDon lichSuTrangThaiHoaDon : set ) {
            list.add( toLichSuResponse( lichSuTrangThaiHoaDon ) );
        }

        return list;
    }

    protected List<AdminGiaoDichThanhToanResponse> giaoDichThanhToanSetToAdminGiaoDichThanhToanResponseList(Set<GiaoDichThanhToan> set) {
        if ( set == null ) {
            return null;
        }

        List<AdminGiaoDichThanhToanResponse> list = new ArrayList<AdminGiaoDichThanhToanResponse>( set.size() );
        for ( GiaoDichThanhToan giaoDichThanhToan : set ) {
            list.add( toGiaoDichResponse( giaoDichThanhToan ) );
        }

        return list;
    }

    private String detailChiTietSanPhamId(HoaDonChiTiet hoaDonChiTiet) {
        if ( hoaDonChiTiet == null ) {
            return null;
        }
        ChiTietSanPham chiTietSanPham = hoaDonChiTiet.getChiTietSanPham();
        if ( chiTietSanPham == null ) {
            return null;
        }
        String id = chiTietSanPham.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String detailChiTietSanPhamSanPhamTen(HoaDonChiTiet hoaDonChiTiet) {
        if ( hoaDonChiTiet == null ) {
            return null;
        }
        ChiTietSanPham chiTietSanPham = hoaDonChiTiet.getChiTietSanPham();
        if ( chiTietSanPham == null ) {
            return null;
        }
        SanPham sanPham = chiTietSanPham.getSanPham();
        if ( sanPham == null ) {
            return null;
        }
        String ten = sanPham.getTen();
        if ( ten == null ) {
            return null;
        }
        return ten;
    }

    private String detailChiTietSanPhamSanPhamMa(HoaDonChiTiet hoaDonChiTiet) {
        if ( hoaDonChiTiet == null ) {
            return null;
        }
        ChiTietSanPham chiTietSanPham = hoaDonChiTiet.getChiTietSanPham();
        if ( chiTietSanPham == null ) {
            return null;
        }
        SanPham sanPham = chiTietSanPham.getSanPham();
        if ( sanPham == null ) {
            return null;
        }
        String ma = sanPham.getMa();
        if ( ma == null ) {
            return null;
        }
        return ma;
    }

    private String detailChiTietSanPhamMaChiTietSanPham(HoaDonChiTiet hoaDonChiTiet) {
        if ( hoaDonChiTiet == null ) {
            return null;
        }
        ChiTietSanPham chiTietSanPham = hoaDonChiTiet.getChiTietSanPham();
        if ( chiTietSanPham == null ) {
            return null;
        }
        String maChiTietSanPham = chiTietSanPham.getMaChiTietSanPham();
        if ( maChiTietSanPham == null ) {
            return null;
        }
        return maChiTietSanPham;
    }

    private String detailChiTietSanPhamMauSacTen(HoaDonChiTiet hoaDonChiTiet) {
        if ( hoaDonChiTiet == null ) {
            return null;
        }
        ChiTietSanPham chiTietSanPham = hoaDonChiTiet.getChiTietSanPham();
        if ( chiTietSanPham == null ) {
            return null;
        }
        MauSac mauSac = chiTietSanPham.getMauSac();
        if ( mauSac == null ) {
            return null;
        }
        String ten = mauSac.getTen();
        if ( ten == null ) {
            return null;
        }
        return ten;
    }

    private String detailChiTietSanPhamKichThuocTen(HoaDonChiTiet hoaDonChiTiet) {
        if ( hoaDonChiTiet == null ) {
            return null;
        }
        ChiTietSanPham chiTietSanPham = hoaDonChiTiet.getChiTietSanPham();
        if ( chiTietSanPham == null ) {
            return null;
        }
        KichThuoc kichThuoc = chiTietSanPham.getKichThuoc();
        if ( kichThuoc == null ) {
            return null;
        }
        String ten = kichThuoc.getTen();
        if ( ten == null ) {
            return null;
        }
        return ten;
    }

    private String paymentPhuongThucThanhToanTen(GiaoDichThanhToan giaoDichThanhToan) {
        if ( giaoDichThanhToan == null ) {
            return null;
        }
        PhuongThucThanhToan phuongThucThanhToan = giaoDichThanhToan.getPhuongThucThanhToan();
        if ( phuongThucThanhToan == null ) {
            return null;
        }
        String ten = phuongThucThanhToan.getTen();
        if ( ten == null ) {
            return null;
        }
        return ten;
    }
}
