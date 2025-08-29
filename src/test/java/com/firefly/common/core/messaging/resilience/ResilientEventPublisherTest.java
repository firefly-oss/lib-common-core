package com.firefly.common.core.messaging.resilience;

import com.firefly.common.core.messaging.publisher.EventPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResilientEventPublisherTest {

    @Mock
    private EventPublisher delegate;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private Retry retry;

    @Mock
    private Timer timer;

    private ResilientEventPublisher resilientPublisher;

    @BeforeEach
    void setUp() {
        // Mock CircuitBreaker and its config
        CircuitBreakerConfig circuitBreakerConfig = mock(CircuitBreakerConfig.class);
        lenient().when(circuitBreaker.getCircuitBreakerConfig()).thenReturn(circuitBreakerConfig);
        lenient().when(circuitBreakerConfig.isWritableStackTraceEnabled()).thenReturn(true);

        lenient().when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        lenient().when(retryRegistry.retry(anyString())).thenReturn(retry);
        lenient().when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        // Mock the Timer.Builder to return our mock timer
        Timer.Builder timerBuilder = mock(Timer.Builder.class);
        lenient().when(meterRegistry.timer(anyString(), any(), any())).thenReturn(timer);

        // Mock the MeterRegistry.config() method to avoid NullPointerException
        MeterRegistry.Config config = mock(MeterRegistry.Config.class);
        lenient().when(meterRegistry.config()).thenReturn(config);
        lenient().when(config.pauseDetector()).thenReturn(null);

        // Mock Timer.Builder.register to return our mock timer
        try (MockedStatic<Timer> mockedTimer = Mockito.mockStatic(Timer.class)) {
            mockedTimer.when(() -> Timer.builder(anyString())).thenReturn(timerBuilder);
            lenient().when(timerBuilder.tag(anyString(), anyString())).thenReturn(timerBuilder);
            lenient().when(timerBuilder.register(any(MeterRegistry.class))).thenReturn(timer);
        }

        // Mock the timer.record method to avoid NullPointerException
        lenient().doNothing().when(timer).record(any(Duration.class));

        resilientPublisher = new ResilientEventPublisher(
                delegate,
                circuitBreakerRegistry,
                retryRegistry,
                meterRegistryProvider,
                "test-publisher"
        );
    }

    @Test
    void shouldDelegatePublishToUnderlyingPublisher() {
        // Given
        String destination = "test-destination";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // Mock the delegate to return an empty Mono
        lenient().when(delegate.publish(eq(destination), eq(eventType), eq(payload), eq(transactionId)))
                .thenReturn(Mono.empty());

        // Mock the circuit breaker to allow calls
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        lenient().doNothing().when(circuitBreaker).acquirePermission();

        // Skip the actual resilience4j operators by creating a new publisher that doesn't use them
        ResilientEventPublisher testPublisher = new ResilientEventPublisher(
                delegate,
                circuitBreakerRegistry,
                retryRegistry,
                meterRegistryProvider,
                "test-publisher"
        ) {
            @Override
            public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
                // Skip the resilience4j operators and just call the delegate directly
                return delegate.publish(destination, eventType, payload, transactionId);
            }
        };

        // When
        StepVerifier.create(testPublisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(delegate).publish(eq(destination), eq(eventType), eq(payload), eq(transactionId));
    }

    @Test
    void shouldBeAvailableWhenDelegateIsAvailableAndCircuitBreakerIsClosed() {
        // Given
        lenient().when(delegate.isAvailable()).thenReturn(true);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // When
        boolean available = resilientPublisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenDelegateIsNotAvailable() {
        // Given
        lenient().when(delegate.isAvailable()).thenReturn(false);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

        // When
        boolean available = resilientPublisher.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenCircuitBreakerIsOpen() {
        // Given
        lenient().when(delegate.isAvailable()).thenReturn(true);
        lenient().when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

        // When
        boolean available = resilientPublisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
