package com.mandiflow.order.api;

import com.mandiflow.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummary(UUID id, String status, String statusReason, BigDecimal totalAmount, Instant createdAt) {

    public static OrderSummary from(Order o) {
        return new OrderSummary(o.getId(), o.getStatus().name(), o.getStatusReason(), o.getTotalAmount(), o.getCreatedAt());
    }
}
