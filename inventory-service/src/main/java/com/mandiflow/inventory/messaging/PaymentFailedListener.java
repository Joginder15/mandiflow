package com.mandiflow.inventory.messaging;

import com.mandiflow.events.PaymentFailedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.inventory.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** The compensating step of the saga. */
@Component
@RequiredArgsConstructor
public class PaymentFailedListener {

    private final ReservationService reservationService;

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "inventory-service")
    public void onPaymentFailed(PaymentFailedEvent event) {
        reservationService.release(event.orderId());
    }
}
