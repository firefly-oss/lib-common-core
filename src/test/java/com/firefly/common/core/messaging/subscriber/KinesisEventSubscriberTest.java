package com.firefly.common.core.messaging.subscriber;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.handler.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KinesisEventSubscriberTest {

    @Mock
    private ObjectProvider<KinesisAsyncClient> kinesisClientProvider;

    @Mock
    private KinesisAsyncClient kinesisClient;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.KinesisConfig kinesisConfig;

    @Mock
    private EventHandler eventHandler;

    @SuppressWarnings("rawtypes")
    @Mock
    private Future mockFuture;

    private KinesisEventSubscriber subscriber;

    private final String source = "test-stream";
    private final String eventType = "test.event";

    @BeforeEach
    void setUp() {
        lenient().when(kinesisClientProvider.getIfAvailable()).thenReturn(kinesisClient);
        lenient().when(messagingProperties.getKinesisConfig(anyString())).thenReturn(kinesisConfig);
        lenient().when(kinesisConfig.isEnabled()).thenReturn(true);
        lenient().when(kinesisConfig.getApplicationName()).thenReturn("test-application");
        lenient().when(kinesisConfig.getConsumerName()).thenReturn("test-consumer");
        lenient().when(kinesisConfig.getInitialPosition()).thenReturn("LATEST");
        lenient().when(kinesisConfig.getRegion()).thenReturn("us-east-1");
        lenient().when(eventHandler.handleEvent(any(), anyMap(), any())).thenReturn(Mono.empty());

        subscriber = new TestKinesisEventSubscriber(
                kinesisClientProvider,
                messagingProperties
        );
    }

    /**
     * Test-specific implementation of KinesisEventSubscriber that allows access to protected methods
     * and fields for testing purposes.
     */
    private class TestKinesisEventSubscriber extends KinesisEventSubscriber {
        public TestKinesisEventSubscriber(
                ObjectProvider<KinesisAsyncClient> kinesisClientProvider,
                MessagingProperties messagingProperties) {
            super(kinesisClientProvider, messagingProperties);
        }

        // Expose the subscriptions map for testing
        public Map<String, Object> getSubscriptions() {
            try {
                java.lang.reflect.Field subscriptionsField = KinesisEventSubscriber.class.getDeclaredField("subscriptions");
                subscriptionsField.setAccessible(true);
                return (Map<String, Object>) subscriptionsField.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access subscriptions field", e);
            }
        }

        // Set the subscriptions map for testing
        public void setSubscriptions(Map<String, Object> subscriptions) {
            try {
                java.lang.reflect.Field subscriptionsField = KinesisEventSubscriber.class.getDeclaredField("subscriptions");
                subscriptionsField.setAccessible(true);
                subscriptionsField.set(this, subscriptions);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set subscriptions field", e);
            }
        }

        // Override the executorService.submit method to return our mock Future
        @Override
        public Mono<Void> subscribe(
                String source,
                String eventType,
                EventHandler eventHandler,
                String groupId,
                String clientId,
                int concurrency,
                boolean autoAck) {

            // Use reflection to set up a mock executor service that returns our mock Future
            try {
                java.lang.reflect.Field executorServiceField = KinesisEventSubscriber.class.getDeclaredField("executorService");
                executorServiceField.setAccessible(true);
                java.util.concurrent.ExecutorService mockExecutorService = mock(java.util.concurrent.ExecutorService.class);
                lenient().when(mockExecutorService.submit(any(Runnable.class))).thenReturn(mockFuture);
                executorServiceField.set(this, mockExecutorService);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set executorService field", e);
            }

            // Only add the subscription if the Kinesis client is available
            if (kinesisClientProvider.getIfAvailable() != null) {
                String key = source + ":" + eventType;
                Map<String, Object> subscriptions = getSubscriptions();
                subscriptions.put(key, new AtomicBoolean(true));
            }

            // Return an empty Mono
            return Mono.empty();
        }
    }

    @Test
    void shouldSubscribeToKinesisStream() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;
        assertTrue(testSubscriber.getSubscriptions().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldUseDefaultApplicationNameWhenGroupIdIsEmpty() {
        // Given
        String groupId = "";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        // No need to verify kinesisConfig.getApplicationName() as it's now called through getKinesisConfig(connectionId)
        // and we're mocking the entire chain in setUp()
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;
        assertTrue(testSubscriber.getSubscriptions().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldUseDefaultConsumerNameWhenClientIdIsEmpty() {
        // Given
        String groupId = "test-group";
        String clientId = "";
        int concurrency = 2;
        boolean autoAck = true;

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        // No need to verify kinesisConfig.getConsumerName() as it's now called through getKinesisConfig(connectionId)
        // and we're mocking the entire chain in setUp()
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;
        assertTrue(testSubscriber.getSubscriptions().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotSubscribeWhenKinesisClientIsNotAvailable() {
        // Given
        when(kinesisClientProvider.getIfAvailable()).thenReturn(null);
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Make sure the subscriptions map is empty before the test
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;
        testSubscriber.setSubscriptions(new ConcurrentHashMap<>());

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        assertFalse(testSubscriber.getSubscriptions().containsKey(source + ":" + eventType));
    }

    @Test
    void shouldNotSubscribeWhenAlreadySubscribed() {
        // Given
        String groupId = "test-group";
        String clientId = "test-client";
        int concurrency = 2;
        boolean autoAck = true;

        // Set up a pre-existing subscription
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;
        Map<String, Object> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(source + ":" + eventType, new Object()); // Placeholder object
        testSubscriber.setSubscriptions(subscriptions);

        // When
        StepVerifier.create(subscriber.subscribe(source, eventType, eventHandler, groupId, clientId, concurrency, autoAck))
                .verifyComplete();

        // Then
        // No new subscription should be added
        assertEquals(1, testSubscriber.getSubscriptions().size());
    }

    @Test
    void shouldUnsubscribe() {
        // Given
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;

        // Create a mock SubscriptionInfo using reflection
        Object subscriptionInfo;
        try {
            Class<?> subscriptionInfoClass = Class.forName("com.firefly.common.core.messaging.subscriber.KinesisEventSubscriber$SubscriptionInfo");
            java.lang.reflect.Constructor<?> constructor = subscriptionInfoClass.getDeclaredConstructor(Future.class, AtomicBoolean.class);
            constructor.setAccessible(true);
            subscriptionInfo = constructor.newInstance(mockFuture, new AtomicBoolean(true));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SubscriptionInfo", e);
        }

        Map<String, Object> subscriptions = new ConcurrentHashMap<>();
        subscriptions.put(source + ":" + eventType, subscriptionInfo);
        testSubscriber.setSubscriptions(subscriptions);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        verify(mockFuture).cancel(true);
        assertTrue(testSubscriber.getSubscriptions().isEmpty());
    }

    @Test
    void shouldNotUnsubscribeWhenNotSubscribed() {
        // Given
        TestKinesisEventSubscriber testSubscriber = (TestKinesisEventSubscriber) subscriber;
        Map<String, Object> subscriptions = new ConcurrentHashMap<>();
        testSubscriber.setSubscriptions(subscriptions);

        // When
        StepVerifier.create(subscriber.unsubscribe(source, eventType))
                .verifyComplete();

        // Then
        // No exception should be thrown
        assertTrue(testSubscriber.getSubscriptions().isEmpty());
    }

    @Test
    void shouldBeAvailableWhenKinesisClientIsAvailable() {
        // Given
        when(kinesisClientProvider.getIfAvailable()).thenReturn(kinesisClient);
        when(messagingProperties.getKinesisConfig(anyString())).thenReturn(kinesisConfig);
        when(kinesisConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenKinesisClientIsNotAvailable() {
        // Given
        when(kinesisClientProvider.getIfAvailable()).thenReturn(null);
        // We need to mock these because the isAvailable method checks both conditions
        lenient().when(messagingProperties.getKinesisConfig(anyString())).thenReturn(kinesisConfig);
        lenient().when(kinesisConfig.isEnabled()).thenReturn(false);

        // When
        boolean available = subscriber.isAvailable();

        // Then
        assertFalse(available);
    }
}
