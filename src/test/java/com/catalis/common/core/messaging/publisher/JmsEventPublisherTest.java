package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import reactor.test.StepVerifier;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JmsEventPublisherTest {

    @Mock
    private ObjectProvider<JmsTemplate> jmsTemplateProvider;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.JmsConfig jmsConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Session session;

    @Mock
    private TextMessage textMessage;

    @InjectMocks
    private JmsEventPublisher publisher;

    @BeforeEach
    void setUp() {
        lenient().when(messagingProperties.getJmsConfig(anyString())).thenReturn(jmsConfig);
        lenient().when(jmsConfig.isEnabled()).thenReturn(true);
    }

    @Test
    void shouldPublishEvent() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String jsonMessage = "{\"payload\":\"test payload\",\"eventType\":\"test.event\",\"transactionId\":\"test-transaction-id\"}";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonMessage);
        lenient().when(session.createTextMessage(jsonMessage)).thenReturn(textMessage);
        lenient().doNothing().when(jmsTemplate).send(anyString(), any(MessageCreator.class));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(jmsTemplate).send(eq(destination), any(MessageCreator.class));
    }

    @Test
    void shouldUseDefaultDestinationWhenDestinationIsEmpty() throws Exception {
        // Given
        String destination = "";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultDestination = "default-queue";
        String jsonMessage = "{\"payload\":\"test payload\",\"eventType\":\"test.event\",\"transactionId\":\"test-transaction-id\"}";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(jmsConfig.getDefaultDestination()).thenReturn(defaultDestination);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonMessage);
        lenient().when(session.createTextMessage(jsonMessage)).thenReturn(textMessage);
        lenient().doNothing().when(jmsTemplate).send(anyString(), any(MessageCreator.class));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(jmsTemplate).send(eq(defaultDestination), any(MessageCreator.class));
    }

    @Test
    void shouldNotPublishWhenJmsTemplateIsNotAvailable() {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectErrorMatches(e -> e instanceof IllegalStateException && 
                                   "JmsTemplate is not available".equals(e.getMessage()))
                .verify();

        // Then
        verify(jmsTemplate, never()).send(anyString(), any(MessageCreator.class));
    }

    @Test
    void shouldHandleJsonProcessingException() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(objectMapper.writeValueAsString(any(Map.class))).thenThrow(new RuntimeException("JSON error"));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectErrorMatches(e -> e instanceof RuntimeException && 
                                   "JSON error".equals(e.getMessage()))
                .verify();

        // Then
        verify(jmsTemplate, never()).send(anyString(), any(MessageCreator.class));
    }

    @Test
    void shouldHandleJmsException() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String jsonMessage = "{\"payload\":\"test payload\",\"eventType\":\"test.event\",\"transactionId\":\"test-transaction-id\"}";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonMessage);
        doThrow(new RuntimeException("JMS error")).when(jmsTemplate).send(anyString(), any(MessageCreator.class));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectErrorMatches(e -> e instanceof RuntimeException && 
                                   "JMS error".equals(e.getMessage()))
                .verify();

        // Then
        verify(jmsTemplate).send(eq(destination), any(MessageCreator.class));
    }

    @Test
    void shouldSetEventTypeAndTransactionIdAsProperties() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String jsonMessage = "{\"payload\":\"test payload\",\"eventType\":\"test.event\",\"transactionId\":\"test-transaction-id\"}";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonMessage);
        when(session.createTextMessage(jsonMessage)).thenReturn(textMessage);

        // Capture the MessageCreator to verify it sets properties
        lenient().doAnswer(invocation -> {
            MessageCreator messageCreator = invocation.getArgument(1);
            messageCreator.createMessage(session);
            return null;
        }).when(jmsTemplate).send(anyString(), any(MessageCreator.class));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(textMessage).setStringProperty("eventType", eventType);
        verify(textMessage).setStringProperty("transactionId", transactionId);
    }

    @Test
    void shouldNotSetTransactionIdPropertyWhenNull() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = null;
        String jsonMessage = "{\"payload\":\"test payload\",\"eventType\":\"test.event\"}";

        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonMessage);
        when(session.createTextMessage(jsonMessage)).thenReturn(textMessage);

        // Capture the MessageCreator to verify it sets properties
        lenient().doAnswer(invocation -> {
            MessageCreator messageCreator = invocation.getArgument(1);
            messageCreator.createMessage(session);
            return null;
        }).when(jmsTemplate).send(anyString(), any(MessageCreator.class));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(textMessage).setStringProperty("eventType", eventType);
        verify(textMessage, never()).setStringProperty(eq("transactionId"), any());
    }

    @Test
    void shouldBeAvailableWhenJmsTemplateIsAvailable() {
        // Given
        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenJmsTemplateIsNotAvailable() {
        // Given
        when(jmsTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
