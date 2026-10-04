package com.mandiflow.notification.messaging;

import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Phase 2's first consumer, kept as a window into Kafka: it logs where each order landed.
 * Stop this service, place three orders, start it again: it logs all three, because the
 * group's committed offset says where to resume.
 */
@Component
@Slf4j
public class OrderCreatedLogger {

    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "notification-service")
    public void on(OrderCreatedEvent event,
                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                   @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Order {} for ₹{} · partition {} · offset {}", event.orderId(), event.total(), partition, offset);
    }
}
