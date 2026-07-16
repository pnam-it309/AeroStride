package com.example.be.core.customer.review.service;

import com.example.be.core.customer.review.model.request.ReviewRequest;
import com.example.be.entity.DanhGiaSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    void submitReview(ReviewRequest request);
    Page<DanhGiaSanPham> getReviewsByProduct(String idSanPham, Pageable pageable);
    boolean checkEligibility(String idHoaDon, String idSanPham, String idKhachHang);
}
