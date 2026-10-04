package com.mandiflow.notification.repo;

import com.mandiflow.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOrderIdOrderBySentAtAsc(UUID orderId);
}
