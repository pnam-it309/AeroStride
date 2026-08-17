package com.example.be.core.customer.review.service;

import com.example.be.core.customer.review.model.request.ReviewRequest;
import com.example.be.core.customer.review.model.response.CustomerReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    CustomerReviewResponse submitReview(ReviewRequest request);
    Page<CustomerReviewResponse> getReviewsByProduct(String idSanPham, Pageable pageable);
    boolean checkEligibility(String idHoaDon, String idSanPham, String idKhachHang);
}
