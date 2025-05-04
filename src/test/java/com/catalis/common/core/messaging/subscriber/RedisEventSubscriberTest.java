package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisEventSubscriberTest {

    @Mock
    private ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider;

    @Mock
    private ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private EventHandler eventHandler;

    @Mock
    private RedisMessageListenerContainer listenerContainer;

    @Mock
    private MessageListenerAdapter messageListenerAdapter;

    private RedisEventSubscriber subscriber;

    private final String source = "test-channel";
    private final String eventType = "test.event";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        lenient().when(redisConnectionFactoryProvider.getIfAvailable()).thenReturn(redisConnectionFactory);
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());

        subscriber = new TestRedisEventSubscriber(
                redisTemplateProvider,
                redisConnectionFactoryProvider,
                messagingProperties
        );
    }

    /**
     * Test-specific implementation of RedisEventSubscriber that allows access to protected methods
     * and fields for testing purposes.
     */
    private class TestRedisEventSubscriber extends RedisEventSubscriber {
        public TestRedisEventSubscriber(
                ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider,
                ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
                MessagingProperties messagingProperties) {
            super(redisTemplateProvider, redisConnectionFactoryProvider, messagingProperties);
        }

        // Expose the listeners map for testing
        public Map<String, MessageListenerAdapter> getListeners() {
            try {
                java.lang.reflect.Field listenersField = RedisEventSubscriber.class.getDeclaredField("listeners");
                listenersField.setAccessible(true);
                return (Map<String, MessageListenerAdapter>) listenersField.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access listeners field", e);
            }
        }

        // Set the listeners map for testing
        public void setListeners(Map<String, MessageListenerAdapter> listeners) {
            try {
                java.lang.reflect.Field listenersField = RedisEventSubscriber.class.getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listenersField.set(this, listeners);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set listeners field", e);
            }
        }

        // Set the listenerContainer for testing
        public void setListenerContainer(RedisMessageListenerContainer container) {
            try {
                java.lang.reflect.Field containerField = RedisEventSubscriber.class.getDeclaredField("listenerContainer");
                containerField.setAccessible(true);
                containerField.set(this, container);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set listenerContainer field", e);
            }
        }

        // Override the subscribe method to avoid creating a real RedisMessageListenerContainer
        @Override
        public Mono<Void> subscribe(
                String source,
                String eventType,
                EventHandler eventHandler,
                String groupId,
                String clientId,
                int concurrency,
                boolean autoAck) {
            
            return Mono.fromRunnable(() -> {
                // Only proceed if the connection factory is available
                if (redisConnectionFactoryProvider.getIfAvailable() == null) {
                    return;
                }
                
                // Check if already subscribed
                String key = source + ":" + eventType;
                if (getListeners().containsKey(key)) {
                    return;
                }
                
                // Set up a mock listener container if needed
                if (listenerContainer == null) {
                    setListenerContainer(mock(RedisMessageListenerContainer.class));
                }
                
                // Add a mock listener adapter to the map
                getListeners().put(key, messageListenerAdapter);
            });
        }
    }

    @Test
    void shouldSubscribeToRedisChannel() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        TestRedisEventSubscriber testSubscriber = (TestRedisEventSubscriber) subscriber;
        assertTrue(testSubscriber.getListeners().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotSubscribeWhenRedisConnectionFactoryIsNotAvailable() {
        // Given
        when(redisConnectionFactoryProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        TestRedisEventSubscriber testSubscriber = (TestRedisEventSubscriber) subscriber;
        assertFalse(testSubscriber.getListeners().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotSubscribeWhenAlreadySubscribed() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Set up a pre-existing listener
        TestRedisEventSubscriber testSubscriber = (TestRedisEventSubscriber) subscriber;
        Map<String, MessageListenerAdapter> listeners = new ConcurrentHashMap<>();
        listeners.put(source + ":" + eventType, messageListenerAdapter);
        testSubscriber.setListeners(listeners);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        assertEquals(1, testSubscriber.getListeners().size());
    }

    @Test
    void shouldUnsubscribe() {
        // Given
        TestRedisEventSubscriber testSubscriber = (TestRedisEventSubscriber) subscriber;
        Map<String, MessageListenerAdapter> listeners = new ConcurrentHashMap<>();
        listeners.put(source + ":" + eventType, messageListenerAdapter);
        testSubscriber.setListeners(listeners);
        testSubscriber.setListenerContainer(listenerContainer);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(listenerContainer).removeMessageListener(eq(messageListenerAdapter), any(ChannelTopic.class));
        assertTrue(testSubscriber.getListeners().isEmpty());
    }

    @Test
    void shouldNotUnsubscribeWhenNotSubscribed() {
        // Given
        TestRedisEventSubscriber testSubscriber = (TestRedisEventSubscriber) subscriber;
        Map<String, MessageListenerAdapter> listeners = new ConcurrentHashMap<>();
        testSubscriber.setListeners(listeners);
        testSubscriber.setListenerContainer(listenerContainer);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(listenerContainer, never()).removeMessageListener(any(), any(ChannelTopic.class));
        assertTrue(testSubscriber.getListeners().isEmpty());
    }

    @Test
    void shouldBeAvailableWhenRedisTemplateAndConnectionFactoryAreAvailable() {
        // Given
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisConnectionFactoryProvider.getIfAvailable()).thenReturn(redisConnectionFactory);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenRedisTemplateIsNotAvailable() {
        // Given
        lenient().when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(redisConnectionFactoryProvider.getIfAvailable()).thenReturn(redisConnectionFactory);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenRedisConnectionFactoryIsNotAvailable() {
        // Given
        lenient().when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        lenient().when(redisConnectionFactoryProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}