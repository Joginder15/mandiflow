package com.mandiflow.notification.config;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retry a failing record 3 times (1 s, 2 s, 4 s), then move it to "<topic>.DLT" on the same
 * partition so the rest of the partition keeps flowing. Spring Boot plugs this bean into every
 * @KafkaListener container.
 */
@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> jsonTemplate,
                                          ProducerFactory<Object, Object> producerFactory) {
        // Records that could not even be deserialized are dead-lettered as their raw bytes.
        Map<String, Object> props = new HashMap<>(producerFactory.getConfigurationProperties());
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new ByteArraySerializer()));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);
        var recoverer = new DeadLetterPublishingRecoverer(templates);

        var backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Retrying cannot fix bad data or an impossible state change: straight to the DLT.
        handler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
        return handler;
    }
}
