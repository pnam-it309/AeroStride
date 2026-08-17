package com.example.be.core.customer.review.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.review.model.request.ReviewRequest;
import com.example.be.core.customer.review.service.ReviewService;
import com.example.be.core.customer.review.model.response.CustomerReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>> submitReview(@RequestBody ReviewRequest request) {
        CustomerReviewResponse response = reviewService.submitReview(request);
        String message = "APPROVED".equalsIgnoreCase(response.getTrangThai())
                ? "Đánh giá của bạn đã được phê duyệt và hiển thị thành công!"
                : "Cảm ơn bạn! Đánh giá đang chờ ban quản trị kiểm duyệt.";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @GetMapping("/product/{idSanPham}")
    public ResponseEntity<ApiResponse<Page<CustomerReviewResponse>>> getReviews(
            @PathVariable String idSanPham,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CustomerReviewResponse> reviews = reviewService.getReviewsByProduct(idSanPham, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayTao")));
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/check-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkEligibility(
            @RequestParam String idHoaDon,
            @RequestParam String idSanPham,
            @RequestParam String idKhachHang) {
        boolean eligible = reviewService.checkEligibility(idHoaDon, idSanPham, idKhachHang);
        return ResponseEntity.ok(ApiResponse.success(eligible));
    }
}
