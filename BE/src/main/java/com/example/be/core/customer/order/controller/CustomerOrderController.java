package com.example.be.core.customer.order.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.order.model.request.CustomerOrderCheckoutRequest;
import com.example.be.core.customer.order.model.response.CustomerOrderResponse;
import com.example.be.core.customer.order.service.CustomerOrderService;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.CUSTOMER + "/order")
@RequiredArgsConstructor
/**
 * Module: Đặt hàng (Customer)
 * Chức năng: Quản lý API phục vụ quá trình đặt hàng trực tuyến của khách hàng.
 * Bao gồm các chức năng như checkout, xem danh sách đơn hàng của tôi, xem chi tiết, hủy đơn, lấy voucher hợp lệ.
 */
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    // Xử lý thanh toán và tạo đơn hàng trực tuyến cho khách hàng
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> checkout(
            @Valid @RequestBody CustomerOrderCheckoutRequest request,
            Authentication authentication
    ) {
        log.info("Customer checkout: user={}", authentication.getName());
        CustomerOrderResponse response = customerOrderService.checkout(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Đặt hàng thành công"));
    }

    // Lấy danh sách đơn hàng cá nhân của khách hàng (có thể lọc theo trạng thái)
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<List<CustomerOrderResponse>>> getMyOrders(
            @RequestParam(required = false) String trangThai,
            Authentication authentication
    ) {
        log.info("Fetching orders for customer: user={}, status={}", authentication.getName(), trangThai);
        List<CustomerOrderResponse> orders = customerOrderService.getMyOrders(authentication.getName(), trangThai);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // Xem chi tiết một đơn hàng cụ thể của khách hàng
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> getOrderDetail(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("Fetching order detail: id={}, user={}", id, authentication.getName());
        CustomerOrderResponse response = customerOrderService.getOrderDetail(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Hủy đơn hàng (nếu đơn hàng đang ở trạng thái cho phép hủy)
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String id,
            Authentication authentication
    ) {
        log.info("Cancelling order: id={}, user={}", id, authentication.getName());
        customerOrderService.cancelOrder(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Hủy đơn hàng thành công"));
    }

    // Lấy danh sách các mã giảm giá (voucher) có thể áp dụng cho tổng tiền giỏ hàng hiện tại
    @GetMapping("/vouchers")
    public ResponseEntity<ApiResponse<List<PhieuGiamGia>>> getAvailableVouchers(
            @RequestParam(required = false) BigDecimal tongTien
    ) {
        log.info("Fetching available vouchers for amount: {}", tongTien);
        List<PhieuGiamGia> vouchers = customerOrderService.getAvailableVouchers(tongTien);
        return ResponseEntity.ok(ApiResponse.success(vouchers));
    }
}
