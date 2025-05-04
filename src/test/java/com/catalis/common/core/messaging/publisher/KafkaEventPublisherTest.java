package com.catalis.common.core.messaging.publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaEventPublisherTest {

    @Mock
    private ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher publisher;

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void shouldNotPublishWhenKafkaTemplateIsNotAvailable() {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(kafkaTemplate, never()).send(any(Message.class));
    }

    @Test
    void shouldBeAvailableWhenKafkaTemplateIsAvailable() {
        // Given
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenKafkaTemplateIsNotAvailable() {
        // Given
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
