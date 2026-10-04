package com.mandiflow.notification.messaging;

import com.mandiflow.events.OrderStatusChangedEvent;
import com.mandiflow.notification.service.EmailService;
import com.mandiflow.notification.service.IdempotencyGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatusEmailListenerTest {

    private final IdempotencyGuard guard = mock(IdempotencyGuard.class);
    private final EmailService emailService = mock(EmailService.class);
    private final StatusEmailListener listener = new StatusEmailListener(guard, emailService);

    private final OrderStatusChangedEvent event = new OrderStatusChangedEvent(UUID.randomUUID(), UUID.randomUUID(),
            42L, "CONFIRMED", null, new BigDecimal("110.00"), Instant.now());

    @Test
    void sendsTheFirstTime() {
        when(guard.firstTime(anyString(), any())).thenReturn(true);
        listener.onStatusChanged(event);
        verify(emailService).sendStatusEmail(event);
        verify(emailService).record(event);
    }

    @Test
    void skipsADuplicate() {
        when(guard.firstTime(anyString(), any())).thenReturn(false);
        listener.onStatusChanged(event);
        verify(emailService, never()).sendStatusEmail(any());
    }

    @Test
    void releasesTheClaimWhenSmtpFailsSoTheRetryCanSend() {
        when(guard.firstTime(anyString(), any())).thenReturn(true);
        doThrow(new MailSendException("Connection refused")).when(emailService).sendStatusEmail(event);

        assertThatThrownBy(() -> listener.onStatusChanged(event)).isInstanceOf(MailSendException.class);
        verify(guard).release("notification", event.eventId());
        verify(emailService, never()).record(any());
    }
}
