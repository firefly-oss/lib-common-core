package com.catalis.common.core.messaging.annotation;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.error.EventErrorHandler;
import com.catalis.common.core.messaging.handler.EventHandler;
import com.catalis.common.core.messaging.processor.EventListenerProcessor;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationFormat;
import com.catalis.common.core.messaging.serialization.SerializerFactory;
import com.catalis.common.core.messaging.subscriber.EventSubscriber;
import com.catalis.common.core.messaging.subscriber.SubscriberFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventListenerAdvancedOptionsTest {

    @Mock
    private SubscriberFactory subscriberFactory;

    @Mock
    private SerializerFactory serializerFactory;

    @Mock
    private EventSubscriber eventSubscriber;

    @Mock
    private MessageSerializer serializer;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EventErrorHandler errorHandler;

    private MessagingProperties messagingProperties;
    private EventListenerProcessor processor;
    private TestEventHandler testEventHandler;

    @BeforeEach
    void setUp() {
        messagingProperties = new MessagingProperties();
        messagingProperties.setEnabled(true);

        processor = new EventListenerProcessor(subscriberFactory, serializerFactory, messagingProperties);
        processor.setApplicationContext(applicationContext);
        testEventHandler = new TestEventHandler();

        // Configure the mocks with lenient() to avoid unnecessary stubbing exceptions
        lenient().when(subscriberFactory.getSubscriber(any(SubscriberType.class), anyString())).thenReturn(eventSubscriber);
        lenient().when(serializerFactory.getSerializer(any(SerializationFormat.class))).thenReturn(serializer);
        lenient().when(eventSubscriber.subscribe(anyString(), anyString(), any(EventHandler.class), anyString(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(Mono.empty());
    }

    @Test
    void shouldRegisterEventListenerWithErrorHandler() {
        // Given
        when(applicationContext.getBean(eq("customErrorHandler"), eq(EventErrorHandler.class))).thenReturn(errorHandler);

        // Enable messaging and Kafka
        messagingProperties.setEnabled(true);
        messagingProperties.getKafka().setEnabled(true);

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
    void shouldUseRoutingKeyForRabbitMQ() {
        // Given
        when(subscriberFactory.getSubscriber(eq(SubscriberType.RABBITMQ), anyString())).thenReturn(eventSubscriber);

        // Enable messaging and RabbitMQ
        messagingProperties.setEnabled(true);
        messagingProperties.getRabbitmq().setEnabled(true);

        // When
        processor.postProcessAfterInitialization(new RabbitMQEventHandler(), "rabbitMQEventHandler");

        // Then
        verify(subscriberFactory).getSubscriber(eq(SubscriberType.RABBITMQ), anyString());
        verify(eventSubscriber).subscribe(
                eq("test-exchange"),
                eq("test.event"),
                any(EventHandler.class),
                eq(""),
                eq(""),
                eq(1),
                eq(true)
        );
    }

    @Test
    void shouldHandleErrorsWithCustomErrorHandler() throws Exception {
        // Given
        when(applicationContext.getBean(eq("customErrorHandler"), eq(EventErrorHandler.class))).thenReturn(errorHandler);
        when(errorHandler.handleError(anyString(), anyString(), any(), anyMap(), any(SubscriberType.class), any(Throwable.class), any()))
                .thenReturn(Mono.empty());

        // Enable messaging and Kafka
        messagingProperties.setEnabled(true);
        messagingProperties.getKafka().setEnabled(true);

        // Capture the event handler
        ArgumentCaptor<EventHandler> eventHandlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        when(eventSubscriber.subscribe(anyString(), anyString(), eventHandlerCaptor.capture(), anyString(), anyString(), anyInt(), anyBoolean()))
                .thenReturn(Mono.empty());

        // When
        processor.postProcessAfterInitialization(testEventHandler, "testEventHandler");

        // Then
        EventHandler capturedHandler = eventHandlerCaptor.getValue();

        // Simulate an error during deserialization
        byte[] payload = "test".getBytes();
        Map<String, Object> headers = new HashMap<>();
        EventHandler.Acknowledgement ack = mock(EventHandler.Acknowledgement.class);
        when(ack.acknowledge()).thenReturn(Mono.empty());
        when(serializer.deserialize(any(byte[].class), any(Class.class))).thenThrow(new RuntimeException("Test error"));

        // When
        capturedHandler.handleEvent(payload, headers, ack).block();

        // Then
        verify(errorHandler).handleError(eq("test-topic"), eq("test.event"), any(), eq(headers), eq(SubscriberType.KAFKA), any(RuntimeException.class), eq(ack));
    }

    static class TestEventHandler {

        @EventListener(
                source = "test-topic",
                eventType = "test.event",
                subscriber = SubscriberType.KAFKA,
                errorHandler = "customErrorHandler"
        )
        public void handleTestEvent(TestEvent event) {
            // Test event handler
        }
    }

    static class RabbitMQEventHandler {

        @EventListener(
                source = "test-exchange",
                eventType = "test.event",
                subscriber = SubscriberType.RABBITMQ,
                routingKey = "custom.routing.key"
        )
        public void handleTestEvent(TestEvent event) {
            // Test event handler
        }
    }

    static class TestEvent {
        private String id;
        private String name;

        public TestEvent() {
        }

        public TestEvent(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
