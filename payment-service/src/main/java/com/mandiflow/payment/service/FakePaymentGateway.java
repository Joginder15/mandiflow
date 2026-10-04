package com.mandiflow.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Stands in for Razorpay or Paytm. Declines anything above the limit so you can test failures on demand. */
@Component
@Slf4j
public class FakePaymentGateway {

    private final BigDecimal declineAbove;

    public FakePaymentGateway(@Value("${mandiflow.payment.decline-above:2000}") BigDecimal declineAbove) {
        this.declineAbove = declineAbove;
    }

    public boolean charge(UUID orderId, BigDecimal amount) {
        boolean paid = amount.compareTo(declineAbove) <= 0;
        log.info("Gateway: order {} for ₹{} → {}", orderId, amount, paid ? "PAID" : "DECLINED");
        return paid;
    }
}
