package com.example.be.repository;

import com.example.be.entity.Order;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends BaseRepository<Order, String> {

    /**
     * Finds orders by status and created timestamp before a certain threshold.
     * Uses Spring Data JPA method name derivation to avoid @Query.
     */
    List<Order> findAllByOrderStatusAndCreatedAtBefore(OrderStatus status, Long threshold);
}
