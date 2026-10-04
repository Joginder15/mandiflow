package com.mandiflow.events;

import java.util.UUID;

/**
 * Common shape of every event. {@code eventId} is unique per message and is what
 * idempotent consumers remember; {@code orderId} is always the Kafka message key.
 */
public interface MandiEvent {
    UUID eventId();
    UUID orderId();
}
