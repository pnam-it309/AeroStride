package com.example.be.core.customer.review.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.review.model.request.ReviewRequest;
import com.example.be.core.customer.review.service.ReviewService;
import com.example.be.entity.DanhGiaSanPham;
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
    public ResponseEntity<ApiResponse<Void>> submitReview(@RequestBody ReviewRequest request) {
        reviewService.submitReview(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/product/{idSanPham}")
    public ResponseEntity<ApiResponse<Page<DanhGiaSanPham>>> getReviews(
            @PathVariable Long idSanPham,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DanhGiaSanPham> reviews = reviewService.getReviewsByProduct(idSanPham, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayTao")));
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/check-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkEligibility(
            @RequestParam Long idHoaDon,
            @RequestParam Long idSanPham,
            @RequestParam Long idKhachHang) {
        boolean eligible = reviewService.checkEligibility(idHoaDon, idSanPham, idKhachHang);
        return ResponseEntity.ok(ApiResponse.success(eligible));
    }
}
