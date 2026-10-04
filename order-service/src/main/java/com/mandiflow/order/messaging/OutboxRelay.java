package com.mandiflow.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiflow.order.domain.OutboxEvent;
import com.mandiflow.order.repo.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes pending outbox rows to Kafka, oldest first, keyed by order ID.
 *
 * If the relay crashes after sending but before the commit, those rows are sent again on the
 * next run. That is "at least once" delivery, and why every consumer is idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxRelayLock relayLock;

    @Scheduled(fixedDelayString = "${mandiflow.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() throws Exception {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();
        if (pending.isEmpty() || !relayLock.tryAcquire()) {
            return;
        }
        for (OutboxEvent e : pending) {
            Object event = objectMapper.readValue(e.getPayload(), Class.forName(e.getEventType()));
            kafkaTemplate.send(e.getTopic(), e.getAggregateId(), event).get(5, TimeUnit.SECONDS);
            e.markPublished();
            log.debug("Outbox {} → {} (key {})", e.getId(), e.getTopic(), e.getAggregateId());
        }
    }

    /** Published rows are only history; keep a day of them for debugging. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgePublished() {
        int deleted = outboxRepository.deletePublishedBefore(Instant.now().minus(Duration.ofDays(1)));
        if (deleted > 0) log.info("Purged {} published outbox rows", deleted);
    }
}
