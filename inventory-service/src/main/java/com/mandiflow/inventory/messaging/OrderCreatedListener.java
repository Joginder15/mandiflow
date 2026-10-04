package com.mandiflow.inventory.messaging;

import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.inventory.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final ReservationService reservationService;

    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        log.debug("orders.created for {} from partition {} offset {}", event.orderId(), partition, offset);
        reservationService.reserve(event);
    }
}
