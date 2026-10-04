package com.mandiflow.notification.service;

import com.mandiflow.events.OrderStatusChangedEvent;
import com.mandiflow.notification.domain.Notification;
import com.mandiflow.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailMessages messages;
    private final NotificationRepository notificationRepository;

    @Value("${mandiflow.mail.from:orders@mandiflow.test}")
    private String from;

    /** Throws if the SMTP server is down, which makes the Kafka error handler retry. */
    public void sendStatusEmail(OrderStatusChangedEvent e) {
        String to = messages.recipientFor(e.customerId());
        String subject = messages.subjectFor(e);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(messages.textFor(e));
        mailSender.send(mail);
        log.info("Email sent to {}: {}", to, subject);
    }

    /** Called only after the email went out; a failure here must not send the email again. */
    public void record(OrderStatusChangedEvent e) {
        try {
            notificationRepository.save(Notification.email(e.orderId(), e.eventId(),
                    messages.recipientFor(e.customerId()), messages.subjectFor(e)));
        } catch (RuntimeException ex) {
            log.warn("Email for order {} was sent but could not be logged: {}", e.orderId(), ex.getMessage());
        }
    }
}
