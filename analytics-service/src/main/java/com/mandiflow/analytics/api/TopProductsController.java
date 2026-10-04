package com.mandiflow.analytics.api;

import com.mandiflow.analytics.streams.TopProductsTopology;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Interactive query: read the Kafka Streams state store straight from this service. */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class TopProductsController {

    private final StreamsBuilderFactoryBean streamsFactory;

    public record ProductUnits(Long productId, long units) {}

    public record TopProducts(Instant windowStart, Instant windowEnd, List<ProductUnits> products) {}

    @GetMapping("/top-products")
    public TopProducts topProducts() {
        KafkaStreams streams = streamsFactory.getKafkaStreams();
        if (streams == null || streams.state() != KafkaStreams.State.RUNNING) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka Streams is not running yet");
        }

        long size = TopProductsTopology.WINDOW.toMillis();
        long now = System.currentTimeMillis();
        Instant windowStart = Instant.ofEpochMilli(now - now % size);   // current tumbling window

        Map<Long, Long> totals = new HashMap<>();
        try {
            ReadOnlyWindowStore<String, Long> store = streams.store(StoreQueryParameters.fromNameAndType(
                    TopProductsTopology.STORE, QueryableStoreTypes.<String, Long>windowStore()));
            try (KeyValueIterator<Windowed<String>, Long> it = store.fetchAll(windowStart, Instant.ofEpochMilli(now))) {
                while (it.hasNext()) {
                    KeyValue<Windowed<String>, Long> kv = it.next();
                    totals.merge(Long.valueOf(kv.key.key()), kv.value, Long::sum);
                }
            }
        } catch (InvalidStateStoreException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "State store is rebalancing, try again");
        }

        List<ProductUnits> products = new ArrayList<>();
        totals.forEach((id, units) -> products.add(new ProductUnits(id, units)));
        products.sort(Comparator.comparingLong(ProductUnits::units).reversed());
        return new TopProducts(windowStart, windowStart.plus(TopProductsTopology.WINDOW), products);
    }
}
