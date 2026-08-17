package com.example.be.core.admin.danhgia.service;

import com.example.be.core.admin.danhgia.model.response.AdminDanhGiaConfigResponse;
import com.example.be.entity.DanhGiaSanPham;
import com.example.be.repository.DanhGiaSanPhamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewConfigService {

    private final DanhGiaSanPhamRepository danhGiaSanPhamRepository;

    // Cấu hình tự động phê duyệt (Mặc định: BẬT - tự động duyệt đánh giá hợp lệ)
    private final AtomicBoolean autoApproveEnabled = new AtomicBoolean(true);

    // Danh sách từ ngữ cấm, tục tĩu, nhạy cảm, spam hoặc mang tính lừa đảo
    private static final List<String> PROFANITY_KEYWORDS = List.of(
            "đm", "dm", "dmm", "đmm", "vcl", "vcc", "clm", "đéo", "deo", "cặc", "cac", "lồn", "lon",
            "buồi", "buoi", "đĩ", "di", "chó", "cứt", "cut", "bitch", "fuck", "shit", "ass", "motherfucker",
            "lừa đảo", "lua dao", "hàng fake", "gian lận", "quỵt tiền", "shop lừa", "scam", "scammer"
    );

    public boolean isAutoApproveEnabled() {
        return autoApproveEnabled.get();
    }

    public boolean setAutoApproveEnabled(boolean enabled) {
        autoApproveEnabled.set(enabled);
        log.info("Chế độ tự động duyệt đánh giá đã chuyển thành: {}", enabled ? "BẬT" : "TẮT");
        return autoApproveEnabled.get();
    }

    /**
     * Kiểm tra nội dung đánh giá có hợp lệ hay không:
     * - Điểm đánh giá hợp lệ từ 1 đến 5 sao.
     * - Nội dung không chứa từ ngữ thô tục, xúc phạm hoặc spam.
     */
    public boolean isReviewContentAppropriate(String content, Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            return false;
        }
        if (content == null || content.trim().isEmpty()) {
            return true; // Đánh giá sao không kèm text vẫn hợp lệ
        }
        String normalized = content.toLowerCase().trim();
        for (String badWord : PROFANITY_KEYWORDS) {
            String pattern = "(?i).*\\b" + Pattern.quote(badWord) + "\\b.*";
            if (normalized.contains(badWord) || normalized.matches(pattern)) {
                log.warn("Đánh giá chứa từ cấm/không hợp lệ: '{}' (từ cấm: '{}')", content, badWord);
                return false;
            }
        }
        return true;
    }

    public AdminDanhGiaConfigResponse getConfigAndStats() {
        long total = danhGiaSanPhamRepository.count();
        long pending = danhGiaSanPhamRepository.countByTrangThai(DanhGiaSanPham.TrangThaiDanhGia.PENDING);
        long approved = danhGiaSanPhamRepository.countByTrangThai(DanhGiaSanPham.TrangThaiDanhGia.APPROVED);
        long rejected = danhGiaSanPhamRepository.countByTrangThai(DanhGiaSanPham.TrangThaiDanhGia.REJECTED);
        long spam = danhGiaSanPhamRepository.countByTrangThai(DanhGiaSanPham.TrangThaiDanhGia.SPAM);

        return AdminDanhGiaConfigResponse.builder()
                .autoApprove(autoApproveEnabled.get())
                .total(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .spam(spam)
                .build();
    }
}
