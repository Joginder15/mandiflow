package com.mandiflow.order.messaging;

import com.mandiflow.events.OrderStatusChangedEvent;
import com.mandiflow.events.Topics;
import com.mandiflow.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the open browser connections and pushes status changes to them.
 *
 * The browser may be connected to ANY running copy of order-service, so every copy must see
 * every status change. A random group ID gives each copy its own consumer group: broadcast,
 * not load-balancing. "latest" means a fresh copy does not replay old history.
 */
@Component
@Slf4j
public class SseHub {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Order order) {
        UUID orderId = order.getId();
        SseEmitter emitter = new SseEmitter(0L);   // no timeout
        emitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> remove(orderId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ex -> remove.run());

        // Send the current status straight away, in case the order finished before the page opened.
        send(emitter, StatusUpdate.from(order));
        if (order.getStatus().isFinal()) {
            emitter.complete();
        }
        return emitter;
    }

    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED,
                   groupId = "#{'order-sse-' + T(java.util.UUID).randomUUID()}",
                   properties = "auto.offset.reset=latest")
    public void push(OrderStatusChangedEvent e) {
        List<SseEmitter> open = emitters.getOrDefault(e.orderId(), List.of());
        log.debug("Pushing {} for order {} to {} browser(s)", e.status(), e.orderId(), open.size());
        open.forEach(em -> {
            send(em, StatusUpdate.from(e));
            em.complete();   // every status after PENDING is final
        });
    }

    private void send(SseEmitter emitter, StatusUpdate update) {
        try {
            emitter.send(update);
        } catch (IOException | IllegalStateException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void remove(UUID orderId, SseEmitter emitter) {
        emitters.computeIfPresent(orderId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
