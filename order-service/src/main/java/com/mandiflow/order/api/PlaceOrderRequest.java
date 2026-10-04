package com.mandiflow.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * The client sends the price it showed the customer. inventory-service checks it against the
 * current price and rejects the order (PRICE_CHANGED) if the admin changed it in the meantime,
 * so order-service never has to call inventory-service directly.
 */
public record PlaceOrderRequest(
        @NotNull @Positive Long customerId,
        @NotEmpty @Size(max = 20) List<@Valid Line> lines) {

    public record Line(@NotNull @Positive Long productId,
                       @Positive @Max(100) int quantity,
                       @NotNull @Positive BigDecimal unitPrice) {}
}
