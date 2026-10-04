package com.mandiflow.notification.api;

import com.mandiflow.notification.domain.Notification;
import com.mandiflow.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-only, for debugging: which emails went out for an order? */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public record NotificationView(UUID orderId, UUID eventId, String channel, String recipient,
                                   String subject, Instant sentAt) {
        static NotificationView from(Notification n) {
            return new NotificationView(n.getOrderId(), n.getEventId(), n.getChannel(), n.getRecipient(),
                    n.getSubject(), n.getSentAt());
        }
    }

    @GetMapping
    public List<NotificationView> forOrder(@RequestParam UUID orderId) {
        return notificationRepository.findByOrderIdOrderBySentAtAsc(orderId).stream()
                .map(NotificationView::from).toList();
    }
}
