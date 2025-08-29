package com.firefly.common.core.messaging.publisher;

import com.firefly.common.core.messaging.event.GenericApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link EventPublisher} that uses Spring's ApplicationEventPublisher.
 * <p>
 * This implementation supports the {@link ConnectionAwarePublisher} interface for consistency,
 * but since Spring's ApplicationEventPublisher is internal to the application, the connection ID
 * is not used for any configuration lookup.
 */
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SpringEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.fromRunnable(() -> {
            log.debug("Publishing event to Spring Event Bus: type={}, transactionId={}", eventType, transactionId);
            GenericApplicationEvent event = new GenericApplicationEvent(
                    payload,
                    eventType,
                    destination,
                    transactionId
            );
            applicationEventPublisher.publishEvent(event);
        });
    }

    @Override
    public boolean isAvailable() {
        return true; // Spring Event Bus is always available
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
