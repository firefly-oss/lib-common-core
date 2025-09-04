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

import com.firefly.common.core.messaging.publisher.ConnectionAwarePublisher;
import com.firefly.common.core.messaging.publisher.EventPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * A decorator for {@link EventPublisher} that adds resilience patterns like
 * circuit breaker, retry, timeout, and metrics.
 * <p>
 * This class implements the Decorator pattern to enhance an {@link EventPublisher}
 * with resilience capabilities. It wraps an existing publisher and adds the following
 * features:
 * <ul>
 *   <li><strong>Circuit Breaker:</strong> Prevents cascading failures by stopping calls to a failing service</li>
 *   <li><strong>Retry:</strong> Automatically retries failed operations with configurable backoff</li>
 *   <li><strong>Timeout:</strong> Sets maximum duration for operations to prevent blocked threads</li>
 *   <li><strong>Metrics:</strong> Collects and exposes metrics for monitoring and alerting</li>
 * </ul>
 * <p>
 * The resilience features are implemented using the Resilience4j library, which provides
 * a comprehensive set of fault tolerance mechanisms for reactive applications.
 * <p>
 * The metrics are collected using Micrometer, which provides a vendor-neutral metrics facade
 * that can be integrated with various monitoring systems like Prometheus, Datadog, etc.
 *
 * @see EventPublisher
 * @see io.github.resilience4j.circuitbreaker.CircuitBreaker
 * @see io.github.resilience4j.retry.Retry
 * @see io.micrometer.core.instrument.MeterRegistry
 */
@Slf4j
public class ResilientEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final EventPublisher delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MeterRegistry meterRegistry;
    private final String publisherName;

    /**
     * Creates a new ResilientEventPublisher.
     *
     * @param delegate the delegate publisher
     * @param circuitBreakerRegistry the circuit breaker registry
     * @param retryRegistry the retry registry
     * @param meterRegistryProvider the meter registry provider
     * @param publisherName the name of the publisher
     */
    public ResilientEventPublisher(
            EventPublisher delegate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            String publisherName) {
        this.delegate = delegate;
        this.publisherName = publisherName;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(publisherName + "-circuit-breaker");
        this.retry = retryRegistry.retry(publisherName + "-retry");
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        Mono<Void> publishMono = delegate.publish(destination, eventType, payload, transactionId)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("Publishing to {} timed out for eventType={}", publisherName, eventType);
                    return Mono.error(e);
                })
                .doOnError(e -> log.error("Error publishing to {}: {}", publisherName, e.getMessage()))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));

        if (meterRegistry != null) {
            Timer timer = Timer.builder("messaging.publish")
                    .tag("publisher", publisherName)
                    .tag("destination", destination)
                    .tag("eventType", eventType)
                    .register(meterRegistry);

            return Mono.defer(() -> {
                long startTime = System.nanoTime();
                return publishMono.doFinally(signal -> {
                    long endTime = System.nanoTime();
                    timer.record(Duration.ofNanos(endTime - startTime));
                });
            });
        }

        return publishMono;
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable() && circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    @Override
    public void setConnectionId(String connectionId) {
        if (delegate instanceof ConnectionAwarePublisher) {
            ((ConnectionAwarePublisher) delegate).setConnectionId(connectionId);
        } else {
            log.warn("Delegate publisher does not implement ConnectionAwarePublisher, connectionId will be ignored");
        }
    }

    @Override
    public String getConnectionId() {
        if (delegate instanceof ConnectionAwarePublisher) {
            return ((ConnectionAwarePublisher) delegate).getConnectionId();
        } else {
            log.warn("Delegate publisher does not implement ConnectionAwarePublisher, returning default connectionId");
            return "default";
        }
    }
}
