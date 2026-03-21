package com.example.be.core.common.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * Custom event triggered when an order is successfully placed.
 * Used to decouple order processing from side effects like notifications.
 */
@Getter
public class OrderPlacedEvent extends ApplicationEvent {

    private final String orderId;
    private final String userEmail;
    private final BigDecimal totalAmount;

    public OrderPlacedEvent(Object source, String orderId, String userEmail, BigDecimal totalAmount) {
        super(source);
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
    }
}
