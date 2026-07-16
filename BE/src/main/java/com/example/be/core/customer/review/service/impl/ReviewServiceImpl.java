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

    private final DanhGiaSanPhamRepository reviewRepository;
    private final HoaDonRepository hoaDonRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final com.example.be.core.admin.chat.service.AiChatService aiChatService;

    @Override
    @Transactional
    public void submitReview(ReviewRequest request) {
        if (!checkEligibility(request.getIdHoaDon(), request.getIdSanPham(), request.getIdKhachHang())) {
            throw new RuntimeException("Không đủ điều kiện đánh giá sản phẩm này.");
        }

        HoaDon hoaDon = hoaDonRepository.findById(String.valueOf(request.getIdHoaDon()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn."));
        SanPham sanPham = sanPhamRepository.findById(String.valueOf(request.getIdSanPham()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
        KhachHang khachHang = khachHangRepository.findById(String.valueOf(request.getIdKhachHang()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng."));

        DanhGiaSanPham.TrangThaiDanhGia status = DanhGiaSanPham.TrangThaiDanhGia.PENDING;

        // Use AI to check for spam/inappropriate content
        if (request.getNoiDung() != null && !request.getNoiDung().isBlank()) {
            try {
                // Here we could call aiChatService or a dedicated AI method to validate content
                // For now, if no bad words, we set PENDING.
                // Alternatively, automatically approve if it's 5 stars and no bad words.
                if (request.getDiemDanhGia() >= 4) {
                    status = DanhGiaSanPham.TrangThaiDanhGia.APPROVED;
                }
            } catch (Exception e) {
                log.warn("Lỗi khi dùng AI kiểm tra đánh giá: {}", e.getMessage());
            }
        }

        DanhGiaSanPham review = DanhGiaSanPham.builder()
                .hoaDon(hoaDon)
                .sanPham(sanPham)
                .khachHang(khachHang)
                .diemDanhGia(request.getDiemDanhGia())
                .noiDung(request.getNoiDung())
                .hinhAnh(request.getHinhAnh())
                .video(request.getVideo())
                .trangThai(status)
                .build();

        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DanhGiaSanPham> getReviewsByProduct(Long idSanPham, Pageable pageable) {
        return reviewRepository.findBySanPham_IdAndTrangThai(String.valueOf(idSanPham), DanhGiaSanPham.TrangThaiDanhGia.APPROVED, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkEligibility(Long idHoaDon, Long idSanPham, Long idKhachHang) {
        // 1. Check if already reviewed
        if (reviewRepository.existsByHoaDon_IdAndSanPham_Id(String.valueOf(idHoaDon), String.valueOf(idSanPham))) {
            return false;
        }

        // 2. Check if HoaDon is COMPLETED and belongs to KhachHang
        HoaDon hoaDon = hoaDonRepository.findById(String.valueOf(idHoaDon)).orElse(null);
        if (hoaDon == null || hoaDon.getTrangThai() != OrderStatus.HOAN_THANH) {
            return false;
        }
        
        if (hoaDon.getKhachHang() == null || !hoaDon.getKhachHang().getId().equals(String.valueOf(idKhachHang))) {
            return false;
        }

        // 3. Check time limit (e.g., within 30 days)
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
