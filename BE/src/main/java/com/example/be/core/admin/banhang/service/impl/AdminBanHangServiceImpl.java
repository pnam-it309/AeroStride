package com.example.be.core.admin.banhang.service.impl;

import com.example.be.core.admin.banhang.model.request.AdminBanHangCheckoutRequest;
import com.example.be.core.admin.banhang.model.request.AdminBanHangHoaDonChiTietRequest;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonChiTietResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangKhachHangResponse;
import com.example.be.core.admin.banhang.model.response.BanHangSanPhamResponse;
import com.example.be.core.admin.banhang.repository.*;
import com.example.be.core.admin.banhang.service.AdminBanHangService;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import com.example.be.utils.HelperUtils;
import com.example.be.utils.MaGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBanHangServiceImpl implements AdminBanHangService {

    private final AdminBanHangHoaDonRepository hoaDonRepository;
    private final AdminBanHangHoaDonChiTietRepository hoaDonChiTietRepository;
    private final AdminBanHangChiTietSanPhamRepository chiTietSanPhamRepository;
    private final AdminBanHangKhachHangRepository khachHangRepository;
    private final AdminBanHangPhieuGiamGiaRepository phieuGiamGiaRepository;
    private final AdminBanHangGiaoDichThanhToanRepository giaoDichThanhToanRepository;
    private final AdminBanHangPhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Override
    public List<AdminBanHangHoaDonResponse> getHoaDonCho() {
        List<HoaDon> list = hoaDonRepository.findAllByTrangThaiAndLoaiDon(OrderStatus.PENDING_PAYMENT, "TAI_QUAY");
        return list.stream().map(this::mapToHoaDonResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse createHoaDon() {
        long count = hoaDonRepository.countByTrangThaiAndLoaiDon(OrderStatus.PENDING_PAYMENT, "TAI_QUAY");
        if (count >= 5) {
            throw new RestApiException("Tối đa 5 hóa đơn chờ.");
        }
        HoaDon hoaDon = new HoaDon();
        hoaDon.setId(HelperUtils.generateUUID());
        hoaDon.setMaHoaDon(MaGenerator.generate(HoaDon.class));
        hoaDon.setTrangThai(OrderStatus.PENDING_PAYMENT);
        hoaDon.setLoaiDon("TAI_QUAY");
        hoaDon.setNgayTao(System.currentTimeMillis());
        hoaDon.setTongTien(BigDecimal.ZERO);
        hoaDon.setTongTienSauGiam(BigDecimal.ZERO);
        hoaDonRepository.save(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    @Transactional
    public void deleteHoaDon(String id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new RestApiException("Không tìm thấy hóa đơn."));
        hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findAllByHoaDon(hd));
        hoaDonRepository.delete(hd);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse addSanPham(String idHoaDon, AdminBanHangHoaDonChiTietRequest request) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new RestApiException("Hóa đơn không tồn tại."));
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(request.getIdChiTietSanPham())
                .orElseThrow(() -> new RestApiException("Sản phẩm không tồn tại."));

        if (ctsp.getSoLuong() < request.getSoLuong()) {
            throw new RestApiException("Sản phẩm không đủ số lượng.");
        }

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDonAndChiTietSanPham(hoaDon, ctsp);
        if (hdct != null) {
            hdct.setSoLuong(hdct.getSoLuong() + request.getSoLuong());
        } else {
            hdct = HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(ctsp.getGiaBan()) // Luôn lấy giá hiện tại khi thêm vào
                    .build();
            // ID handled automatically? Let's be safe.
            hdct.setId(HelperUtils.generateUUID());
            hdct.setTrangThai(TrangThai.DANG_HOAT_DONG);
            hdct.setNgayTao(System.currentTimeMillis());
        }
        hoaDonChiTietRepository.save(hdct);

        updateHoaDonTotals(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse updateSoLuong(String idHoaDon, String idHoaDonChiTiet, Integer soLuong) {
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHoaDonChiTiet).orElseThrow(() -> new RestApiException("Không tìm thấy sản phẩm trong hóa đơn."));
        if (soLuong <= 0) {
            hoaDonChiTietRepository.delete(hdct);
        } else {
            if (hdct.getChiTietSanPham().getSoLuong() < soLuong) {
                throw new RestApiException("Sản phẩm không đủ số lượng.");
            }
            hdct.setSoLuong(soLuong);
            hoaDonChiTietRepository.save(hdct);
        }
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new RestApiException("Hóa đơn không tồn tại."));
        updateHoaDonTotals(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    public void removeHoaDonChiTiet(String idHoaDon, String idHoaDonChiTiet) {
        hoaDonChiTietRepository.deleteById(idHoaDonChiTiet);
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElse(null);
        if (hoaDon != null) {
            updateHoaDonTotals(hoaDon);
        }
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse setKhachHang(String idHoaDon, String idKhachHang) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new RestApiException("Hóa đơn không tồn tại."));
        KhachHang khachHang = null;
        if (idKhachHang != null && !idKhachHang.isEmpty()) {
            khachHang = khachHangRepository.findById(idKhachHang).orElseThrow(() -> new RestApiException("Khách hàng không tồn tại."));
        }
        hoaDon.setKhachHang(khachHang);
        hoaDonRepository.save(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse setPhieuGiamGia(String idHoaDon, String idPhieuGiamGia) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new RestApiException("Hóa đơn không tồn tại."));
        PhieuGiamGia voucher = null;
        if (idPhieuGiamGia != null && !idPhieuGiamGia.isEmpty()) {
            voucher = phieuGiamGiaRepository.findById(idPhieuGiamGia).orElseThrow(() -> new RestApiException("Voucher không tồn tại."));
        }
        hoaDon.setPhieuGiamGia(voucher);
        hoaDonRepository.save(hoaDon);
        updateHoaDonTotals(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    @Transactional
    public void checkout(String idHoaDon, AdminBanHangCheckoutRequest request) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new RestApiException("Hóa đơn không tồn tại."));
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
        if (details.isEmpty()) {
            throw new RestApiException("Hóa đơn trống.");
        }

        // Logic trừ tồn kho và validate Stock
        for (HoaDonChiTiet d : details) {
            ChiTietSanPham ct = d.getChiTietSanPham();
            if (ct.getSoLuong() < d.getSoLuong()) {
                throw new RestApiException("Sản phẩm " + ct.getSanPham().getTen() + " không đủ tồn kho.");
            }
            ct.setSoLuong(ct.getSoLuong() - d.getSoLuong());
            chiTietSanPhamRepository.save(ct);
        }

        // Cập nhật thông tin hóa đơn
        hoaDon.setTrangThai(OrderStatus.DELIVERED); // TAI_QUAY chốt xong là Delivered hoặc Processing? 
        // POS usually means delivered immediately.
        hoaDon.setLoaiDon(request.getLoaiDon());
        hoaDon.setPhiVanChuyen(request.getPhiVanChuyen() != null ? request.getPhiVanChuyen() : BigDecimal.ZERO);
        hoaDon.setTongTien(request.getTongTien());
        hoaDon.setTongTienSauGiam(request.getTongTienSauGiam());
        hoaDon.setGhiChu(request.getGhiChu());
        hoaDon.setNgayCapNhat(System.currentTimeMillis());
        hoaDonRepository.save(hoaDon);

        // Xử lý thanh toán (Mixed)
        if (request.getTienMat() != null && request.getTienMat().compareTo(BigDecimal.ZERO) > 0) {
            createGiaoDich(hoaDon, "TIEN_MAT", request.getTienMat(), null);
        }
        if (request.getTienChuyenKhoan() != null && request.getTienChuyenKhoan().compareTo(BigDecimal.ZERO) > 0) {
            createGiaoDich(hoaDon, "CHUYEN_KHOAN", request.getTienChuyenKhoan(), request.getMaGiaoDich());
        }
    }

    @Override
    public List<BanHangSanPhamResponse> searchSanPham(String keyword) {
        // Simple search logic
        List<ChiTietSanPham> list = chiTietSanPhamRepository.searchByKeyword(keyword);
        return list.stream().map(ct -> BanHangSanPhamResponse.builder()
                .id(ct.getId())
                .tenSanPham(ct.getSanPham().getTen())
                .maChiTietSanPham(ct.getMaChiTietSanPham())
                .tenMauSac(ct.getMauSac().getTen())
                .tenKichThuoc(ct.getKichThuoc().getTen())
                .soLuongTon(ct.getSoLuong())
                .giaBan(ct.getGiaBan())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<AdminBanHangKhachHangResponse> searchKhachHang(String keyword) {
        List<KhachHang> list = khachHangRepository.searchByKeyword(keyword);
        return list.stream().map(kh -> AdminBanHangKhachHangResponse.builder()
                .id(kh.getId())
                .tenKhachHang(kh.getTen())
                .sdt(kh.getSdt())
                .email(kh.getEmail())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<PhieuGiamGia> getVouchers(BigDecimal tongTien) {
        return phieuGiamGiaRepository.findAllByTrangThaiAndDonHangToiThieuLessThanEqual(
                TrangThai.DANG_HOAT_DONG, tongTien);
    }

    // Help Methods
    private void updateHoaDonTotals(HoaDon hoaDon) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
        BigDecimal total = details.stream()
                .map(d -> d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        hoaDon.setTongTien(total);
        
        BigDecimal discounted = total;
        if (hoaDon.getPhieuGiamGia() != null) {
            PhieuGiamGia v = hoaDon.getPhieuGiamGia();
            if (total.compareTo(v.getDonHangToiThieu()) >= 0) {
                // Calculation logic
                BigDecimal val = BigDecimal.ZERO;
                if ("PERCENT".equals(v.getLoaiPhieu())) {
                    val = total.multiply(BigDecimal.valueOf(v.getPhanTramGiamGia())).divide(BigDecimal.valueOf(100));
                    if (v.getGiamToiDa() != null && val.compareTo(v.getGiamToiDa()) > 0) {
                        val = v.getGiamToiDa();
                    }
                } else {
                    val = v.getSoTienGiam();
                }
                discounted = total.subtract(val);
                if (discounted.compareTo(BigDecimal.ZERO) < 0) discounted = BigDecimal.ZERO;
            }
        }
        hoaDon.setTongTienSauGiam(discounted);
        hoaDonRepository.save(hoaDon);
    }

    private void createGiaoDich(HoaDon hoaDon, String maPTTT, BigDecimal soTien, String maGiaoDichNgoai) {
        PhuongThucThanhToan pt = phuongThucThanhToanRepository.findByMa(maPTTT);
        if (pt == null) {
            // Default setup if missing
            pt = new PhuongThucThanhToan(maPTTT, maPTTT.equals("TIEN_MAT") ? "Tiền mặt" : "Chuyển khoản");
            phuongThucThanhToanRepository.save(pt);
        }
        GiaoDichThanhToan gd = GiaoDichThanhToan.builder()
                .hoaDon(hoaDon)
                .phuongThucThanhToan(pt)
                .soTien(soTien)
                .maGiaoDichNgoai(maGiaoDichNgoai)
                .loaiGiaoDich("THANH_TOAN")
                .build();
        gd.setId(HelperUtils.generateUUID());
        gd.setTrangThai(TrangThai.DANG_HOAT_DONG);
        gd.setNgayTao(System.currentTimeMillis());
        giaoDichThanhToanRepository.save(gd);
    }

    private AdminBanHangHoaDonResponse mapToHoaDonResponse(HoaDon hd) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        List<AdminBanHangHoaDonChiTietResponse> detailDTOs = details.stream().map(d -> AdminBanHangHoaDonChiTietResponse.builder()
                .id(d.getId())
                .idChiTietSanPham(d.getChiTietSanPham().getId())
                .tenSanPham(d.getChiTietSanPham().getSanPham().getTen())
                .tenMauSac(d.getChiTietSanPham().getMauSac().getTen())
                .tenKichThuoc(d.getChiTietSanPham().getKichThuoc().getTen())
                .soLuong(d.getSoLuong())
                .donGia(d.getDonGia())
                .thanhTien(d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .build()).collect(Collectors.toList());

        return AdminBanHangHoaDonResponse.builder()
                .id(hd.getId())
                .maHoaDon(hd.getMaHoaDon())
                .idKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getId() : null)
                .tenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getTen() : "Khách lẻ")
                .sdtKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getSdt() : "")
                .idPhieuGiamGia(hd.getPhieuGiamGia() != null ? hd.getPhieuGiamGia().getId() : null)
                .tongTien(hd.getTongTien())
                .tongTienSauGiam(hd.getTongTienSauGiam())
                .listsHoaDonChiTiet(detailDTOs)
                .build();
    }
}
