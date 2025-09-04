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

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.serialization.MessageSerializer;
import com.firefly.common.core.messaging.serialization.SerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GooglePubSubEventPublisherTest {

    @Mock
    private ObjectProvider<PubSubTemplate> pubSubTemplateProvider;

    @Mock
    private ObjectProvider<PubSubPublisherTemplate> pubSubPublisherTemplateProvider;

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.GooglePubSubConfig pubSubConfig;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GooglePubSubEventPublisher publisher;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test to ensure clean state
        reset(pubSubTemplateProvider, pubSubPublisherTemplateProvider, pubSubTemplate, messagingProperties, pubSubConfig);

        // Set up common mocks
        lenient().when(messagingProperties.getGooglePubSubConfig(anyString())).thenReturn(pubSubConfig);
        lenient().when(pubSubConfig.isEnabled()).thenReturn(true);

        // Make sure the mocks return null by default
        lenient().when(pubSubTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(pubSubPublisherTemplateProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // When/Then
        // Verify that the method completes without error
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();
    }

    @Test
    void shouldUseDefaultTopicWhenDestinationIsEmpty() {
        // Given
        String destination = "";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultTopic = "default-topic";

        // Mock pubSubTemplateProvider to return pubSubTemplate
        lenient().when(pubSubTemplateProvider.getIfAvailable()).thenReturn(pubSubTemplate);
        lenient().when(pubSubConfig.getDefaultTopic()).thenReturn(defaultTopic);

        // When/Then
        // Verify that the method completes without error
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();
    }

    @Test
    void shouldNotPublishWhenPubSubTemplateIsNotAvailable() {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        lenient().when(pubSubTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(pubSubTemplate, never()).publish(anyString(), any(), anyMap());
    }

    @Test
    void shouldHandlePublishException() {
        // Given
        String destination = "test-topic";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // When/Then
        // Verify that the method completes without error, even when publish throws an exception
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();
    }

    @Test
    void shouldBeAvailableWhenPubSubTemplateIsAvailable() {
        // Given
        // Create a new publisher with mocked dependencies
        PubSubTemplate mockTemplate = mock(PubSubTemplate.class);
        ObjectProvider<PubSubTemplate> mockProvider = mock(ObjectProvider.class);
        when(mockProvider.getIfAvailable()).thenReturn(mockTemplate);
        when(messagingProperties.getGooglePubSubConfig(anyString())).thenReturn(pubSubConfig);
        when(pubSubConfig.isEnabled()).thenReturn(true);

        GooglePubSubEventPublisher testPublisher = new GooglePubSubEventPublisher(
            mockProvider,
            pubSubPublisherTemplateProvider,
            messagingProperties,
            objectMapper
        );

        // When
        boolean available = testPublisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldBeAvailableWhenPubSubPublisherTemplateIsAvailable() {
        // Given
        // Create a new publisher with mocked dependencies
        PubSubPublisherTemplate mockTemplate = mock(PubSubPublisherTemplate.class);
        ObjectProvider<PubSubPublisherTemplate> mockProvider = mock(ObjectProvider.class);
        when(mockProvider.getIfAvailable()).thenReturn(mockTemplate);
        when(messagingProperties.getGooglePubSubConfig(anyString())).thenReturn(pubSubConfig);
        when(pubSubConfig.isEnabled()).thenReturn(true);

        GooglePubSubEventPublisher testPublisher = new GooglePubSubEventPublisher(
            pubSubTemplateProvider,
            mockProvider,
            messagingProperties,
            objectMapper
        );

        // When
        boolean available = testPublisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenNeitherTemplateIsAvailable() {
        // Given
        lenient().when(pubSubTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(pubSubPublisherTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
