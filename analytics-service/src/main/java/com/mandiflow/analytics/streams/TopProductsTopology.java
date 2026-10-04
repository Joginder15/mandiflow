package com.mandiflow.analytics.streams;

import com.mandiflow.events.OrderCreatedEvent;
import com.mandiflow.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

/**
 * orders.created → one record per line (key = productId, value = quantity)
 * → grouped by product → 5-minute tumbling windows → sum.
 *
 * The running totals live in a local RocksDB state store ("units-per-product-5m"), backed up
 * to an internal changelog topic so they survive a restart.
 */
@Configuration
@EnableKafkaStreams
@Slf4j
public class TopProductsTopology {

    public static final String STORE = "units-per-product-5m";
    public static final Duration WINDOW = Duration.ofMinutes(5);

    @Bean
    KStream<String, OrderCreatedEvent> ordersStream(StreamsBuilder builder) {
        JsonSerde<OrderCreatedEvent> orderSerde = new JsonSerde<>(OrderCreatedEvent.class).ignoreTypeHeaders();

        KStream<String, OrderCreatedEvent> orders = builder.stream(
                Topics.ORDER_CREATED, Consumed.with(Serdes.String(), orderSerde));

        orders.<String, Long>flatMap((orderId, order) -> order.lines().stream()
                        .map(l -> KeyValue.pair(l.productId().toString(), (long) l.quantity()))
                        .toList())
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(WINDOW))
                .reduce(Long::sum, Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .foreach((window, units) -> log.info("Product {} sold {} units in window starting {}",
                        window.key(), units, window.window().startTime()));

        return orders;
    }
}
