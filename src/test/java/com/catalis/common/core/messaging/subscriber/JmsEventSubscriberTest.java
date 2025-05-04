package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.MessageListenerContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import jakarta.jms.MessageListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class JmsEventSubscriberTest {

    @Mock
    private ObjectProvider<JmsTemplate> jmsTemplateProvider;

    @Mock
    private ObjectProvider jmsListenerContainerFactoryProvider;

    @Mock
    private ObjectProvider<JmsListenerEndpointRegistry> jmsListenerEndpointRegistryProvider;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private JmsListenerContainerFactory jmsListenerContainerFactory;

    @Mock
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    @Mock
    private MessageListenerContainer messageListenerContainer;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private EventHandler eventHandler;

    private JmsEventSubscriber subscriber;

    private final String source = "test-queue";
    private final String eventType = "test.event";

    @BeforeEach
    void setUp() {
        lenient().when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        lenient().when(jmsListenerContainerFactoryProvider.getIfAvailable()).thenReturn(jmsListenerContainerFactory);
        lenient().when(jmsListenerEndpointRegistryProvider.getIfAvailable()).thenReturn(jmsListenerEndpointRegistry);
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());
        lenient().when(jmsListenerEndpointRegistry.getListenerContainer(anyString())).thenReturn(messageListenerContainer);

        subscriber = new TestJmsEventSubscriber(
                jmsTemplateProvider,
                jmsListenerContainerFactoryProvider,
                jmsListenerEndpointRegistryProvider,
                messagingProperties
        );
    }

    /**
     * Test-specific implementation of JmsEventSubscriber that allows access to protected methods
     * and fields for testing purposes.
     */
    private class TestJmsEventSubscriber extends JmsEventSubscriber {
        public TestJmsEventSubscriber(
                ObjectProvider<JmsTemplate> jmsTemplateProvider,
                ObjectProvider<JmsListenerContainerFactory<?>> jmsListenerContainerFactoryProvider,
                ObjectProvider<JmsListenerEndpointRegistry> jmsListenerEndpointRegistryProvider,
                MessagingProperties messagingProperties) {
            super(jmsTemplateProvider, jmsListenerContainerFactoryProvider, jmsListenerEndpointRegistryProvider, messagingProperties);
        }

        // Expose the endpointIds map for testing
        public Map<String, String> getEndpointIds() {
            try {
                java.lang.reflect.Field endpointIdsField = JmsEventSubscriber.class.getDeclaredField("endpointIds");
                endpointIdsField.setAccessible(true);
                return (Map<String, String>) endpointIdsField.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access endpointIds field", e);
            }
        }

        // Set the endpointIds map for testing
        public void setEndpointIds(Map<String, String> endpointIds) {
            try {
                java.lang.reflect.Field endpointIdsField = JmsEventSubscriber.class.getDeclaredField("endpointIds");
                endpointIdsField.setAccessible(true);
                endpointIdsField.set(this, endpointIds);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set endpointIds field", e);
            }
        }
    }

    @Test
    void shouldSubscribeToJmsDestination() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Capture the endpoint
        ArgumentCaptor<JmsListenerEndpoint> endpointCaptor = ArgumentCaptor.forClass(JmsListenerEndpoint.class);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(jmsListenerEndpointRegistry).registerListenerContainer(endpointCaptor.capture(), eq(jmsListenerContainerFactory), eq(true));

        // Verify the endpoint was configured correctly
        SimpleJmsListenerEndpoint endpoint = (SimpleJmsListenerEndpoint) endpointCaptor.getValue();
        assertEquals(source, endpoint.getDestination());
        assertEquals(String.valueOf(concurrency), endpoint.getConcurrency());
        assertNotNull(endpoint.getMessageListener());

        // Verify the endpoint ID was stored
        TestJmsEventSubscriber testSubscriber = (TestJmsEventSubscriber) subscriber;
        assertTrue(testSubscriber.getEndpointIds().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotSubscribeWhenJmsComponentsAreNotAvailable() {
        // Given
        when(jmsTemplateProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(jmsListenerEndpointRegistry, never()).registerListenerContainer(any(), any(), anyBoolean());
    }

    @Test
    void shouldNotSubscribeWhenAlreadySubscribed() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Set up a pre-existing endpoint ID
        TestJmsEventSubscriber testSubscriber = (TestJmsEventSubscriber) subscriber;
        Map<String, String> endpointIds = new ConcurrentHashMap<>();
        endpointIds.put(source + ":" + eventType, "existing-endpoint-id");
        testSubscriber.setEndpointIds(endpointIds);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(jmsListenerEndpointRegistry, never()).registerListenerContainer(any(), any(), anyBoolean());
    }

    @Test
    void shouldUnsubscribe() {
        // Given
        String endpointId = "test-endpoint-id";
        TestJmsEventSubscriber testSubscriber = (TestJmsEventSubscriber) subscriber;
        Map<String, String> endpointIds = new ConcurrentHashMap<>();
        endpointIds.put(source + ":" + eventType, endpointId);
        testSubscriber.setEndpointIds(endpointIds);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(jmsListenerEndpointRegistry).getListenerContainer(endpointId);
        verify(messageListenerContainer).stop();
        assertTrue(testSubscriber.getEndpointIds().isEmpty());
    }

    @Test
    void shouldNotUnsubscribeWhenNotSubscribed() {
        // Given
        TestJmsEventSubscriber testSubscriber = (TestJmsEventSubscriber) subscriber;
        Map<String, String> endpointIds = new ConcurrentHashMap<>();
        testSubscriber.setEndpointIds(endpointIds);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(jmsListenerEndpointRegistry, never()).getListenerContainer(anyString());
        verify(messageListenerContainer, never()).stop();
    }

    @Test
    void shouldBeAvailableWhenAllJmsComponentsAreAvailable() {
        // Given
        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(jmsListenerContainerFactoryProvider.getIfAvailable()).thenReturn(jmsListenerContainerFactory);
        when(jmsListenerEndpointRegistryProvider.getIfAvailable()).thenReturn(jmsListenerEndpointRegistry);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenJmsTemplateIsNotAvailable() {
        // Given
        lenient().when(jmsTemplateProvider.getIfAvailable()).thenReturn(null);
        lenient().when(jmsListenerContainerFactoryProvider.getIfAvailable()).thenReturn(jmsListenerContainerFactory);
        lenient().when(jmsListenerEndpointRegistryProvider.getIfAvailable()).thenReturn(jmsListenerEndpointRegistry);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenJmsListenerContainerFactoryIsNotAvailable() {
        // Given
        lenient().when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        lenient().when(jmsListenerContainerFactoryProvider.getIfAvailable()).thenReturn(null);
        lenient().when(jmsListenerEndpointRegistryProvider.getIfAvailable()).thenReturn(jmsListenerEndpointRegistry);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void shouldNotBeAvailableWhenJmsListenerEndpointRegistryIsNotAvailable() {
        // Given
        when(jmsTemplateProvider.getIfAvailable()).thenReturn(jmsTemplate);
        when(jmsListenerContainerFactoryProvider.getIfAvailable()).thenReturn(jmsListenerContainerFactory);
        when(jmsListenerEndpointRegistryProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}
