package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import com.example.be.infrastructure.constants.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends PrimaryEntity {

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    private BigDecimal totalAmount;

    private String customerName;

    private String customerEmail;
}
