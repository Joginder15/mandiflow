package com.mandiflow.payment.messaging;

import com.mandiflow.events.InventoryReservedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReservedListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "payment-service")
    public void onInventoryReserved(InventoryReservedEvent event) {
        paymentService.handle(event);
    }
}
