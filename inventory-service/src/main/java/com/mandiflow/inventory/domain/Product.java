package com.mandiflow.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;            // "Tomatoes"

    private String unit;            // "kg", "bunch", "dozen"

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    private int availableQty;

    private int reservedQty;

    @Version
    private Long version;           // two orders racing for the last kg? one fails and is retried

    public static Product of(String name, String unit, BigDecimal pricePerUnit, int availableQty) {
        Product p = new Product();
        p.name = name;
        p.unit = unit;
        p.pricePerUnit = pricePerUnit;
        p.availableQty = availableQty;
        return p;
    }

    public void reserve(int qty) {
        if (qty > availableQty) {
            throw new IllegalStateException(name + ": cannot reserve " + qty + ", only " + availableQty + " left");
        }
        availableQty -= qty;
        reservedQty += qty;
    }

    public void release(int qty) {
        availableQty += qty;
        reservedQty -= qty;
    }
}
