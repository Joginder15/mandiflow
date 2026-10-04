package com.mandiflow.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiflow.events.Topics;
import com.mandiflow.order.api.OrderResponse;
import com.mandiflow.order.api.OrderSummary;
import com.mandiflow.order.api.PlaceOrderRequest;
import com.mandiflow.order.domain.Order;
import com.mandiflow.order.domain.OrderItem;
import com.mandiflow.order.domain.OrderStatus;
import com.mandiflow.order.domain.OutboxEvent;
import com.mandiflow.order.repo.OrderRepository;
import com.mandiflow.order.repo.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves the order and its OrderCreatedEvent in ONE transaction. Nothing is sent to Kafka here:
     * the outbox relay does that, so the order row and the event can never disagree.
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req) {
        List<OrderItem> items = req.lines().stream()
                .map(l -> OrderItem.of(l.productId(), l.quantity(), l.unitPrice()))
                .toList();
        Order order = orderRepository.save(Order.pending(req.customerId(), items));
        outboxRepository.save(OutboxEvent.of(Topics.ORDER_CREATED, order.getId(),
                OrderEvents.created(order), objectMapper));
        log.info("Order {} placed by customer {} for ₹{}", order.getId(), order.getCustomerId(), order.getTotalAmount());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return OrderResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public Order find(UUID id) {
        return orderRepository.findWithItemsById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> recentForCustomer(Long customerId) {
        return orderRepository.findTop20ByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderSummary::from)
                .toList();
    }

    /**
     * Called by the saga listener. A repeated event for the same status is ignored, which makes
     * this method safe to run twice for the same Kafka message.
     */
    @Transactional
    public void changeStatus(UUID orderId, OrderStatus next, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.moveTo(next, reason)) {
            outboxRepository.save(OutboxEvent.of(Topics.ORDER_STATUS_CHANGED, orderId,
                    OrderEvents.statusChanged(order), objectMapper));
            log.info("Order {} is now {}{}", orderId, next, reason == null ? "" : " (" + reason + ")");
        } else {
            log.info("Order {} already {}, duplicate event skipped", orderId, next);
        }
    }
}
