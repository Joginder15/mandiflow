package com.mandiflow.notification.service;

import com.mandiflow.events.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMessagesTest {

    private final EmailMessages messages = new EmailMessages();

    @Test
    void rejectedEmailCarriesTheReason() {
        var e = new OrderStatusChangedEvent(UUID.randomUUID(), UUID.randomUUID(), 7L, "REJECTED",
                "Sorry, sarson saag is short by 15 bunch (only 35 left).", new BigDecimal("1500.00"), Instant.now());

        assertThat(messages.recipientFor(7L)).isEqualTo("customer-7@mandiflow.test");
        assertThat(messages.subjectFor(e)).endsWith("is REJECTED");
        assertThat(messages.textFor(e)).contains("short by 15").contains("No money was taken");
    }
}
