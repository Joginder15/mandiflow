package com.mandiflow.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published by order-service when a customer places an order. */
public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        Long customerId,
        List<OrderLine> lines,
        BigDecimal total,
        Instant occurredAt) implements MandiEvent {

    /** One line of the order, with the price the customer saw in the catalogue. */
    public record OrderLine(Long productId, int quantity, BigDecimal unitPrice) {}

    public List<Long> productIds() {
        return lines.stream().map(OrderLine::productId).toList();
    }
}
