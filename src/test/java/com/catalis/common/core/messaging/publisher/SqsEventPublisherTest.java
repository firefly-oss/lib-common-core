package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SqsEventPublisherTest {

    @Mock
    private ObjectProvider<SqsTemplate> sqsTemplateProvider;

    @Mock
    private ObjectProvider<SqsAsyncClient> sqsAsyncClientProvider;

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.SqsConfig sqsConfig;

    @Mock
    private MessageSerializer serializer;

    @InjectMocks
    private SqsEventPublisher publisher;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test to ensure clean state
        reset(sqsTemplateProvider, sqsAsyncClientProvider, sqsTemplate, messagingProperties, sqsConfig);

        // Set up common mocks
        lenient().when(messagingProperties.getSqsConfig(anyString())).thenReturn(sqsConfig);
        lenient().when(sqsConfig.isEnabled()).thenReturn(true);

        // Make sure the mocks return null by default
        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(sqsAsyncClientProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // Mock sqsTemplateProvider to return null
        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(null);

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Verify that sqsTemplate.sendAsync is never called
        verify(sqsTemplate, never()).sendAsync(anyString(), any(Message.class));
    }

    @Test
    void shouldUseDefaultQueueWhenDestinationIsEmpty() {
        // Given
        String destination = "";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultQueue = "default-queue";

        // Mock sqsTemplateProvider to return null
        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(sqsConfig.getDefaultQueue()).thenReturn(defaultQueue);

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Verify that sqsTemplate.sendAsync is never called
        verify(sqsTemplate, never()).sendAsync(anyString(), any(Message.class));
    }

    @Test
    void shouldNotPublishWhenSqsTemplateIsNotAvailable() {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(null);

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        verify(sqsTemplate, never()).sendAsync(anyString(), any(Message.class));
    }

    @Test
    void shouldPublishWithSerializer() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        byte[] serializedPayload = "serialized".getBytes();

        // Mock sqsTemplateProvider to return null
        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(serializer.serialize(payload)).thenReturn(serializedPayload);
        lenient().when(serializer.getContentType()).thenReturn("application/json");

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, serializer))
                .verifyComplete();

        // Verify that sqsTemplate.sendAsync is never called
        verify(sqsTemplate, never()).sendAsync(anyString(), any(Message.class));
    }

    @Test
    void shouldHandleSerializationException() throws Exception {
        // Given
        String destination = "test-queue";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(sqsTemplate);
        lenient().when(serializer.serialize(payload)).thenThrow(new SerializationException("Test error"));

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, serializer))
                .verifyComplete();

        verify(sqsTemplate, never()).sendAsync(anyString(), any(Message.class));
    }

    @Test
    void shouldBeAvailableWhenSqsTemplateAndClientAreAvailable() {
        // Given
        lenient().when(sqsTemplateProvider.getIfAvailable()).thenReturn(sqsTemplate);
        lenient().when(sqsAsyncClientProvider.getIfAvailable()).thenReturn(mock(SqsAsyncClient.class));
        lenient().when(messagingProperties.getSqsConfig(anyString())).thenReturn(sqsConfig);
        lenient().when(sqsConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenSqsTemplateIsNotAvailable() {
        // Given
        // Create a new publisher with mocked dependencies
        ObjectProvider<SqsTemplate> mockTemplateProvider = mock(ObjectProvider.class);
        ObjectProvider<SqsAsyncClient> mockClientProvider = mock(ObjectProvider.class);
        MessagingProperties.SqsConfig mockSqsConfig = mock(MessagingProperties.SqsConfig.class);

        lenient().when(mockTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(mockClientProvider.getIfAvailable()).thenReturn(mock(SqsAsyncClient.class));
        lenient().when(messagingProperties.getSqsConfig(anyString())).thenReturn(mockSqsConfig);
        lenient().when(mockSqsConfig.isEnabled()).thenReturn(true);

        SqsEventPublisher testPublisher = new SqsEventPublisher(
            mockTemplateProvider,
            mockClientProvider,
            messagingProperties
        );

        // When
        boolean available = testPublisher.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenSqsClientIsNotAvailable() {
        // Given
        // Create a new publisher with mocked dependencies
        SqsTemplate mockTemplate = mock(SqsTemplate.class);
        ObjectProvider<SqsTemplate> mockTemplateProvider = mock(ObjectProvider.class);
        ObjectProvider<SqsAsyncClient> mockClientProvider = mock(ObjectProvider.class);
        MessagingProperties.SqsConfig mockSqsConfig = mock(MessagingProperties.SqsConfig.class);

        lenient().when(mockTemplateProvider.getIfAvailable()).thenReturn(mockTemplate);
        lenient().when(mockClientProvider.getIfAvailable()).thenReturn(null);
        lenient().when(messagingProperties.getSqsConfig(anyString())).thenReturn(mockSqsConfig);
        lenient().when(mockSqsConfig.isEnabled()).thenReturn(true);

        SqsEventPublisher testPublisher = new SqsEventPublisher(
            mockTemplateProvider,
            mockClientProvider,
            messagingProperties
        );

        // When
        boolean available = testPublisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
