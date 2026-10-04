package com.mandiflow.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // plain VARCHAR, not a MySQL ENUM column
    @Column(length = 20)
    private OrderStatus status;

    private String statusReason;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private Long version;

    private Instant createdAt;

    public static Order pending(Long customerId, List<OrderItem> items) {
        Order o = new Order();
        o.id = UUID.randomUUID();
        o.customerId = customerId;
        o.status = OrderStatus.PENDING;
        o.createdAt = Instant.now();
        items.forEach(o::addItem);
        o.totalAmount = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return o;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Moves a PENDING order to a final status.
     *
     * @return false when the order already has this status (a duplicate event), so callers can skip it
     * @throws IllegalStateException when the order already reached a different final status
     */
    public boolean moveTo(OrderStatus next, String reason) {
        if (status == next) return false;
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order " + id + " is already " + status + ", cannot become " + next);
        }
        status = next;
        statusReason = reason;
        return true;
    }
}
