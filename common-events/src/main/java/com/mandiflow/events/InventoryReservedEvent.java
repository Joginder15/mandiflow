package com.mandiflow.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by inventory-service once every line of an order is held. */
public record InventoryReservedEvent(UUID eventId, UUID orderId, BigDecimal amount) implements MandiEvent {}
