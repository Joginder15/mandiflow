package com.mandiflow.analytics.streams;

import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.Topics;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.WindowStore;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the topology in memory, no Kafka broker needed. */
class TopProductsTopologyTest {

    @Test
    void sumsUnitsPerProductInsideTheWindow() {
        StreamsBuilder builder = new StreamsBuilder();
        new TopProductsTopology().ordersStream(builder);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "analytics-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        Instant start = Instant.parse("2026-10-04T10:00:00Z");
        try (TopologyTestDriver driver = new TopologyTestDriver(builder.build(), props, start)) {
            TestInputTopic<String, OrderCreatedEvent> input = driver.createInputTopic(Topics.ORDER_CREATED,
                    new StringSerializer(), new JsonSerde<>(OrderCreatedEvent.class).serializer());

            input.pipeInput("a", order(new OrderCreatedEvent.OrderLine(1L, 2, new BigDecimal("40"))), start);
            input.pipeInput("b", order(new OrderCreatedEvent.OrderLine(1L, 5, new BigDecimal("40")),
                    new OrderCreatedEvent.OrderLine(3L, 1, new BigDecimal("30"))), start.plusSeconds(60));

            WindowStore<String, Long> store = driver.getWindowStore(TopProductsTopology.STORE);
            assertThat(store.fetch("1", start)).isEqualTo(7L);   // 2 + 5 kg tomatoes
            assertThat(store.fetch("3", start)).isEqualTo(1L);
        }
    }

    private OrderCreatedEvent order(OrderCreatedEvent.OrderLine... lines) {
        return new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), 42L, List.of(lines),
                BigDecimal.TEN, Instant.now());
    }
}
