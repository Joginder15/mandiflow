package com.mandiflow.payment.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A Kafka message waiting to be sent. It is written in the same MySQL transaction as the
 * business change, so the two can never disagree. {@code OutboxRelay} publishes it later.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)   // CHAR(36), matching the Liquibase column
    @Column(length = 36)
    private String aggregateId;

    private String topic;

    private String eventType;

    @Column(columnDefinition = "json")
    private String payload;

    private Instant createdAt;

    private Instant publishedAt;

    public static OutboxEvent of(String topic, UUID aggregateId, Object event, ObjectMapper mapper) {
        OutboxEvent o = new OutboxEvent();
        o.topic = topic;
        o.aggregateId = aggregateId.toString();
        o.eventType = event.getClass().getName();
        try {
            o.payload = mapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize " + o.eventType, ex);
        }
        o.createdAt = Instant.now();
        return o;
    }

    public void markPublished() {
        publishedAt = Instant.now();
    }
}
