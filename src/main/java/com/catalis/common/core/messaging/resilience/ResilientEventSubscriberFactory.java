package com.catalis.common.core.messaging.resilience;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.subscriber.EventSubscriber;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Factory for creating resilient event subscribers.
 */
@Component
@RequiredArgsConstructor
public class ResilientEventSubscriberFactory {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final MessagingProperties messagingProperties;
    
    /**
     * Creates a resilient event subscriber that wraps the given subscriber.
     *
     * @param subscriber the subscriber to wrap
     * @param subscriberName the name of the subscriber
     * @return a resilient event subscriber
     */
    public EventSubscriber createResilientSubscriber(EventSubscriber subscriber, String subscriberName) {
        // Only wrap the subscriber if resilience is enabled
        if (messagingProperties.isResilience()) {
            return new ResilientEventSubscriber(
                    subscriber,
                    circuitBreakerRegistry,
                    retryRegistry,
                    meterRegistryProvider,
                    subscriberName
            );
        }
        
        return subscriber;
    }
}
