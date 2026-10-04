package com.mandiflow.inventory.service;

import com.mandiflow.inventory.domain.Product;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * What the Redis catalogue cache stores. Stock is left out on purpose: it changes with every
 * order, so a cached value would be stale at once and we would oversell.
 */
public record ProductDto(Long id, String name, String unit, BigDecimal pricePerUnit) implements Serializable {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getUnit(), p.getPricePerUnit());
    }
}
