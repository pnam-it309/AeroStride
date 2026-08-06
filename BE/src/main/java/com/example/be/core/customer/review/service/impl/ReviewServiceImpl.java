package com.example.be.core.customer.review.service.impl;

import com.example.be.core.customer.review.model.request.ReviewRequest;
import com.example.be.core.customer.review.service.ReviewService;
import com.example.be.entity.DanhGiaSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.KhachHang;
import com.example.be.entity.SanPham;
import com.example.be.repository.DanhGiaSanPhamRepository;
import com.example.be.repository.HoaDonRepository;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.SanPhamRepository;
import com.example.be.infrastructure.constants.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final DanhGiaSanPhamRepository danhGiaSanPhamRepository;
    private final HoaDonRepository hoaDonRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;

    @Override
    @Transactional
    public void submitReview(ReviewRequest request) {
        HoaDon hoaDon = null;
        if (request.getIdHoaDon() != null && !request.getIdHoaDon().isBlank()) {
            hoaDon = hoaDonRepository.findById(request.getIdHoaDon()).orElse(null);
        }
        
        SanPham sanPham = null;
        if (request.getIdSanPham() != null && !request.getIdSanPham().isBlank()) {
            sanPham = sanPhamRepository.findById(request.getIdSanPham()).orElse(null);
        }

        // Fallback: Nếu không truyền idSanPham trực tiếp nhưng có idHoaDon, lấy sản phẩm từ chi tiết hóa đơn
        if (sanPham == null && hoaDon != null && hoaDon.getListsHoaDonChiTiet() != null && !hoaDon.getListsHoaDonChiTiet().isEmpty()) {
            com.example.be.entity.HoaDonChiTiet firstHdct = hoaDon.getListsHoaDonChiTiet().iterator().next();
            sanPham = (firstHdct != null && firstHdct.getChiTietSanPham() != null) 
                    ? firstHdct.getChiTietSanPham().getSanPham() 
                    : null;
        }

        if (sanPham == null) {
            throw new RuntimeException("Không tìm thấy sản phẩm cần đánh giá.");
        }

        KhachHang khachHang = null;
        if (request.getIdKhachHang() != null && !request.getIdKhachHang().isBlank()) {
            khachHang = khachHangRepository.findById(request.getIdKhachHang()).orElse(null);
        }

        // Đơn đánh giá trực tiếp được tự động duyệt APPROVED để hiển thị trên chi tiết sản phẩm
        DanhGiaSanPham.TrangThaiDanhGia status = DanhGiaSanPham.TrangThaiDanhGia.APPROVED;

        DanhGiaSanPham review = DanhGiaSanPham.builder()
                .hoaDon(hoaDon)
                .sanPham(sanPham)
                .khachHang(khachHang)
                .diemDanhGia(request.getDiemDanhGia() != null ? request.getDiemDanhGia() : 5)
                .noiDung(request.getNoiDung())
                .hinhAnh(request.getHinhAnh())
                .video(request.getVideo())
                .trangThai(status)
                .build();

        danhGiaSanPhamRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DanhGiaSanPham> getReviewsByProduct(String idSanPham, Pageable pageable) {
        // Trả về đánh giá của sản phẩm (bao gồm APPROVED và PENDING)
        return danhGiaSanPhamRepository.findBySanPham_Id(idSanPham, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkEligibility(String idHoaDon, String idSanPham, String idKhachHang) {
        if (danhGiaSanPhamRepository.existsByHoaDon_IdAndSanPham_Id(idHoaDon, idSanPham)) {
            return false;
        }

        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElse(null);
        if (hoaDon == null || hoaDon.getTrangThai() != OrderStatus.HOAN_THANH) {
            return false;
        }
        
        if (hoaDon.getKhachHang() == null || !hoaDon.getKhachHang().getId().equals(idKhachHang)) {
            return false;
        }

        Long completionTime = hoaDon.getNgayCapNhat() != null ? hoaDon.getNgayCapNhat() : hoaDon.getNgayTao();
        if (completionTime != null) {
            long daysSinceCompletion = ChronoUnit.DAYS.between(Instant.ofEpochMilli(completionTime), Instant.now());
            if (daysSinceCompletion > 30) {
                return false;
            }
        }

        return true;
    }
}
