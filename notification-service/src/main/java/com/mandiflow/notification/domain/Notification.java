package com.mandiflow.notification.domain;

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

/** A log of every email sent. Useful when you debug the DLT. */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID orderId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID eventId;

    private String channel;

    private String recipient;

    private String subject;

    private Instant sentAt;

    public static Notification email(UUID orderId, UUID eventId, String recipient, String subject) {
        Notification n = new Notification();
        n.orderId = orderId;
        n.eventId = eventId;
        n.channel = "EMAIL";
        n.recipient = recipient;
        n.subject = subject;
        n.sentAt = Instant.now();
        return n;
    }
}
