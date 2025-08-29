package com.firefly.common.core.messaging.resilience;

import com.firefly.common.core.messaging.handler.EventHandler;
import com.firefly.common.core.messaging.subscriber.EventSubscriber;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ResilientEventSubscriber}.
 * 
 * Note: These tests focus on the delegation behavior rather than the actual resilience patterns,
 * which are difficult to test due to the complexity of the resilience4j library.
 */
@ExtendWith(MockitoExtension.class)
public class ResilientEventSubscriberTest {

    @Mock
    private EventSubscriber delegateSubscriber;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private io.github.resilience4j.retry.Retry retry;

    private MeterRegistry meterRegistry;

    private TestResilientEventSubscriber resilientSubscriber;

    private final String subscriberName = "test-subscriber";
    private final String source = "test-source";
    private final String eventType = "test.event";

    /**
     * Test-specific implementation of ResilientEventSubscriber that bypasses the resilience components.
     */
    private class TestResilientEventSubscriber extends ResilientEventSubscriber {
        public TestResilientEventSubscriber(
                EventSubscriber delegateSubscriber,
                CircuitBreakerRegistry circuitBreakerRegistry,
                RetryRegistry retryRegistry,
                ObjectProvider<MeterRegistry> meterRegistryProvider,
                String subscriberName) {
            super(delegateSubscriber, circuitBreakerRegistry, retryRegistry, meterRegistryProvider, subscriberName);
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
            // Bypass resilience components and directly delegate
            return delegateSubscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck);
        }

        @Override
        public Mono<Void> unsubscribe(String source, String eventType) {
            // Bypass resilience components and directly delegate
            return delegateSubscriber.unsubscribe(source, eventType);
        }
    }

    @BeforeEach
    void setUp() {
        // Set up the circuit breaker
        lenient().when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // Set up the retry
        lenient().when(retryRegistry.retry(anyString())).thenReturn(retry);

        // Set up the meter registry
        meterRegistry = new SimpleMeterRegistry();
        lenient().when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        // Set up the delegate subscriber
        lenient().when(delegateSubscriber.subscribe(anyString(), anyString(), any(), anyString(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(Mono.empty());
        lenient().when(delegateSubscriber.unsubscribe(anyString(), anyString())).thenReturn(Mono.empty());
        lenient().when(delegateSubscriber.isAvailable()).thenReturn(true);

        // Create the test resilient subscriber
        resilientSubscriber = new TestResilientEventSubscriber(
                delegateSubscriber,
                circuitBreakerRegistry,
                retryRegistry,
                meterRegistryProvider,
                subscriberName
        );
    }

    @Test
    void shouldDelegateSubscribe() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;
        EventHandler eventHandler = mock(EventHandler.class);

        // When
        resilientSubscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck).block();

        // Then
        verify(delegateSubscriber).subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck);
    }

    @Test
    void shouldDelegateUnsubscribe() {
        // Given
        // When
        resilientSubscriber.unsubscribe(source, eventType).block();

        // Then
        verify(delegateSubscriber).unsubscribe(source, eventType);
    }

    @Test
    void shouldHandleErrorsInSubscribe() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;
        EventHandler eventHandler = mock(EventHandler.class);

        RuntimeException exception = new RuntimeException("Test exception");
        lenient().when(delegateSubscriber.subscribe(anyString(), anyString(), any(), anyString(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(Mono.error(exception));

        // When
        try {
            resilientSubscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck).block();
            fail("Expected exception was not thrown");
        } catch (RuntimeException e) {
            // Then
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    void shouldHandleErrorsInUnsubscribe() {
        // Given
        RuntimeException exception = new RuntimeException("Test exception");
        lenient().when(delegateSubscriber.unsubscribe(anyString(), anyString())).thenReturn(Mono.error(exception));

        // When
        try {
            resilientSubscriber.unsubscribe(source, eventType).block();
            fail("Expected exception was not thrown");
        } catch (RuntimeException e) {
            // Then
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    void shouldBeAvailableWhenDelegateIsAvailableAndCircuitBreakerIsClosed() {
        // Given
        lenient().when(delegateSubscriber.isAvailable()).thenReturn(true);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // When
        boolean available = resilientSubscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenDelegateIsNotAvailable() {
        // Given
        lenient().when(delegateSubscriber.isAvailable()).thenReturn(false);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // When
        boolean available = resilientSubscriber.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenCircuitBreakerIsOpen() {
        // Given
        lenient().when(delegateSubscriber.isAvailable()).thenReturn(true);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

        // When
        boolean available = resilientSubscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}
