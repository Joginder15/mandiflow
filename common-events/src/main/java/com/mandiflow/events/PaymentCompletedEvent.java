package com.mandiflow.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by payment-service when the money is collected. */
public record PaymentCompletedEvent(UUID eventId, UUID orderId, BigDecimal amount) implements MandiEvent {}
