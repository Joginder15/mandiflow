package com.mandiflow.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @Positive BigDecimal pricePerUnit,
        @PositiveOrZero int availableQty) {}
