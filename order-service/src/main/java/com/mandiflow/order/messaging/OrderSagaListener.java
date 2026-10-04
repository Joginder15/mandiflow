package com.mandiflow.order.messaging;

import com.mandiflow.events.InventoryRejectedEvent;
import com.mandiflow.events.PaymentCompletedEvent;
import com.mandiflow.events.PaymentFailedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.order.domain.OrderStatus;
import com.mandiflow.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The order's side of the choreographed saga. One listener reads three topics; the JSON type
 * header on each message picks the matching @KafkaHandler method.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(groupId = "order-service",
        topics = {Topics.INVENTORY_REJECTED, Topics.PAYMENT_COMPLETED, Topics.PAYMENT_FAILED})
public class OrderSagaListener {

    private final OrderService orderService;

    @KafkaHandler
    public void on(PaymentCompletedEvent e) {
        orderService.changeStatus(e.orderId(), OrderStatus.CONFIRMED, null);
    }

    @KafkaHandler
    public void on(InventoryRejectedEvent e) {
        orderService.changeStatus(e.orderId(), OrderStatus.REJECTED, e.detail());
    }

    @KafkaHandler
    public void on(PaymentFailedEvent e) {
        orderService.changeStatus(e.orderId(), OrderStatus.CANCELLED,
                "Your payment was declined. No money was taken.");
    }

    @KafkaHandler(isDefault = true)
    public void unknown(Object payload) {
        log.warn("Ignoring unexpected message type {}", payload.getClass().getName());
    }
}
