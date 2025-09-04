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


package com.firefly.common.core.messaging.subscriber;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.handler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureServiceBusEventSubscriberTest {

    @Mock
    private ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider;

    @Mock
    private ServiceBusClientBuilder serviceBusClientBuilder;

    @Mock
    private ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder;

    @Mock
    private ServiceBusProcessorClient processorClient;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.AzureServiceBusConfig azureServiceBusConfig;

    @Mock
    private EventHandler eventHandler;

    private AzureServiceBusEventSubscriber subscriber;

    private final String source = "test-queue";
    private final String topicSource = "test-namespace/topics/test-topic";
    private final String eventType = "test.event";

    @BeforeEach
    void setUp() {
        lenient().when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(serviceBusClientBuilder);
        lenient().when(serviceBusClientBuilder.processor()).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.queueName(anyString())).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.topicName(anyString())).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.subscriptionName(anyString())).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.maxConcurrentCalls(anyInt())).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.processMessage(any())).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.processError(any())).thenReturn(processorClientBuilder);
        lenient().when(processorClientBuilder.buildProcessorClient()).thenReturn(processorClient);
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());
        lenient().when(messagingProperties.getAzureServiceBusConfig(anyString())).thenReturn(azureServiceBusConfig);
        lenient().when(azureServiceBusConfig.isEnabled()).thenReturn(true);

        subscriber = new TestAzureServiceBusEventSubscriber(
                serviceBusClientBuilderProvider,
                messagingProperties
        );
    }

    /**
     * Test-specific implementation of AzureServiceBusEventSubscriber that allows access to protected methods
     * and fields for testing purposes.
     */
    private class TestAzureServiceBusEventSubscriber extends AzureServiceBusEventSubscriber {
        public TestAzureServiceBusEventSubscriber(
                ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider,
                MessagingProperties messagingProperties) {
            super(serviceBusClientBuilderProvider, messagingProperties);
        }

        // Expose the processors map for testing
        public Map<String, ServiceBusProcessorClient> getProcessors() {
            try {
                java.lang.reflect.Field processorsField = AzureServiceBusEventSubscriber.class.getDeclaredField("processors");
                processorsField.setAccessible(true);
                return (Map<String, ServiceBusProcessorClient>) processorsField.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access processors field", e);
            }
        }

        // Set the processors map for testing
        public void setProcessors(Map<String, ServiceBusProcessorClient> processors) {
            try {
                java.lang.reflect.Field processorsField = AzureServiceBusEventSubscriber.class.getDeclaredField("processors");
                processorsField.setAccessible(true);
                processorsField.set(this, processors);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set processors field", e);
            }
        }
    }

    @Test
    void shouldSubscribeToQueue() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(processorClientBuilder).queueName(source);
        verify(processorClientBuilder).maxConcurrentCalls(concurrency);
        verify(processorClientBuilder).processMessage(any());
        verify(processorClientBuilder).processError(any());
        verify(processorClient).start();
    }

    @Test
    void shouldSubscribeToTopic() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(topicSource, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(processorClientBuilder).topicName(topicSource);
        verify(processorClientBuilder).subscriptionName(groupId);
        verify(processorClientBuilder).maxConcurrentCalls(concurrency);
        verify(processorClientBuilder).processMessage(any());
        verify(processorClientBuilder).processError(any());
        verify(processorClient).start();
    }

    @Test
    void shouldUseDefaultSubscriptionNameWhenGroupIdIsEmpty() {
        // Given
        String groupId = "";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(topicSource, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(processorClientBuilder).subscriptionName("subscription-" + topicSource);
    }

    @Test
    void shouldNotSubscribeWhenClientBuilderIsNotAvailable() {
        // Given
        when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(processorClientBuilder, never()).queueName(anyString());
        verify(processorClient, never()).start();
    }

    @Test
    void shouldNotSubscribeWhenAlreadySubscribed() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Set up a pre-existing processor
        TestAzureServiceBusEventSubscriber testSubscriber = (TestAzureServiceBusEventSubscriber) subscriber;
        Map<String, ServiceBusProcessorClient> processors = new ConcurrentHashMap<>();
        processors.put(source + ":" + eventType, processorClient);
        testSubscriber.setProcessors(processors);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(processorClientBuilder, never()).queueName(anyString());
        verify(processorClient, never()).start();
    }

    @Test
    void shouldUnsubscribe() {
        // Given
        TestAzureServiceBusEventSubscriber testSubscriber = (TestAzureServiceBusEventSubscriber) subscriber;
        Map<String, ServiceBusProcessorClient> processors = new ConcurrentHashMap<>();
        processors.put(source + ":" + eventType, processorClient);
        testSubscriber.setProcessors(processors);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(processorClient).close();
        assertTrue(testSubscriber.getProcessors().isEmpty());
    }

    @Test
    void shouldNotUnsubscribeWhenNotSubscribed() {
        // Given
        TestAzureServiceBusEventSubscriber testSubscriber = (TestAzureServiceBusEventSubscriber) subscriber;
        Map<String, ServiceBusProcessorClient> processors = new ConcurrentHashMap<>();
        testSubscriber.setProcessors(processors);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(processorClient, never()).close();
    }

    @Test
    void shouldBeAvailableWhenClientBuilderIsAvailable() {
        // Given
        when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(serviceBusClientBuilder);
        when(messagingProperties.getAzureServiceBusConfig(anyString())).thenReturn(azureServiceBusConfig);
        when(azureServiceBusConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenClientBuilderIsNotAvailable() {
        // Given
        when(serviceBusClientBuilderProvider.getIfAvailable()).thenReturn(null);
        // These mocks are not used in this test because the method returns early
        // when serviceBusClientBuilderProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getAzureServiceBusConfig(anyString())).thenReturn(azureServiceBusConfig);
        // lenient().when(azureServiceBusConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}
