package com.mandiflow.order.service;

import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.OrderStatusChangedEvent;
import com.mandiflow.order.domain.Order;

import java.time.Instant;
import java.util.UUID;

/** Turns Order entities into the event records other services understand. */
public final class OrderEvents {

    private OrderEvents() {}

    public static OrderCreatedEvent created(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(i -> new OrderCreatedEvent.OrderLine(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                        .toList(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    public static OrderStatusChangedEvent statusChanged(Order order) {
        return new OrderStatusChangedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getStatusReason(),
                order.getTotalAmount(),
                Instant.now());
    }
}
