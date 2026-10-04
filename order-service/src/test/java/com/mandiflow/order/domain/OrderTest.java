package com.mandiflow.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order sampleOrder() {
        return Order.pending(42L, List.of(
                OrderItem.of(1L, 2, new BigDecimal("40.00")),    // 2 kg tomatoes
                OrderItem.of(3L, 1, new BigDecimal("30.00"))));  // 1 bunch sarson saag
    }

    @Test
    void newOrderIsPendingWithSummedTotal() {
        Order order = sampleOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("110.00");
        assertThat(order.getItems()).allSatisfy(i -> assertThat(i.getOrder()).isSameAs(order));
    }

    @Test
    void moveToIsIdempotentForTheSameStatus() {
        Order order = sampleOrder();
        assertThat(order.moveTo(OrderStatus.CONFIRMED, null)).isTrue();
        assertThat(order.moveTo(OrderStatus.CONFIRMED, null)).isFalse();   // duplicate event
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void finalStatusCannotChange() {
        Order order = sampleOrder();
        order.moveTo(OrderStatus.REJECTED, "Out of stock");
        assertThatThrownBy(() -> order.moveTo(OrderStatus.CONFIRMED, null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatusReason()).isEqualTo("Out of stock");
    }
}
