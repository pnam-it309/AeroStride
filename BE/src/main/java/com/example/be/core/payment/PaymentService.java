package com.example.be.core.payment;

import com.example.be.core.payment.dto.PaymentRequest;
import java.util.Map;

public interface PaymentService {

    /**
     * Generates a payment URL to redirect the customer to the payment gateway.
     * @param request The payment details (amount, order id, etc.)
     * @return String The payment URL.
     */
    String createPaymentUrl(PaymentRequest request);

    /**
     * Verifies the integrity of the payment response from the gateway.
     * @param params Map of request parameters received from the gateway callback.
     * @return boolean True if signature is valid.
     */
    boolean verifyPayment(Map<String, String> params);
}
