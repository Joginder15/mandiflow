package com.mandiflow.inventory.api;

import com.mandiflow.inventory.domain.Product;

import java.math.BigDecimal;

/** Catalogue row with live stock, as shown on the catalogue and admin pages. */
public record ProductView(Long id, String name, String unit, BigDecimal pricePerUnit,
                          int availableQty, int reservedQty) {

    public static ProductView from(Product p) {
        return new ProductView(p.getId(), p.getName(), p.getUnit(), p.getPricePerUnit(),
                p.getAvailableQty(), p.getReservedQty());
    }
}
