package com.mandiflow.events;

import java.util.List;

/** Every Kafka topic in MandiFlow. Each one also has a "<name>.DLT" dead-letter twin. */
public final class Topics {
    public static final String ORDER_CREATED        = "orders.created";
    public static final String INVENTORY_RESERVED   = "inventory.reserved";
    public static final String INVENTORY_REJECTED   = "inventory.rejected";
    public static final String PAYMENT_COMPLETED    = "payments.completed";
    public static final String PAYMENT_FAILED       = "payments.failed";
    public static final String ORDER_STATUS_CHANGED = "orders.status-changed";

    public static final String DLT_SUFFIX = ".DLT";
    public static final int PARTITIONS = 3;

    public static final List<String> ALL = List.of(ORDER_CREATED, INVENTORY_RESERVED,
            INVENTORY_REJECTED, PAYMENT_COMPLETED, PAYMENT_FAILED, ORDER_STATUS_CHANGED);

    private Topics() {}
}
