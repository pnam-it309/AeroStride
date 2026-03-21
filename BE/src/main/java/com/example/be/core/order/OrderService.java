package com.example.be.core.order;

import com.example.be.entity.Order;
import com.example.be.infrastructure.constants.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    void cleanupExpiredPendingOrders();
    Page<Order> getAllOrders(Pageable pageable);
    Order getOrderById(UUID id);
    Order updateOrderStatus(UUID id, OrderStatus status);
    void deleteOrder(UUID id);
}

