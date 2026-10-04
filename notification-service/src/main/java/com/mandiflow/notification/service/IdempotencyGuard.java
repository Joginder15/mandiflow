package com.mandiflow.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Kafka can deliver a message more than once. Before acting, a consumer claims the event ID with
 * Redis SET NX; if the key already exists, the event was handled and is skipped.
 * Key example: processed:notification:3f1c…
 */
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private static final Duration KEEP_FOR = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    /** @return true the first time this consumer sees this event, false for a repeat */
    public boolean firstTime(String consumer, UUID eventId) {
        Boolean fresh = redis.opsForValue().setIfAbsent(key(consumer, eventId), "1", KEEP_FOR);
        return Boolean.TRUE.equals(fresh);
    }

    /** Forget the claim so a retry of a failed attempt can run again. */
    public void release(String consumer, UUID eventId) {
        redis.delete(key(consumer, eventId));
    }

    private String key(String consumer, UUID eventId) {
        return "processed:" + consumer + ":" + eventId;
    }
}
