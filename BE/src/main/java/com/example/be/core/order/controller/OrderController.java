package com.example.be.core.order.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.order.OrderService;
import com.example.be.entity.Order;
import com.example.be.infrastructure.constants.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Order>>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateStatus(@PathVariable UUID id, @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, status), "Order status updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Order deleted successfully"));
    }
}
