package com.mandiflow.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One row per order this service has seen, whatever the outcome. */
@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private ReservationStatus status;

    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reservation_id", nullable = false)
    private List<ReservationItem> items = new ArrayList<>();

    /** @param quantities product ID → quantity held */
    public static Reservation held(UUID orderId, Map<Long, Integer> quantities) {
        Reservation r = create(orderId, ReservationStatus.HELD);
        quantities.forEach((productId, qty) -> r.items.add(ReservationItem.of(productId, qty)));
        return r;
    }

    public static Reservation rejected(UUID orderId) {
        return create(orderId, ReservationStatus.REJECTED);
    }

    private static Reservation create(UUID orderId, ReservationStatus status) {
        Reservation r = new Reservation();
        r.orderId = orderId;
        r.status = status;
        r.createdAt = Instant.now();
        return r;
    }

    /** Status becomes RELEASED, so a repeated payments.failed event does nothing. */
    public void release() {
        status = ReservationStatus.RELEASED;
    }
}
