package com.mandiflow.events;

import java.util.UUID;

/** Published by payment-service when the charge is declined. Inventory releases the stock. */
public record PaymentFailedEvent(UUID eventId, UUID orderId, String reason) implements MandiEvent {

    public static final String CARD_DECLINED = "CARD_DECLINED";
}
