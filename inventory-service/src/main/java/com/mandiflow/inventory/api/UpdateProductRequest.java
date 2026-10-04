package com.mandiflow.inventory.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Either field may be left out: send only what changes. */
public record UpdateProductRequest(@Positive BigDecimal pricePerUnit, @PositiveOrZero Integer availableQty) {}
