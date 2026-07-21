package com.example.be.core.customer.review.service;

import com.example.be.core.customer.review.model.request.ReviewRequest;
import com.example.be.entity.DanhGiaSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    void submitReview(ReviewRequest request);
    Page<DanhGiaSanPham> getReviewsByProduct(Long idSanPham, Pageable pageable);
    boolean checkEligibility(Long idHoaDon, Long idSanPham, Long idKhachHang);
}
