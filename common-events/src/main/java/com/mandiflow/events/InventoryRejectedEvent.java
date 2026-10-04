package com.mandiflow.events;

import java.util.UUID;

/**
 * Published by inventory-service when an order cannot be reserved.
 *
 * @param reason machine-readable code: OUT_OF_STOCK, UNKNOWN_PRODUCT or PRICE_CHANGED
 * @param detail human-readable text that ends up in the customer's email
 */
public record InventoryRejectedEvent(UUID eventId, UUID orderId, String reason, String detail) implements MandiEvent {

    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String UNKNOWN_PRODUCT = "UNKNOWN_PRODUCT";
    public static final String PRICE_CHANGED = "PRICE_CHANGED";
}
