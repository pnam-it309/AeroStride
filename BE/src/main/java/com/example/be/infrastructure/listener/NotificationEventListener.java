package com.example.be.infrastructure.listener;

import com.example.be.core.common.events.OrderPlacedEvent;
import com.example.be.core.notification.EmailService;
import com.example.be.core.notification.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for system-wide notifications.
 * Decouples the business process (like ordering) from the notification process (like email).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    /**
     * Listens for OrderPlacedEvent and sends a confirmation email.
     * TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) ensures
     * the email is only sent if the database transaction is successfully committed.
     * @Async makes the email sending non-blocking for the main request thread.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for order: {}. Sending confirmation to {}",
                event.getOrderId(), event.getUserEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", event.getOrderId());
            variables.put("totalAmount", event.getTotalAmount());

            EmailRequest request = EmailRequest.builder()
                    .to(event.getUserEmail())
                    .subject("AeroStride - Order Confirmation #" + event.getOrderId())
                    .templateName("order-confirmation") // Assume this template exists
                    .variables(variables)
                    .build();

            emailService.sendHtmlEmail(request);
            log.info("Order confirmation email sent successfully for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}",
                    event.getOrderId(), e.getMessage());
        }
    }
}
