package com.mandiflow.order.api;

import com.mandiflow.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        Long customerId,
        String status,
        String statusReason,
        BigDecimal totalAmount,
        Instant createdAt,
        List<Item> items) {

    public record Item(Long productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getStatus().name(), o.getStatusReason(),
                o.getTotalAmount(), o.getCreatedAt(),
                o.getItems().stream()
                        .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.lineTotal()))
                        .toList());
    }
}
