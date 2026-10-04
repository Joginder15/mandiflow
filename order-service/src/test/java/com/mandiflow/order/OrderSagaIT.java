package com.mandiflow.order;

import com.mandiflow.events.InventoryRejectedEvent;
import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.PaymentCompletedEvent;
import com.mandiflow.events.PaymentFailedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.order.api.OrderResponse;
import com.mandiflow.order.api.PlaceOrderRequest;
import com.mandiflow.order.domain.OrderStatus;
import com.mandiflow.order.service.OrderService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Runs the order side of the saga against real MySQL, Kafka and Redis containers. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OrderSagaIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4").withExposedPorts(6379);

    @Autowired OrderService orderService;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired KafkaConnectionDetails kafkaConnection;

    private PlaceOrderRequest sampleRequest() {
        return new PlaceOrderRequest(42L, List.of(
                new PlaceOrderRequest.Line(1L, 2, new BigDecimal("40.00")),
                new PlaceOrderRequest.Line(3L, 1, new BigDecimal("30.00"))));
    }

    @Test
    void placedOrderIsPublishedThroughTheOutbox() {
        OrderResponse order = orderService.placeOrder(sampleRequest());

        try (Consumer<String, OrderCreatedEvent> consumer = createdConsumer()) {
            consumer.subscribe(List.of(Topics.ORDER_CREATED));
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1));
                assertThat(records.records(Topics.ORDER_CREATED))
                        .extracting(ConsumerRecord::key)
                        .contains(order.id().toString());
            });
        }
    }

    @Test
    void paymentCompletedConfirmsTheOrder() {
        OrderResponse order = orderService.placeOrder(sampleRequest());

        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, order.id().toString(),
                new PaymentCompletedEvent(UUID.randomUUID(), order.id(), order.totalAmount()));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(orderService.find(order.id()).getStatus()).isEqualTo(OrderStatus.CONFIRMED));
    }

    @Test
    void inventoryRejectionRejectsTheOrderWithItsReason() {
        OrderResponse order = orderService.placeOrder(sampleRequest());

        kafkaTemplate.send(Topics.INVENTORY_REJECTED, order.id().toString(),
                new InventoryRejectedEvent(UUID.randomUUID(), order.id(),
                        InventoryRejectedEvent.OUT_OF_STOCK, "Sarson saag: only 35 bunch left, you asked for 50"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var saved = orderService.find(order.id());
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.REJECTED);
            assertThat(saved.getStatusReason()).contains("Sarson saag");
        });
    }

    @Test
    void duplicatePaymentFailureIsHarmless() {
        OrderResponse order = orderService.placeOrder(sampleRequest());
        var failed = new PaymentFailedEvent(UUID.randomUUID(), order.id(), PaymentFailedEvent.CARD_DECLINED);

        kafkaTemplate.send(Topics.PAYMENT_FAILED, order.id().toString(), failed);
        kafkaTemplate.send(Topics.PAYMENT_FAILED, order.id().toString(), failed);   // Kafka redelivery

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(orderService.find(order.id()).getStatus()).isEqualTo(OrderStatus.CANCELLED));
    }

    private Consumer<String, OrderCreatedEvent> createdConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaConnection.getBootstrapServers()),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<OrderCreatedEvent> valueDeserializer = new JsonDeserializer<>(OrderCreatedEvent.class, false);
        valueDeserializer.addTrustedPackages("com.mandiflow.events");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer).createConsumer();
    }
}
