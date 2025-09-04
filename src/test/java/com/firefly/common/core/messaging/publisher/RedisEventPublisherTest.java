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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisEventPublisherTest {

    @Mock
    private ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider;

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.RedisConfig redisConfig;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisEventPublisher publisher;

    @BeforeEach
    void setUp() {
        lenient().when(messagingProperties.getRedisConfig(anyString())).thenReturn(redisConfig);
        lenient().when(redisConfig.isEnabled()).thenReturn(true);
    }

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-channel";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.convertAndSend(anyString(), any(Map.class))).thenReturn(Mono.just(1L));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(redisTemplate).convertAndSend(eq(destination), any(Map.class));
    }

    @Test
    void shouldUseDefaultChannelWhenDestinationIsEmpty() {
        // Given
        String destination = "";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        String defaultChannel = "default-channel";

        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisConfig.getDefaultChannel()).thenReturn(defaultChannel);
        when(redisTemplate.convertAndSend(anyString(), any(Map.class))).thenReturn(Mono.just(1L));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(redisTemplate).convertAndSend(eq(defaultChannel), any(Map.class));
    }

    @Test
    void shouldNotPublishWhenRedisTemplateIsNotAvailable() {
        // Given
        String destination = "test-channel";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(redisTemplate, never()).convertAndSend(anyString(), any(Map.class));
    }

    @Test
    void shouldHandlePublishException() {
        // Given
        String destination = "test-channel";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.convertAndSend(anyString(), any(Map.class))).thenReturn(Mono.error(new RuntimeException("Test error")));

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectError(RuntimeException.class)
                .verify();

        verify(redisTemplate).convertAndSend(eq(destination), any(Map.class));
    }

    @Test
    void shouldIncludeTransactionIdWhenProvided() {
        // Given
        String destination = "test-channel";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.convertAndSend(anyString(), any(Map.class))).thenReturn(Mono.just(1L));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(redisTemplate).convertAndSend(eq(destination), argThat((Map<String, Object> message) -> 
                message.containsKey("transactionId") && 
                message.get("transactionId").equals(transactionId)));
    }

    @Test
    void shouldNotIncludeTransactionIdWhenNull() {
        // Given
        String destination = "test-channel";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = null;

        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.convertAndSend(anyString(), any(Map.class))).thenReturn(Mono.just(1L));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(redisTemplate).convertAndSend(eq(destination), argThat((Map<String, Object> message) -> 
                !message.containsKey("transactionId")));
    }

    @Test
    void shouldBeAvailableWhenRedisTemplateIsAvailable() {
        // Given
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(messagingProperties.getRedisConfig(anyString())).thenReturn(redisConfig);
        when(redisConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenRedisTemplateIsNotAvailable() {
        // Given
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
        // These mocks are not used in this test because the method returns early
        // when redisTemplateProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getRedisConfig(anyString())).thenReturn(redisConfig);
        // lenient().when(redisConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
