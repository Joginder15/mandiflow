package com.mandiflow.inventory.domain;

public enum ReservationStatus {
    /** Stock is held for the order. */
    HELD,
    /** Payment failed and the stock went back on the shelf. */
    RELEASED,
    /** The order could not be reserved. Kept so a redelivered event is recognised. */
    REJECTED
}
