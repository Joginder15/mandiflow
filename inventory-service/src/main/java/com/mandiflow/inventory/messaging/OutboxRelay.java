package com.mandiflow.inventory.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiflow.inventory.domain.OutboxEvent;
import com.mandiflow.inventory.repo.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Same pattern as order-service: publish pending outbox rows, oldest first.
 * (order-service also shows the Redis lock you need when running several copies.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${mandiflow.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() throws Exception {
        for (OutboxEvent e : outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()) {
            Object event = objectMapper.readValue(e.getPayload(), Class.forName(e.getEventType()));
            kafkaTemplate.send(e.getTopic(), e.getAggregateId(), event).get(5, TimeUnit.SECONDS);
            e.markPublished();
            log.debug("Outbox {} → {} (key {})", e.getId(), e.getTopic(), e.getAggregateId());
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgePublished() {
        outboxRepository.deletePublishedBefore(Instant.now().minus(Duration.ofDays(1)));
    }
}
