package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RabbitMqEventPublisherTest {

    @Mock
    private ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.RabbitMqConfig rabbitMqConfig;

    @InjectMocks
    private RabbitMqEventPublisher publisher;

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-exchange";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        lenient().when(rabbitTemplateProvider.getIfAvailable()).thenReturn(rabbitTemplate);
        lenient().when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        lenient().when(rabbitMqConfig.getDefaultRoutingKey()).thenReturn("default");

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(rabbitTemplate).convertAndSend(eq(destination), eq(eventType), eq(payload), any(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    void shouldUseDefaultRoutingKeyWhenEventTypeIsEmpty() {
        // Given
        String destination = "test-exchange";
        String eventType = "";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultRoutingKey = "default-routing-key";

        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(rabbitTemplate);
        when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        when(rabbitMqConfig.getDefaultRoutingKey()).thenReturn(defaultRoutingKey);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(rabbitTemplate).convertAndSend(eq(destination), eq(defaultRoutingKey), eq(payload), any(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    void shouldNotPublishWhenRabbitTemplateIsNotAvailable() {
        // Given
        String destination = "test-exchange";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectErrorMatches(e -> e instanceof IllegalStateException && 
                                        "RabbitTemplate is not available".equals(e.getMessage()))
                .verify();

        // Then
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    void shouldBeAvailableWhenRabbitTemplateIsAvailable() {
        // Given
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(rabbitTemplate);
        when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        when(rabbitMqConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenRabbitTemplateIsNotAvailable() {
        // Given
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(null);
        // These mocks are not used in this test because the method returns early
        // when rabbitTemplateProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        // lenient().when(rabbitMqConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
