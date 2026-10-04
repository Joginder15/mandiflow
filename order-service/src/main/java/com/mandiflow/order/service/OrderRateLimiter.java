package com.mandiflow.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window rate limit in Redis: one counter per customer per minute.
 * Key example: rate:orders:42:29340112 (customer 42, minute number 29340112).
 */
@Component
public class OrderRateLimiter {

    private final StringRedisTemplate redis;
    private final int maxPerMinute;

    public OrderRateLimiter(StringRedisTemplate redis,
                            @Value("${mandiflow.rate-limit.orders-per-minute:5}") int maxPerMinute) {
        this.redis = redis;
        this.maxPerMinute = maxPerMinute;
    }

    public boolean allow(Long customerId) {
        long minute = System.currentTimeMillis() / 60_000;
        String key = "rate:orders:" + customerId + ":" + minute;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofMinutes(2));   // old windows clean themselves up
        }
        return count != null && count <= maxPerMinute;
    }

    public int maxPerMinute() {
        return maxPerMinute;
    }
}
