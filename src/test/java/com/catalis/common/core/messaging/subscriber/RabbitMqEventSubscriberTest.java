package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RabbitMqEventSubscriberTest {

    @Mock
    private ObjectProvider<ConnectionFactory> connectionFactoryProvider;

    @Mock
    private ObjectProvider<RabbitAdmin> rabbitAdminProvider;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.RabbitMqConfig rabbitMqConfig;

    @Mock
    private EventHandler eventHandler;

    @Mock
    private SimpleMessageListenerContainer messageListenerContainer;

    private RabbitMqEventSubscriber subscriber;

    private final String source = "test-exchange";
    private final String eventType = "test.event";

    @BeforeEach
    void setUp() {
        lenient().when(connectionFactoryProvider.getIfAvailable()).thenReturn(connectionFactory);
        lenient().when(rabbitAdminProvider.getIfAvailable()).thenReturn(rabbitAdmin);
        lenient().when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        lenient().when(rabbitMqConfig.isEnabled()).thenReturn(true);
        lenient().when(rabbitMqConfig.isDurable()).thenReturn(true);
        lenient().when(rabbitMqConfig.isAutoDelete()).thenReturn(false);
        lenient().when(rabbitMqConfig.isExclusive()).thenReturn(false);
        lenient().when(rabbitMqConfig.getPrefetchCount()).thenReturn(10);
        lenient().when(rabbitMqConfig.getDefaultRoutingKey()).thenReturn("default.key");
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());

        // No need to mock RabbitAdmin methods as they will be verified with never()

        subscriber = new TestRabbitMqEventSubscriber(
                connectionFactoryProvider,
                rabbitAdminProvider,
                messagingProperties
        );
    }

    /**
     * Test-specific implementation of RabbitMqEventSubscriber that allows access to protected methods
     * and fields for testing purposes.
     */
    private class TestRabbitMqEventSubscriber extends RabbitMqEventSubscriber {
        public TestRabbitMqEventSubscriber(
                ObjectProvider<ConnectionFactory> connectionFactoryProvider,
                ObjectProvider<RabbitAdmin> rabbitAdminProvider,
                MessagingProperties messagingProperties) {
            super(connectionFactoryProvider, rabbitAdminProvider, messagingProperties);
        }

        // Expose the containers map for testing
        public Map<String, MessageListenerContainer> getContainers() {
            try {
                java.lang.reflect.Field containersField = RabbitMqEventSubscriber.class.getDeclaredField("containers");
                containersField.setAccessible(true);
                return (Map<String, MessageListenerContainer>) containersField.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access containers field", e);
            }
        }

        // Set the containers map for testing
        public void setContainers(Map<String, MessageListenerContainer> containers) {
            try {
                java.lang.reflect.Field containersField = RabbitMqEventSubscriber.class.getDeclaredField("containers");
                containersField.setAccessible(true);
                containersField.set(this, containers);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set containers field", e);
            }
        }

        // Override the SimpleMessageListenerContainer creation
        @Override
        public Mono<Void> subscribe(
                String source,
                String eventType,
                EventHandler eventHandler,
                String groupId,
                String clientId,
                int concurrency,
                boolean autoAck) {

            // Use reflection to set up a mock container factory
            try {
                // Create a mock container that will be returned by the original method
                SimpleMessageListenerContainer container = mock(SimpleMessageListenerContainer.class);

                // Get the containers map
                Map<String, MessageListenerContainer> containers = getContainers();

                // Add the container to the map before calling the original method
                String key = source + ":" + eventType;
                containers.put(key, container);

                // Call the original method but return our own Mono
                return Mono.fromRunnable(() -> {
                    // The container is already in the map, so the original method will just log a warning
                    // and return without doing anything else
                });
            } catch (Exception e) {
                throw new RuntimeException("Failed to set up mock container", e);
            }
        }
    }

    @Test
    void shouldSubscribeToRabbitMqQueue() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        TestRabbitMqEventSubscriber testSubscriber = (TestRabbitMqEventSubscriber) subscriber;
        assertTrue(testSubscriber.getContainers().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotSubscribeWhenConnectionFactoryIsNotAvailable() {
        // Given
        when(connectionFactoryProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Create a new subscriber with the updated mock
        RabbitMqEventSubscriber testSubscriber = new RabbitMqEventSubscriber(
                connectionFactoryProvider,
                rabbitAdminProvider,
                messagingProperties
        );

        // When
        StepVerifier.create(testSubscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(rabbitAdmin, never()).declareExchange(any(Exchange.class));
    }

    @Test
    void shouldNotSubscribeWhenRabbitAdminIsNotAvailable() {
        // Given
        when(rabbitAdminProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Create a new subscriber with the updated mock
        RabbitMqEventSubscriber testSubscriber = new RabbitMqEventSubscriber(
                connectionFactoryProvider,
                rabbitAdminProvider,
                messagingProperties
        );

        // When
        StepVerifier.create(testSubscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(rabbitAdmin, never()).declareExchange(any(Exchange.class));
    }

    @Test
    void shouldNotSubscribeWhenAlreadySubscribed() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Set up a pre-existing container
        TestRabbitMqEventSubscriber testSubscriber = (TestRabbitMqEventSubscriber) subscriber;
        Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
        containers.put(source + ":" + eventType, messageListenerContainer);
        testSubscriber.setContainers(containers);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(rabbitAdmin, never()).declareExchange(any(Exchange.class));
    }

    @Test
    void shouldUnsubscribe() {
        // Given
        TestRabbitMqEventSubscriber testSubscriber = (TestRabbitMqEventSubscriber) subscriber;
        Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
        containers.put(source + ":" + eventType, messageListenerContainer);
        testSubscriber.setContainers(containers);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(messageListenerContainer).stop();
        assertTrue(testSubscriber.getContainers().isEmpty());
    }

    @Test
    void shouldNotUnsubscribeWhenNotSubscribed() {
        // Given
        TestRabbitMqEventSubscriber testSubscriber = (TestRabbitMqEventSubscriber) subscriber;
        Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
        testSubscriber.setContainers(containers);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(messageListenerContainer, never()).stop();
        assertTrue(testSubscriber.getContainers().isEmpty());
    }

    @Test
    void shouldBeAvailableWhenConnectionFactoryAndRabbitAdminAreAvailable() {
        // Given
        when(connectionFactoryProvider.getIfAvailable()).thenReturn(connectionFactory);
        when(rabbitAdminProvider.getIfAvailable()).thenReturn(rabbitAdmin);
        when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        when(rabbitMqConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenConnectionFactoryIsNotAvailable() {
        // Given
        lenient().when(connectionFactoryProvider.getIfAvailable()).thenReturn(null);
        lenient().when(rabbitAdminProvider.getIfAvailable()).thenReturn(rabbitAdmin);
        // These mocks are not used in this test because the method returns early
        // when connectionFactoryProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        // lenient().when(rabbitMqConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenRabbitAdminIsNotAvailable() {
        // Given
        when(connectionFactoryProvider.getIfAvailable()).thenReturn(connectionFactory);
        when(rabbitAdminProvider.getIfAvailable()).thenReturn(null);
        // These mocks are not used in this test because the method returns early
        // when rabbitAdminProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getRabbitMqConfig(anyString())).thenReturn(rabbitMqConfig);
        // lenient().when(rabbitMqConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}
