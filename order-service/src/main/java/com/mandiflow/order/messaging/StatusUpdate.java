package com.mandiflow.order.messaging;

import com.mandiflow.events.OrderStatusChangedEvent;
import com.mandiflow.order.domain.Order;

import java.math.BigDecimal;
import java.util.UUID;

/** What the tracking page receives over Server-Sent Events. */
public record StatusUpdate(UUID orderId, String status, String reason, BigDecimal total) {

    static StatusUpdate from(Order o) {
        return new StatusUpdate(o.getId(), o.getStatus().name(), o.getStatusReason(), o.getTotalAmount());
    }

    static StatusUpdate from(OrderStatusChangedEvent e) {
        return new StatusUpdate(e.orderId(), e.status(), e.reason(), e.total());
    }
}
