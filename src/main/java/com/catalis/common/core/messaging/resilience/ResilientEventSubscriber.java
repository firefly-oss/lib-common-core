package com.catalis.common.core.messaging.resilience;

import com.catalis.common.core.messaging.handler.EventHandler;
import com.catalis.common.core.messaging.subscriber.EventSubscriber;
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
 * A decorator for {@link EventSubscriber} that adds resilience patterns like
 * circuit breaker, retry, timeout, and metrics.
 */
@Slf4j
public class ResilientEventSubscriber implements EventSubscriber {

    private final EventSubscriber delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MeterRegistry meterRegistry;
    private final String subscriberName;

    /**
     * Creates a new ResilientEventSubscriber.
     *
     * @param delegate the delegate subscriber
     * @param circuitBreakerRegistry the circuit breaker registry
     * @param retryRegistry the retry registry
     * @param meterRegistryProvider the meter registry provider
     * @param subscriberName the name of the subscriber
     */
    public ResilientEventSubscriber(
            EventSubscriber delegate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            String subscriberName) {
        this.delegate = delegate;
        this.subscriberName = subscriberName;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(subscriberName + "-circuit-breaker");
        this.retry = retryRegistry.retry(subscriberName + "-retry");
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public Mono<Void> subscribe(
            String source,
            String eventType,
            EventHandler eventHandler,
            String groupId,
            String clientId,
            int concurrency,
            boolean autoAck) {

        Mono<Void> subscribeMono = delegate.subscribe(
                source,
                eventType,
                eventHandler,
                groupId,
                clientId,
                concurrency,
                autoAck
        )
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("Subscribing to {} timed out for source={}, eventType={}",
                            subscriberName, source, eventType);
                    return Mono.error(e);
                })
                .doOnError(e -> log.error("Error subscribing to {}: {}", subscriberName, e.getMessage()))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));

        if (meterRegistry != null) {
            Timer timer = Timer.builder("messaging.subscribe")
                    .tag("subscriber", subscriberName)
                    .tag("source", source)
                    .tag("eventType", eventType)
                    .register(meterRegistry);

            return Mono.defer(() -> {
                long startTime = System.nanoTime();
                return subscribeMono.doFinally(signal -> {
                    long endTime = System.nanoTime();
                    timer.record(Duration.ofNanos(endTime - startTime));
                });
            });
        }

        return subscribeMono;
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        Mono<Void> unsubscribeMono = delegate.unsubscribe(source, eventType)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("Unsubscribing from {} timed out for source={}, eventType={}",
                            subscriberName, source, eventType);
                    return Mono.error(e);
                })
                .doOnError(e -> log.error("Error unsubscribing from {}: {}", subscriberName, e.getMessage()))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));

        if (meterRegistry != null) {
            Timer timer = Timer.builder("messaging.unsubscribe")
                    .tag("subscriber", subscriberName)
                    .tag("source", source)
                    .tag("eventType", eventType)
                    .register(meterRegistry);

            return Mono.defer(() -> {
                long startTime = System.nanoTime();
                return unsubscribeMono.doFinally(signal -> {
                    long endTime = System.nanoTime();
                    timer.record(Duration.ofNanos(endTime - startTime));
                });
            });
        }

        return unsubscribeMono;
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable() && circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }
}
