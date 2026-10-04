package com.mandiflow.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiflow.events.InventoryReservedEvent;
import com.mandiflow.events.PaymentCompletedEvent;
import com.mandiflow.events.PaymentFailedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.payment.domain.OutboxEvent;
import com.mandiflow.payment.domain.Payment;
import com.mandiflow.payment.domain.PaymentStatus;
import com.mandiflow.payment.repo.OutboxRepository;
import com.mandiflow.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final FakePaymentGateway gateway;
    private final ObjectMapper objectMapper;

    /**
     * Never charge twice: an order that already has a payment row is skipped, and the UNIQUE key on
     * order_id stops two parallel attempts. The payment row and its event commit together.
     */
    @Transactional
    public void handle(InventoryReservedEvent e) {
        if (paymentRepository.existsByOrderId(e.orderId())) {
            log.info("Order {} already charged, duplicate inventory.reserved skipped", e.orderId());
            return;
        }

        boolean paid = gateway.charge(e.orderId(), e.amount());
        paymentRepository.save(Payment.of(e.orderId(), e.amount(), paid ? PaymentStatus.PAID : PaymentStatus.DECLINED));

        Object event = paid
                ? new PaymentCompletedEvent(UUID.randomUUID(), e.orderId(), e.amount())
                : new PaymentFailedEvent(UUID.randomUUID(), e.orderId(), PaymentFailedEvent.CARD_DECLINED);
        String topic = paid ? Topics.PAYMENT_COMPLETED : Topics.PAYMENT_FAILED;
        outboxRepository.save(OutboxEvent.of(topic, e.orderId(), event, objectMapper));
    }
}
