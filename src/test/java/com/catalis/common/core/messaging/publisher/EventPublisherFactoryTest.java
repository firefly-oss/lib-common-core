package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.annotation.PublisherType;
import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.resilience.ResilientEventPublisherFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventPublisherFactoryTest {

    @Mock
    private SpringEventPublisher springEventPublisher;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @Mock
    private RabbitMqEventPublisher rabbitMqEventPublisher;

    @Mock
    private SqsEventPublisher sqsEventPublisher;

    @Mock
    private GooglePubSubEventPublisher googlePubSubEventPublisher;

    @Mock
    private AzureServiceBusEventPublisher azureServiceBusEventPublisher;

    @Mock
    private RedisEventPublisher redisEventPublisher;

    @Mock
    private JmsEventPublisher jmsEventPublisher;

    @Mock
    private ResilientEventPublisherFactory resilientFactory;

    @Mock
    private MessagingProperties messagingProperties;

    @InjectMocks
    private EventPublisherFactory factory;

    private List<EventPublisher> publishers;

    @BeforeEach
    void setUp() {
        publishers = Arrays.asList(
                springEventPublisher,
                kafkaEventPublisher,
                rabbitMqEventPublisher,
                sqsEventPublisher,
                googlePubSubEventPublisher,
                azureServiceBusEventPublisher,
                redisEventPublisher,
                jmsEventPublisher
        );

        // Use reflection to set the publishers field
        try {
            java.lang.reflect.Field publishersField = EventPublisherFactory.class.getDeclaredField("publishers");
            publishersField.setAccessible(true);
            publishersField.set(factory, publishers);

            // Mock default connection ID
            when(messagingProperties.getDefaultConnectionId()).thenReturn("default");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set publishers field", e);
        }

        // Mock the resilient factory to return the same publisher
        lenient().when(resilientFactory.createResilientPublisher(any(EventPublisher.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnSpringEventPublisher() {
        // Given
        when(springEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.EVENT_BUS);

        // Then
        assertEquals(springEventPublisher, publisher);
    }

    @Test
    void shouldReturnKafkaEventPublisher() {
        // Given
        when(kafkaEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.KAFKA);

        // Then
        assertEquals(kafkaEventPublisher, publisher);
    }

    @Test
    void shouldReturnRabbitMqEventPublisher() {
        // Given
        when(rabbitMqEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.RABBITMQ);

        // Then
        assertEquals(rabbitMqEventPublisher, publisher);
    }

    @Test
    void shouldReturnSqsEventPublisher() {
        // Given
        when(sqsEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.SQS);

        // Then
        assertEquals(sqsEventPublisher, publisher);
    }

    @Test
    void shouldReturnGooglePubSubEventPublisher() {
        // Given
        when(googlePubSubEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.GOOGLE_PUBSUB);

        // Then
        assertEquals(googlePubSubEventPublisher, publisher);
    }

    @Test
    void shouldReturnAzureServiceBusEventPublisher() {
        // Given
        when(azureServiceBusEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.AZURE_SERVICE_BUS);

        // Then
        assertEquals(azureServiceBusEventPublisher, publisher);
    }

    @Test
    void shouldReturnRedisEventPublisher() {
        // Given
        when(redisEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.REDIS);

        // Then
        assertEquals(redisEventPublisher, publisher);
    }

    @Test
    void shouldReturnJmsEventPublisher() {
        // Given
        when(jmsEventPublisher.isAvailable()).thenReturn(true);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.JMS);

        // Then
        assertEquals(jmsEventPublisher, publisher);
    }

    @Test
    void shouldReturnNullWhenPublisherIsNotAvailable() {
        // Given
        when(kafkaEventPublisher.isAvailable()).thenReturn(false);

        // When
        EventPublisher publisher = factory.getPublisher(PublisherType.KAFKA);

        // Then
        assertNull(publisher);
    }
}
