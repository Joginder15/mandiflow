package com.mandiflow.order.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * When two copies of order-service run, only the one holding lock:outbox-relay publishes,
 * so outbox rows are not sent twice in parallel. The holder renews the lock on every poll;
 * if it dies, the lock expires after 10 seconds and the other copy takes over.
 */
@Component
@Slf4j
public class OutboxRelayLock {

    private static final String KEY = "lock:outbox-relay";
    private static final Duration TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final String instanceId = UUID.randomUUID().toString();

    public OutboxRelayLock(StringRedisTemplate redis, @Value("${mandiflow.outbox.redis-lock:true}") boolean enabled) {
        this.redis = redis;
        this.enabled = enabled;
    }

    public boolean tryAcquire() {
        if (!enabled) return true;
        if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY, instanceId, TTL))) {
            log.info("This instance ({}) is now the outbox relay", instanceId);
            return true;
        }
        if (instanceId.equals(redis.opsForValue().get(KEY))) {
            redis.expire(KEY, TTL);
            return true;
        }
        return false;
    }
}
