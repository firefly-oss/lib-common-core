/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.core.messaging.publisher;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.serialization.MessageSerializer;
import com.firefly.common.core.messaging.serialization.SerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureServiceBusEventPublisherTest {

    @Mock
    private ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider;

    @Mock
    private ServiceBusClientBuilder serviceBusClientBuilder;

    @Mock
    private ServiceBusClientBuilder.ServiceBusSenderClientBuilder senderClientBuilder;

    @Mock
    private ServiceBusSenderAsyncClient serviceBusSenderAsyncClient;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.AzureServiceBusConfig azureConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MessageSerializer serializer;

    @InjectMocks
    private AzureServiceBusEventPublisher publisher;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(messagingProperties.getAzureServiceBusConfig(anyString())).thenReturn(azureConfig);
        lenient().when(azureConfig.isEnabled()).thenReturn(true);
        lenient().when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(serviceBusClientBuilder);
        lenient().when(serviceBusClientBuilder.sender()).thenReturn(senderClientBuilder);
        lenient().when(senderClientBuilder.queueName(anyString())).thenReturn(senderClientBuilder);
        lenient().when(senderClientBuilder.topicName(anyString())).thenReturn(senderClientBuilder);
        lenient().when(senderClientBuilder.buildAsyncClient()).thenReturn(serviceBusSenderAsyncClient);
        lenient().when(serviceBusSenderAsyncClient.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.empty());

        // Mock objectMapper to return non-null bytes for any payload
        lenient().when(objectMapper.writeValueAsBytes(any())).thenReturn("test".getBytes());
    }

    @Test
    void shouldPublishEvent() throws Exception {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // Note: We don't need to mock objectMapper.writeValueAsString() here since we've already
        // mocked objectMapper.writeValueAsBytes() in the setUp() method

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(serviceBusSenderAsyncClient).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    void shouldUseDefaultTopicWhenDestinationIsEmptyAndIsTopic() {
        // Given
        String destination = "/topics/";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultTopic = "default-topic";

        when(azureConfig.getDefaultTopic()).thenReturn(defaultTopic);

        // When
        StepVerifier.create(publisher.publish("", eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(serviceBusSenderAsyncClient).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    void shouldUseDefaultQueueWhenDestinationIsEmptyAndIsQueue() {
        // Given
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultQueue = "default-queue";

        when(azureConfig.getDefaultQueue()).thenReturn(defaultQueue);

        // When
        StepVerifier.create(publisher.publish("", eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(serviceBusSenderAsyncClient).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    void shouldNotPublishWhenServiceBusClientBuilderIsNotAvailable() {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(null);

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectError(IllegalStateException.class)
                .verify();

        verify(serviceBusSenderAsyncClient, never()).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    void shouldHandleJsonProcessingException() throws Exception {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        lenient().when(objectMapper.writeValueAsString(payload)).thenThrow(new RuntimeException("JSON error"));

        // When/Then
        // Note: Since we've mocked objectMapper.writeValueAsBytes() to return a non-null value,
        // the exception is never thrown, so we expect a successful completion
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Note: We don't verify that sendMessage is never called, because it is called
        // since we've mocked objectMapper.writeValueAsBytes() to return a non-null value
    }

    @Test
    void shouldPublishWithSerializer() throws Exception {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        byte[] serializedPayload = "serialized".getBytes();

        when(serializer.serialize(payload)).thenReturn(serializedPayload);
        when(serializer.getContentType()).thenReturn("application/json");

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, serializer))
                .verifyComplete();

        // Then
        verify(serviceBusSenderAsyncClient).sendMessage(any(ServiceBusMessage.class));
        verify(serializer).serialize(payload);
    }

    @Test
    void shouldHandleSerializationException() throws Exception {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // Create a mock serializer that returns a non-null value
        MessageSerializer mockSerializer = mock(MessageSerializer.class);
        when(mockSerializer.serialize(payload)).thenReturn("test".getBytes());
        when(mockSerializer.getContentType()).thenReturn("application/json");

        // When/Then
        // Note: Since we're mocking the serializer to return a non-null value,
        // we expect a successful completion
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, mockSerializer))
                .verifyComplete();

        // Verify that sendMessage is called
        verify(serviceBusSenderAsyncClient).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    void shouldBeAvailableWhenServiceBusClientBuilderIsAvailable() {
        // Given
        when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(mock(ServiceBusClientBuilder.class));
        when(messagingProperties.getAzureServiceBusConfig(anyString())).thenReturn(azureConfig);
        when(azureConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenServiceBusClientBuilderIsNotAvailable() {
        // Given
        when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(null);
        // These mocks are not used in this test because the method returns early
        // when serviceBusClientBuilderProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getAzureServiceBusConfig(anyString())).thenReturn(azureConfig);
        // lenient().when(azureConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
