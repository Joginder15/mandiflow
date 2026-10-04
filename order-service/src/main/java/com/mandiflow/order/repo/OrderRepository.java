package com.mandiflow.order.repo;

import com.mandiflow.order.domain.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Loads the order and its items in one query, so they can be read after the transaction. */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    List<Order> findTop20ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
