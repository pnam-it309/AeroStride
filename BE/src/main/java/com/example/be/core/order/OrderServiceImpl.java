package com.example.be.core.order;

import com.example.be.entity.Order;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Value("${order.cleanup.threshold_ms}")
    private long cleanupThresholdMs;

    @Override
    @Transactional
    public void cleanupExpiredPendingOrders() {
        log.info("Starting cleanup of expired pending orders...");
        long threshold = System.currentTimeMillis() - cleanupThresholdMs;
        
        try {
            List<Order> expiredOrders = orderRepository.findAllByOrderStatusAndCreatedAtBefore(
                    OrderStatus.PENDING_PAYMENT, threshold);
            
            if (!expiredOrders.isEmpty()) {
                expiredOrders.forEach(order -> order.setOrderStatus(OrderStatus.CANCELLED));
                orderRepository.saveAll(expiredOrders);
                log.info("Cancelled {} expired pending orders.", expiredOrders.size());
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired orders: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
    }

    @Override
    @Transactional
    public Order updateOrderStatus(UUID id, OrderStatus status) {
        Order order = getOrderById(id);
        order.setOrderStatus(status);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID id) {
        Order order = getOrderById(id);
        orderRepository.delete(order);
    }
}

