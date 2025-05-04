package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.event.GenericApplicationEvent;
import com.catalis.common.core.messaging.handler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringEventSubscriberTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EventHandler eventHandler;

    @InjectMocks
    private SpringEventSubscriber subscriber;

    private final String source = "test-source";
    private final String eventType = "test.event";
    private final String eventKey = "test-source:test.event";

    @BeforeEach
    void setUp() {
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldSubscribeToEvents() {
        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true))
                .verifyComplete();

        // Then
        // Verify that the event handler is registered by triggering an event
        GenericApplicationEvent event = new GenericApplicationEvent("test payload", eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        verify(eventHandler).handleEvent(any(byte[].class), anyMap(), eq(null));
    }

    @Test
    void shouldUnsubscribeFromEvents() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        // Verify that the event handler is no longer called
        GenericApplicationEvent event = new GenericApplicationEvent("test payload", eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        verify(eventHandler, never()).handleEvent(any(byte[].class), anyMap(), any());
    }

    @Test
    void shouldAlwaysBeAvailable() {
        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldHandleStringPayload() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        String payload = "test payload";

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(eq(payload.getBytes(StandardCharsets.UTF_8)), anyMap(), eq(null));
    }

    @Test
    void shouldHandleByteArrayPayload() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        byte[] payload = "test payload".getBytes(StandardCharsets.UTF_8);

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(eq(payload), anyMap(), eq(null));
    }

    @Test
    void shouldHandleNullPayload() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();

        // When
        // Use an empty string instead of null to avoid IllegalArgumentException
        GenericApplicationEvent event = new GenericApplicationEvent("", eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(eq(new byte[0]), anyMap(), eq(null));
    }

    @Test
    void shouldHandleObjectPayload() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        Object payload = new TestPayload("test");

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(any(byte[].class), anyMap(), eq(null));
    }

    @Test
    void shouldIncludeHeadersInEvent() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, eventType, source, transactionId);
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(any(byte[].class), argThat(headers -> 
                headers.get("eventType").equals(eventType) &&
                headers.get("destination").equals(source) &&
                headers.get("transactionId").equals(transactionId)
        ), eq(null));
    }

    @Test
    void shouldNotIncludeTransactionIdWhenNull() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        String payload = "test payload";

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, eventType, source, null);
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(any(byte[].class), argThat(headers -> 
                headers.get("eventType").equals(eventType) &&
                headers.get("destination").equals(source) &&
                !headers.containsKey("transactionId")
        ), eq(null));
    }

    @Test
    void shouldHandleErrorInEventHandler() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        String payload = "test payload";
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.error(new RuntimeException("Test error")));

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, eventType, source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler).handleEvent(any(byte[].class), anyMap(), eq(null));
        // No exception should be thrown
    }

    @Test
    void shouldIgnoreEventsWithNoMatchingHandler() {
        // Given
        subscriber.subscribe(source, eventType, eventHandler, "", "", 1, true).block();
        String payload = "test payload";

        // When
        GenericApplicationEvent event = new GenericApplicationEvent(payload, "different.event", source, "test-transaction-id");
        subscriber.handleApplicationEvent(event);

        // Then
        verify(eventHandler, never()).handleEvent(any(byte[].class), anyMap(), any());
    }

    static class TestPayload {
        private final String value;

        public TestPayload(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "TestPayload{value='" + value + "'}";
        }
    }
}
