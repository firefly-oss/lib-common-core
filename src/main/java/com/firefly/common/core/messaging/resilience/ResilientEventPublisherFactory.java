package com.firefly.common.core.messaging.resilience;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.publisher.EventPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Factory for creating resilient event publishers.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ResilientEventPublisherFactory {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final MessagingProperties messagingProperties;

    public ResilientEventPublisherFactory(
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
     * Creates a resilient event publisher that wraps the given publisher.
     *
     * @param publisher the publisher to wrap
     * @param publisherName the name of the publisher
     * @return a resilient event publisher
     */
    public EventPublisher createResilientPublisher(EventPublisher publisher, String publisherName) {
        // Only wrap the publisher if resilience is enabled
        if (messagingProperties.isResilience()) {
            return new ResilientEventPublisher(
                    publisher,
                    circuitBreakerRegistry,
                    retryRegistry,
                    meterRegistryProvider,
                    publisherName
            );
        }

        return publisher;
    }
}
