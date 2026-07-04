package com.example.be.core.customer.order.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.order.model.request.CustomerOrderCheckoutRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateItemsRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateShippingRequest;
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
@RequestMapping(RoutesConstant.CUSTOMER_ORDER)
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

    // Lấy thống kê đơn hàng cá nhân
    @GetMapping("/stats")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<com.example.be.core.customer.order.model.response.CustomerOrderStatsResponse>> getMyOrderStats(
            Authentication authentication
    ) {
        log.info("Fetching order stats for customer: user={}", authentication.getName());
        com.example.be.core.customer.order.model.response.CustomerOrderStatsResponse stats = customerOrderService.getMyOrderStats(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(stats));
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

    // Khách cập nhật thông tin nhận hàng (sđt, địa chỉ, ghi chú) khi đơn đang chờ xác nhận
    @PutMapping("/{id}/shipping")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> updateShipping(
            @PathVariable String id,
            @Valid @RequestBody CustomerUpdateShippingRequest request,
            Authentication authentication
    ) {
        log.info("Customer update shipping: id={}, user={}", id, authentication.getName());
        CustomerOrderResponse response = customerOrderService.updateShippingInfo(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin nhận hàng thành công"));
    }

    // Khách cập nhật số lượng sản phẩm (chỉ đơn tiền mặt, đang chờ xác nhận)
    @PutMapping("/{id}/items")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> updateItems(
            @PathVariable String id,
            @Valid @RequestBody CustomerUpdateItemsRequest request,
            Authentication authentication
    ) {
        log.info("Customer update items: id={}, user={}", id, authentication.getName());
        CustomerOrderResponse response = customerOrderService.updateItems(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật sản phẩm thành công"));
    }

    // Tạo lại URL thanh toán VNPay cho đơn chuyển khoản chưa thanh toán (thanh toán lại)
    @PostMapping("/{id}/vnpay-url")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<ApiResponse<String>> createVnPayUrl(
            @PathVariable String id,
            @RequestParam String returnUrl,
            Authentication authentication
    ) {
        log.info("Customer re-pay VNPay: id={}, user={}", id, authentication.getName());
        String url = customerOrderService.createVnPayUrl(id, returnUrl, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(url, "Tạo URL thanh toán thành công"));
    }

    // Lấy danh sách các mã giảm giá (voucher) có thể áp dụng cho tổng tiền giỏ hàng hiện tại
    @GetMapping("/vouchers")
    public ResponseEntity<ApiResponse<List<PhieuGiamGia>>> getAvailableVouchers(
            @RequestParam(required = false) BigDecimal tongTien,
            Authentication authentication
    ) {
        // Khách vãng lai (chưa đăng nhập) -> username null -> chỉ nhận phiếu công khai
        String username = (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) ? authentication.getName() : null;
        log.info("Fetching available vouchers for amount: {}, user: {}", tongTien, username);
        List<PhieuGiamGia> vouchers = customerOrderService.getAvailableVouchers(tongTien, username);
        return ResponseEntity.ok(ApiResponse.success(vouchers));
    }
}
