package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KafkaEventPublisherTest {

    @Mock
    private ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.KafkaConfig kafkaConfig;

    @InjectMocks
    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(messagingProperties.getKafkaConfig(anyString())).thenReturn(kafkaConfig);
        when(kafkaConfig.isEnabled()).thenReturn(true);
    }

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
