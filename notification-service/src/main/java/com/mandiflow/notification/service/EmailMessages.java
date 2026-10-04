package com.mandiflow.notification.service;

import com.mandiflow.events.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;

/** Subject and body text for each status. */
@Component
public class EmailMessages {

    public String recipientFor(Long customerId) {
        return "customer-" + customerId + "@mandiflow.test";
    }

    public String subjectFor(OrderStatusChangedEvent e) {
        return "Order " + shortId(e) + " is " + e.status();
    }

    public String textFor(OrderStatusChangedEvent e) {
        String greeting = "Namaste,\n\n";
        String body = switch (e.status()) {
            case "CONFIRMED" -> "Good news! Your order " + shortId(e) + " for ₹" + e.total()
                    + " is confirmed. Fresh produce is on its way from the mandi.";
            case "REJECTED" -> "We could not accept your order " + shortId(e) + ".\n\n"
                    + (e.reason() != null ? e.reason() : "Some items are not available.")
                    + "\n\nNo money was taken.";
            case "CANCELLED" -> "Your order " + shortId(e) + " was cancelled.\n\n"
                    + (e.reason() != null ? e.reason() : "Your payment was declined. No money was taken.");
            default -> "Your order " + shortId(e) + " is now " + e.status() + ".";
        };
        return greeting + body + "\n\nTeam MandiFlow";
    }

    private String shortId(OrderStatusChangedEvent e) {
        return e.orderId().toString().substring(0, 8).toUpperCase();
    }
}
