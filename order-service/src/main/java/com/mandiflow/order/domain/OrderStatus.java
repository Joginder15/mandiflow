package com.mandiflow.order.domain;

public enum OrderStatus {
    /** Placed, waiting for inventory and payment. */
    PENDING,
    /** Stock held and money collected. */
    CONFIRMED,
    /** Inventory could not hold the stock (out of stock, unknown product, price changed). */
    REJECTED,
    /** Payment was declined; inventory released the stock. */
    CANCELLED;

    public boolean isFinal() {
        return this != PENDING;
    }
}
