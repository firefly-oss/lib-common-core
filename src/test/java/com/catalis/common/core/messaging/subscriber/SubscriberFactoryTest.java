package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.annotation.SubscriberType;
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
    private com.catalis.common.core.messaging.resilience.ResilientEventSubscriberFactory resilientFactory;

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

        factory = new SubscriberFactory(subscribers, resilientFactory);

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
