package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GooglePubSubEventSubscriberTest {

    @Mock
    private ObjectProvider<PubSubTemplate> pubSubTemplateProvider;

    @Mock
    private ObjectProvider<PubSubSubscriberTemplate> pubSubSubscriberTemplateProvider;

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private PubSubSubscriberTemplate pubSubSubscriberTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private EventHandler eventHandler;

    @Mock
    private BasicAcknowledgeablePubsubMessage acknowledgeablePubsubMessage;

    @Mock
    private PubsubMessage pubsubMessage;

    private GooglePubSubEventSubscriber subscriber;

    private final String source = "test-topic";
    private final String eventType = "test.event";

    @BeforeEach
    void setUp() {
        lenient().when(pubSubTemplateProvider.getIfAvailable()).thenReturn(pubSubTemplate);
        lenient().when(pubSubSubscriberTemplateProvider.getIfAvailable()).thenReturn(pubSubSubscriberTemplate);
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());

        // Mock the BasicAcknowledgeablePubsubMessage
        lenient().when(acknowledgeablePubsubMessage.getPubsubMessage()).thenReturn(pubsubMessage);
        lenient().when(pubsubMessage.getAttributesMap()).thenReturn(Map.of("eventType", eventType));
        lenient().when(pubsubMessage.getMessageId()).thenReturn("test-message-id");

        subscriber = new TestGooglePubSubEventSubscriber(
                pubSubTemplateProvider,
                pubSubSubscriberTemplateProvider,
                messagingProperties
        );
    }

    /**
     * Test-specific implementation of GooglePubSubEventSubscriber that allows access to protected methods
     * and fields for testing purposes.
     */
    private class TestGooglePubSubEventSubscriber extends GooglePubSubEventSubscriber {
        public TestGooglePubSubEventSubscriber(
                ObjectProvider<PubSubTemplate> pubSubTemplateProvider,
                ObjectProvider<PubSubSubscriberTemplate> pubSubSubscriberTemplateProvider,
                MessagingProperties messagingProperties) {
            super(pubSubTemplateProvider, pubSubSubscriberTemplateProvider, messagingProperties);
        }

        // Expose the subscriptions map for testing
        public Map<String, AtomicBoolean> getSubscriptions() {
            try {
                java.lang.reflect.Field subscriptionsField = GooglePubSubEventSubscriber.class.getDeclaredField("subscriptions");
                subscriptionsField.setAccessible(true);
                return (Map<String, AtomicBoolean>) subscriptionsField.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access subscriptions field", e);
            }
        }

        // Set the subscriptions map for testing
        public void setSubscriptions(Map<String, AtomicBoolean> subscriptions) {
            try {
                java.lang.reflect.Field subscriptionsField = GooglePubSubEventSubscriber.class.getDeclaredField("subscriptions");
                subscriptionsField.setAccessible(true);
                subscriptionsField.set(this, subscriptions);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set subscriptions field", e);
            }
        }
    }

    @Test
    void shouldSubscribeToPubSubTopic() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Capture the subscription consumer
        ArgumentCaptor<Consumer<BasicAcknowledgeablePubsubMessage>> consumerCaptor = 
            ArgumentCaptor.forClass(Consumer.class);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(pubSubTemplate).subscribe(eq(groupId), consumerCaptor.capture());

        // Verify the subscription was registered
        TestGooglePubSubEventSubscriber testSubscriber = (TestGooglePubSubEventSubscriber) subscriber;
        assertTrue(testSubscriber.getSubscriptions().containsKey(source + ":" + eventType));
        assertTrue(testSubscriber.getSubscriptions().get(source + ":" + eventType).get());
    }

    @Test
    void shouldUseDefaultSubscriptionNameWhenGroupIdIsEmpty() {
        // Given
        String groupId = "";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(pubSubTemplate).subscribe(eq("subscription-" + source), any());
    }

    @Test
    void shouldNotSubscribeWhenPubSubTemplateIsNotAvailable() {
        // Given
        when(pubSubTemplateProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(pubSubTemplate, never()).subscribe(anyString(), any());
    }

    @Test
    void shouldNotSubscribeWhenAlreadySubscribed() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Set up a pre-existing subscription
        TestGooglePubSubEventSubscriber testSubscriber = (TestGooglePubSubEventSubscriber) subscriber;
        Map<String, AtomicBoolean> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(source + ":" + eventType, new AtomicBoolean(true));
        testSubscriber.setSubscriptions(subscriptions);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        verify(pubSubTemplate, never()).subscribe(anyString(), any());
    }

    @Test
    void shouldUnsubscribe() {
        // Given
        TestGooglePubSubEventSubscriber testSubscriber = (TestGooglePubSubEventSubscriber) subscriber;
        Map<String, AtomicBoolean> subscriptions = new ConcurrentHashMap<>();
        AtomicBoolean active = new AtomicBoolean(true);
        subscriptions.put(source + ":" + eventType, active);
        testSubscriber.setSubscriptions(subscriptions);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        // Verify the subscription was removed
        assertFalse(testSubscriber.getSubscriptions().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotUnsubscribeWhenNotSubscribed() {
        // Given
        TestGooglePubSubEventSubscriber testSubscriber = (TestGooglePubSubEventSubscriber) subscriber;
        Map<String, AtomicBoolean> subscriptions = new ConcurrentHashMap<>();
        testSubscriber.setSubscriptions(subscriptions);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        // No exception should be thrown
        assertTrue(testSubscriber.getSubscriptions().isEmpty());
    }

    @Test
    void shouldBeAvailableWhenPubSubTemplateIsAvailable() {
        // Given
        // No need to stub pubSubTemplateProvider.getIfAvailable() as it's already set up in setUp()
        lenient().when(pubSubSubscriberTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldBeAvailableWhenPubSubSubscriberTemplateIsAvailable() {
        // Given
        when(pubSubTemplateProvider.getIfAvailable()).thenReturn(null);
        when(pubSubSubscriberTemplateProvider.getIfAvailable()).thenReturn(pubSubSubscriberTemplate);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenNeitherTemplateIsAvailable() {
        // Given
        when(pubSubTemplateProvider.getIfAvailable()).thenReturn(null);
        when(pubSubSubscriberTemplateProvider.getIfAvailable()).thenReturn(null);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}
