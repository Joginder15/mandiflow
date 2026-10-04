package com.mandiflow.notification.messaging;

import com.mandiflow.events.OrderStatusChangedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.notification.service.EmailService;
import com.mandiflow.notification.service.IdempotencyGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Emails the customer on every status change.
 *
 * Try it: stop Mailpit and place an order. The send fails, the guard key is released, and the
 * error handler retries after 1 s, 2 s and 4 s before moving the record to orders.status-changed.DLT.
 * Start Mailpit, re-send the DLT message from Kafka UI, and exactly one email goes out.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusEmailListener {

    private static final String CONSUMER = "notification";

    private final IdempotencyGuard guard;
    private final EmailService emailService;

    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED, groupId = "notification-service")
    public void onStatusChanged(OrderStatusChangedEvent e) {
        if (!guard.firstTime(CONSUMER, e.eventId())) {
            log.info("Skipping duplicate event {}", e.eventId());
            return;
        }
        try {
            emailService.sendStatusEmail(e);
        } catch (RuntimeException ex) {
            guard.release(CONSUMER, e.eventId());   // let the retry run again
            throw ex;
        }
        emailService.record(e);
    }
}
