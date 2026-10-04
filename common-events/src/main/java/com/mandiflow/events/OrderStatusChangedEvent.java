package com.mandiflow.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by order-service after every status change. Read by notification-service
 * (emails) and by every order-service instance (live tracking over SSE).
 *
 * @param reason why the order was rejected or cancelled; null when it was confirmed
 */
public record OrderStatusChangedEvent(
        UUID eventId,
        UUID orderId,
        Long customerId,
        String status,
        String reason,
        BigDecimal total,
        Instant occurredAt) implements MandiEvent {}
