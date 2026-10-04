package com.mandiflow.payment.api;

import com.mandiflow.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentView(UUID orderId, BigDecimal amount, String status, Instant createdAt) {

    public static PaymentView from(Payment p) {
        return new PaymentView(p.getOrderId(), p.getAmount(), p.getStatus().name(), p.getCreatedAt());
    }
}
