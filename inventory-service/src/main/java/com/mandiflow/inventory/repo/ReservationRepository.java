package com.mandiflow.inventory.repo;

import com.mandiflow.inventory.domain.Reservation;
import com.mandiflow.inventory.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByOrderId(UUID orderId);

    Optional<Reservation> findByOrderId(UUID orderId);

    Optional<Reservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
