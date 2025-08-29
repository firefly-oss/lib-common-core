package com.firefly.common.core.messaging.publisher;

import com.firefly.common.core.messaging.event.GenericApplicationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SpringEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SpringEventPublisher publisher;

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-destination";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(applicationEventPublisher).publishEvent(any(GenericApplicationEvent.class));
    }

    @Test
    void shouldAlwaysBeAvailable() {
        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }
}
