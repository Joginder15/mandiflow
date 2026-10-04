package com.mandiflow.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiflow.events.InventoryRejectedEvent;
import com.mandiflow.events.InventoryReservedEvent;
import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.inventory.domain.OutboxEvent;
import com.mandiflow.inventory.domain.Product;
import com.mandiflow.inventory.domain.Reservation;
import com.mandiflow.inventory.domain.ReservationStatus;
import com.mandiflow.inventory.repo.OutboxRepository;
import com.mandiflow.inventory.repo.ProductRepository;
import com.mandiflow.inventory.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * All or nothing: every line is checked first, and stock is only touched if every line fits.
     * Two orders racing for the last kg both read the same @Version; the second commit fails with
     * an optimistic-lock error and the Kafka error handler retries it against the new stock.
     */
    @Transactional
    public void reserve(OrderCreatedEvent event) {
        UUID orderId = event.orderId();
        if (reservationRepository.existsByOrderId(orderId)) {
            log.info("Order {} already handled, duplicate orders.created skipped", orderId);
            return;
        }

        // Two lines for the same product count together.
        Map<Long, Integer> wanted = event.lines().stream().collect(Collectors.toMap(
                OrderCreatedEvent.OrderLine::productId, OrderCreatedEvent.OrderLine::quantity,
                Integer::sum, LinkedHashMap::new));
        Map<Long, Product> products = productRepository.findAllById(wanted.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        InventoryRejectedEvent rejection = findProblem(event, wanted, products);
        if (rejection != null) {
            reservationRepository.save(Reservation.rejected(orderId));
            outboxRepository.save(OutboxEvent.of(Topics.INVENTORY_REJECTED, orderId, rejection, objectMapper));
            log.info("Order {} rejected: {}", orderId, rejection.detail());
            return;
        }

        wanted.forEach((productId, qty) -> products.get(productId).reserve(qty));
        reservationRepository.save(Reservation.held(orderId, wanted));
        outboxRepository.save(OutboxEvent.of(Topics.INVENTORY_RESERVED, orderId,
                new InventoryReservedEvent(UUID.randomUUID(), orderId, event.total()), objectMapper));
        log.info("Order {} reserved: {}", orderId, wanted);
    }

    private InventoryRejectedEvent findProblem(OrderCreatedEvent event, Map<Long, Integer> wanted,
                                               Map<Long, Product> products) {
        for (Long productId : wanted.keySet()) {
            if (!products.containsKey(productId)) {
                return rejection(event, InventoryRejectedEvent.UNKNOWN_PRODUCT,
                        "Product " + productId + " is not in our catalogue.");
            }
        }
        for (OrderCreatedEvent.OrderLine line : event.lines()) {
            Product p = products.get(line.productId());
            if (p.getPricePerUnit().compareTo(line.unitPrice()) != 0) {
                return rejection(event, InventoryRejectedEvent.PRICE_CHANGED,
                        p.getName() + " now costs ₹" + p.getPricePerUnit() + " per " + p.getUnit()
                                + " (you saw ₹" + line.unitPrice() + "). Please order again.");
            }
        }
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            Product p = products.get(e.getKey());
            if (p.getAvailableQty() < e.getValue()) {
                return rejection(event, InventoryRejectedEvent.OUT_OF_STOCK,
                        "Sorry, " + p.getName().toLowerCase() + " is short by " + (e.getValue() - p.getAvailableQty())
                                + " " + p.getUnit() + " (only " + p.getAvailableQty() + " left).");
            }
        }
        return null;
    }

    private InventoryRejectedEvent rejection(OrderCreatedEvent event, String reason, String detail) {
        return new InventoryRejectedEvent(UUID.randomUUID(), event.orderId(), reason, detail);
    }

    /** Compensation for payments.failed: put the held stock back on the shelf. */
    @Transactional
    public void release(UUID orderId) {
        reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.HELD).ifPresentOrElse(r -> {
            r.getItems().forEach(i ->
                    productRepository.findById(i.getProductId()).ifPresent(p -> p.release(i.getQuantity())));
            r.release();
            log.info("Order {}: payment failed, stock released", orderId);
        }, () -> log.info("Order {}: nothing held, release skipped", orderId));
    }
}
