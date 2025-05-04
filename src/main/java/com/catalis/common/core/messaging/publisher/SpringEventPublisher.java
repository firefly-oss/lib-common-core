package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.event.GenericApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link EventPublisher} that uses Spring's ApplicationEventPublisher.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventPublisher implements EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
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
}
