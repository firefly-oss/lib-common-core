package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.event.GenericApplicationEvent;
import com.catalis.common.core.messaging.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link EventSubscriber} that uses Spring's ApplicationContext for event handling.
 * <p>
 * This implementation supports the {@link ConnectionAwareSubscriber} interface for consistency,
 * although Spring events don't use external connections. The connectionId is ignored in this implementation.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class SpringEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    private final ApplicationContext applicationContext;
    private final Map<String, EventHandler> eventHandlers = new ConcurrentHashMap<>();

    private String connectionId = "default";

    @Override
    public Mono<Void> subscribe(
            String source,
            String eventType,
            EventHandler eventHandler,
            String groupId,
            String clientId,
            int concurrency,
            boolean autoAck) {

        String key = getEventKey(source, eventType);
        eventHandlers.put(key, eventHandler);

        log.info("Subscribed to Spring events with key: {}", key);
        return Mono.empty();
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        String key = getEventKey(source, eventType);
        eventHandlers.remove(key);

        log.info("Unsubscribed from Spring events with key: {}", key);
        return Mono.empty();
    }

    @Override
    public boolean isAvailable() {
        return true; // Spring Event Bus is always available
    }

    /**
     * Handles Spring application events.
     *
     * @param event the event to handle
     */
    @EventListener
    public void handleApplicationEvent(GenericApplicationEvent event) {
        String key = getEventKey(event.getDestination(), event.getEventType());
        EventHandler handler = eventHandlers.get(key);

        if (handler != null) {
            try {
                // Create a map for headers that can handle null values
                Map<String, Object> headers = new HashMap<>();
                headers.put("eventType", event.getEventType());
                headers.put("destination", event.getDestination());

                // Only add transactionId if it's not null
                if (event.getTransactionId() != null) {
                    headers.put("transactionId", event.getTransactionId());
                }

                // Get the payload
                Object rawPayload = event.getPayload();
                byte[] payload;

                // Handle different payload types properly
                if (rawPayload == null) {
                    payload = new byte[0];
                } else if (rawPayload instanceof byte[]) {
                    payload = (byte[]) rawPayload;
                } else if (rawPayload instanceof String) {
                    payload = ((String) rawPayload).getBytes(StandardCharsets.UTF_8);
                } else {
                    // For other types, we'll need to serialize them properly
                    // This is a safer approach than just calling toString()
                    try {
                        // Use Jackson or another serializer if available
                        // For now, we'll use a simple toString() but in a safer way
                        payload = rawPayload.toString().getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.error("Failed to serialize payload for Spring event: {}", e.getMessage(), e);
                        payload = new byte[0];
                    }
                }

                // Handle the event with proper error handling
                handler.handleEvent(payload, headers, null)
                        .doOnSuccess(v -> log.debug("Successfully handled Spring event: {}", key))
                        .doOnError(error -> log.error("Error handling Spring event: {}", error.getMessage(), error))
                        .onErrorResume(e -> {
                            // Just log the error and continue
                            return Mono.empty();
                        })
                        .subscribe();
            } catch (Exception e) {
                log.error("Error handling Spring event: {}", e.getMessage(), e);
            }
        }
    }

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }
}
