package com.firefly.common.core.messaging.resilience;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.subscriber.EventSubscriber;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Factory for creating resilient event subscribers.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ResilientEventSubscriberFactory {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final MessagingProperties messagingProperties;

    public ResilientEventSubscriberFactory(
            @Qualifier("messagingCircuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("messagingRetryRegistry") RetryRegistry retryRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            MessagingProperties messagingProperties) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.meterRegistryProvider = meterRegistryProvider;
        this.messagingProperties = messagingProperties;
    }

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
