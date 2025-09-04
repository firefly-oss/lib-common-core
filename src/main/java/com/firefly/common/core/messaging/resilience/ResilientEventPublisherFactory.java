/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
