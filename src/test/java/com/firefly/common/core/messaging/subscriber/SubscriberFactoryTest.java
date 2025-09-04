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

import com.firefly.common.core.messaging.annotation.SubscriberType;
import com.firefly.common.core.messaging.config.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubscriberFactoryTest {

    @Mock
    private SpringEventSubscriber springEventSubscriber;

    @Mock
    private KafkaEventSubscriber kafkaEventSubscriber;

    @Mock
    private RabbitMqEventSubscriber rabbitMqEventSubscriber;

    @Mock
    private SqsEventSubscriber sqsEventSubscriber;

    @Mock
    private GooglePubSubEventSubscriber googlePubSubEventSubscriber;

    @Mock
    private AzureServiceBusEventSubscriber azureServiceBusEventSubscriber;

    @Mock
    private RedisEventSubscriber redisEventSubscriber;

    @Mock
    private JmsEventSubscriber jmsEventSubscriber;

    @Mock
    private KinesisEventSubscriber kinesisEventSubscriber;

    @Mock
    private com.firefly.common.core.messaging.resilience.ResilientEventSubscriberFactory resilientFactory;

    @Mock
    private MessagingProperties messagingProperties;

    private SubscriberFactory factory;

    @BeforeEach
    void setUp() {
        List<EventSubscriber> subscribers = Arrays.asList(
                springEventSubscriber,
                kafkaEventSubscriber,
                rabbitMqEventSubscriber,
                sqsEventSubscriber,
                googlePubSubEventSubscriber,
                azureServiceBusEventSubscriber,
                redisEventSubscriber,
                jmsEventSubscriber,
                kinesisEventSubscriber
        );

        factory = new SubscriberFactory(subscribers, resilientFactory, messagingProperties);

        // Mock the resilient factory to return the original subscriber
        lenient().when(resilientFactory.createResilientSubscriber(any(EventSubscriber.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnSpringEventSubscriber() {
        // Given
        when(springEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.EVENT_BUS);

        // Then
        assertEquals(springEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnKafkaEventSubscriber() {
        // Given
        when(kafkaEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.KAFKA);

        // Then
        assertEquals(kafkaEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnRabbitMqEventSubscriber() {
        // Given
        when(rabbitMqEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.RABBITMQ);

        // Then
        assertEquals(rabbitMqEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnSqsEventSubscriber() {
        // Given
        when(sqsEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.SQS);

        // Then
        assertEquals(sqsEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnGooglePubSubEventSubscriber() {
        // Given
        when(googlePubSubEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.GOOGLE_PUBSUB);

        // Then
        assertEquals(googlePubSubEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnAzureServiceBusEventSubscriber() {
        // Given
        when(azureServiceBusEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.AZURE_SERVICE_BUS);

        // Then
        assertEquals(azureServiceBusEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnRedisEventSubscriber() {
        // Given
        when(redisEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.REDIS);

        // Then
        assertEquals(redisEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnJmsEventSubscriber() {
        // Given
        when(jmsEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.JMS);

        // Then
        assertEquals(jmsEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnKinesisEventSubscriber() {
        // Given
        when(kinesisEventSubscriber.isAvailable()).thenReturn(true);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.KINESIS);

        // Then
        assertEquals(kinesisEventSubscriber, subscriber);
    }

    @Test
    void shouldReturnNullWhenSubscriberIsNotAvailable() {
        // Given
        when(kafkaEventSubscriber.isAvailable()).thenReturn(false);

        // When
        EventSubscriber subscriber = factory.getSubscriber(SubscriberType.KAFKA);

        // Then
        assertNull(subscriber);
    }
}
