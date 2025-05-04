package com.catalis.common.core.messaging.processor;

import com.catalis.common.core.messaging.annotation.EventListener;
import com.catalis.common.core.messaging.annotation.SubscriberType;
import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.JsonSerializer;
import com.catalis.common.core.messaging.serialization.SerializationFormat;
import com.catalis.common.core.messaging.serialization.SerializerFactory;
import com.catalis.common.core.messaging.handler.EventHandler;
import com.catalis.common.core.messaging.subscriber.EventSubscriber;
import com.catalis.common.core.messaging.subscriber.SubscriberFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventListenerProcessorTest {

    @Mock
    private SubscriberFactory subscriberFactory;

    @Mock
    private SerializerFactory serializerFactory;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EventSubscriber eventSubscriber;

    @Mock
    private JsonSerializer jsonSerializer;

    @InjectMocks
    private EventListenerProcessor processor;

    private TestEventHandler testEventHandler;

    @BeforeEach
    void setUp() {
        testEventHandler = new TestEventHandler();
        processor.setApplicationContext(applicationContext);

        // Configure messaging properties
        lenient().when(messagingProperties.isEnabled()).thenReturn(true);

        MessagingProperties.KafkaConfig kafkaConfig = new MessagingProperties.KafkaConfig();
        kafkaConfig.setEnabled(true);
        kafkaConfig.setDefaultTopic("default-topic");
        lenient().when(messagingProperties.getKafka()).thenReturn(kafkaConfig);

        // Configure serializer factory
        lenient().when(serializerFactory.getSerializer(any(SerializationFormat.class))).thenReturn(jsonSerializer);

        // Configure subscriber factory
        lenient().when(subscriberFactory.getSubscriber(any(SubscriberType.class), anyString())).thenReturn(eventSubscriber);

        // Configure event subscriber
        lenient().when(eventSubscriber.subscribe(anyString(), anyString(), any(EventHandler.class),
                anyString(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(Mono.empty());
    }

    @Test
    void shouldRegisterEventListener() {
        // When
        processor.postProcessAfterInitialization(testEventHandler, "testEventHandler");

        // Then
        verify(subscriberFactory).getSubscriber(eq(SubscriberType.KAFKA), anyString());
        verify(eventSubscriber).subscribe(
                eq("test-topic"),
                eq("test.event"),
                any(EventHandler.class),
                eq(""),
                eq(""),
                eq(1),
                eq(true)
        );
    }

    @Test
    void shouldNotRegisterEventListenerWhenMessagingIsDisabled() {
        // Given
        when(messagingProperties.isEnabled()).thenReturn(false);

        // When
        processor.postProcessAfterInitialization(testEventHandler, "testEventHandler");

        // Then
        verify(subscriberFactory, never()).getSubscriber(any());
        verify(eventSubscriber, never()).subscribe(
                anyString(),
                anyString(),
                any(EventHandler.class),
                anyString(),
                anyString(),
                anyInt(),
                anyBoolean()
        );
    }

    @Test
    void shouldNotRegisterEventListenerWhenSubscriberTypeIsDisabled() {
        // Given
        MessagingProperties.KafkaConfig kafkaConfig = new MessagingProperties.KafkaConfig();
        kafkaConfig.setEnabled(false);
        when(messagingProperties.getKafka()).thenReturn(kafkaConfig);

        // When
        processor.postProcessAfterInitialization(testEventHandler, "testEventHandler");

        // Then
        verify(subscriberFactory, never()).getSubscriber(any());
        verify(eventSubscriber, never()).subscribe(
                anyString(),
                anyString(),
                any(EventHandler.class),
                anyString(),
                anyString(),
                anyInt(),
                anyBoolean()
        );
    }

    @Test
    void shouldNotRegisterEventListenerWhenNoSubscriberIsAvailable() {
        // Given
        when(subscriberFactory.getSubscriber(any(SubscriberType.class), anyString())).thenReturn(null);

        // When
        processor.postProcessAfterInitialization(testEventHandler, "testEventHandler");

        // Then
        verify(subscriberFactory).getSubscriber(any(), anyString());
        verify(eventSubscriber, never()).subscribe(
                anyString(),
                anyString(),
                any(EventHandler.class),
                anyString(),
                anyString(),
                anyInt(),
                anyBoolean()
        );
    }

    static class TestEventHandler {

        @EventListener(
                source = "test-topic",
                eventType = "test.event",
                subscriber = SubscriberType.KAFKA
        )
        public void handleTestEvent(TestEvent event) {
            // Test event handler
        }
    }

    static class TestEvent {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
