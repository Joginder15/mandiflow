package com.mandiflow.payment;

import com.mandiflow.events.InventoryReservedEvent;
import com.mandiflow.payment.domain.PaymentStatus;
import com.mandiflow.payment.repo.PaymentRepository;
import com.mandiflow.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PaymentIT {

    @Container @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;

    @Test
    void smallOrderIsPaidOnlyOnce() {
        UUID orderId = UUID.randomUUID();
        var event = new InventoryReservedEvent(UUID.randomUUID(), orderId, new BigDecimal("110.00"));

        paymentService.handle(event);
        paymentService.handle(event);   // duplicate delivery

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paymentRepository.count()).isPositive();
    }

    @Test
    void orderAboveTheLimitIsDeclined() {
        UUID orderId = UUID.randomUUID();
        paymentService.handle(new InventoryReservedEvent(UUID.randomUUID(), orderId, new BigDecimal("3000.00")));

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }
}
