package com.mandiflow.inventory;

import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.inventory.domain.ReservationStatus;
import com.mandiflow.inventory.repo.ProductRepository;
import com.mandiflow.inventory.repo.ReservationRepository;
import com.mandiflow.inventory.service.CatalogService;
import com.mandiflow.inventory.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Reservation rules against a real MySQL with the seeded produce (tomatoes = 1, saag = 3, basmati = 4). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ReservationIT {

    @Container @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @Container @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4").withExposedPorts(6379);

    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CatalogService catalogService;

    private OrderCreatedEvent order(OrderCreatedEvent.OrderLine... lines) {
        BigDecimal total = List.of(lines).stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), 42L, List.of(lines), total, Instant.now());
    }

    private int available(long productId) {
        return productRepository.findById(productId).orElseThrow().getAvailableQty();
    }

    @Test
    void reservesEveryLineAndIgnoresADuplicate() {
        int before = available(1);
        var event = order(new OrderCreatedEvent.OrderLine(1L, 2, new BigDecimal("40.00")));

        reservationService.reserve(event);
        reservationService.reserve(event);   // Kafka delivered it twice

        assertThat(available(1)).isEqualTo(before - 2);
        assertThat(reservationRepository.findByOrderId(event.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void rejectsWholeOrderWhenOneLineIsShort() {
        int tomatoes = available(1);
        int saag = available(3);
        var event = order(
                new OrderCreatedEvent.OrderLine(1L, 1, new BigDecimal("40.00")),
                new OrderCreatedEvent.OrderLine(3L, saag + 15, new BigDecimal("30.00")));

        reservationService.reserve(event);

        assertThat(available(1)).isEqualTo(tomatoes);   // all or nothing
        assertThat(available(3)).isEqualTo(saag);
        assertThat(reservationRepository.findByOrderId(event.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.REJECTED);
    }

    @Test
    void rejectsAStalePrice() {
        var event = order(new OrderCreatedEvent.OrderLine(4L, 1, new BigDecimal("99.00")));   // basmati is ₹120

        reservationService.reserve(event);

        assertThat(reservationRepository.findByOrderId(event.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.REJECTED);
    }

    @Test
    void paymentFailureReleasesTheStockOnce() {
        int before = available(4);
        var event = order(new OrderCreatedEvent.OrderLine(4L, 25, new BigDecimal("120.00")));
        reservationService.reserve(event);
        assertThat(available(4)).isEqualTo(before - 25);

        reservationService.release(event.orderId());
        reservationService.release(event.orderId());   // duplicate payments.failed

        assertThat(available(4)).isEqualTo(before);
        assertThat(reservationRepository.findByOrderId(event.orderId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void catalogueReadIsCached() {
        var first = catalogService.get(1L);
        var second = catalogService.get(1L);
        assertThat(second).isEqualTo(first);
        assertThat(first.name()).isEqualTo("Tomatoes");
    }
}
