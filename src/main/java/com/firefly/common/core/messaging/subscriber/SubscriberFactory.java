package com.firefly.common.core.messaging.subscriber;

import com.firefly.common.core.messaging.annotation.SubscriberType;
import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.resilience.ResilientEventSubscriberFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating event subscribers based on the subscriber type.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class SubscriberFactory {

    private final List<EventSubscriber> subscribers;
    private final ResilientEventSubscriberFactory resilientFactory;
    private final MessagingProperties messagingProperties;

    // Map of subscriber instances by type and connection ID
    private final Map<String, EventSubscriber> subscriberCache = new ConcurrentHashMap<>();
    private Map<Class<? extends EventSubscriber>, EventSubscriber> subscriberMap;

    /**
     * Gets the appropriate event subscriber for the specified subscriber type and connection ID.
     * <p>
     * This method returns the appropriate {@link EventSubscriber} implementation for the
     * specified {@link com.firefly.common.core.messaging.annotation.SubscriberType} and connection ID.
     * It first checks if the subscriber is available and properly configured. If not, it returns null.
     * <p>
     * The method also enhances the subscriber with resilience capabilities using the
     * {@link com.firefly.common.core.messaging.resilience.ResilientEventSubscriberFactory}.
     * <p>
     * The method follows these steps:
     * <ol>
     *   <li>Check if a subscriber for the specified type and connection ID is already cached</li>
     *   <li>If not, get the base subscriber for the specified type</li>
     *   <li>Check if the subscriber is available and properly configured</li>
     *   <li>Enhance the subscriber with resilience capabilities</li>
     *   <li>Cache and return the enhanced subscriber</li>
     * </ol>
     * <p>
     * If any of these steps fail, the method returns null.
     *
     * @param subscriberType the type of subscriber to get
     * @param connectionId the connection ID to use, or null/empty for the default connection
     * @return the event subscriber, or null if not available or properly configured
     */
    public EventSubscriber getSubscriber(SubscriberType subscriberType, String connectionId) {
        // Normalize connection ID
        String normalizedConnectionId = (connectionId == null || connectionId.isEmpty()) ?
                messagingProperties.getDefaultConnectionId() : connectionId;

        // Create a cache key using subscriber type and connection ID
        String cacheKey = subscriberType.name() + "_" + normalizedConnectionId;

        // Check if we already have a subscriber for this type and connection ID
        return subscriberCache.computeIfAbsent(cacheKey, key -> createSubscriber(subscriberType, normalizedConnectionId));
    }

    /**
     * Gets the appropriate event subscriber for the specified subscriber type using the default connection.
     * <p>
     * This is a convenience method that calls {@link #getSubscriber(SubscriberType, String)} with a null connection ID.
     *
     * @param subscriberType the type of subscriber to get
     * @return the event subscriber, or null if not available or properly configured
     * @see #getSubscriber(SubscriberType, String)
     */
    public EventSubscriber getSubscriber(SubscriberType subscriberType) {
        return getSubscriber(subscriberType, null);
    }

    /**
     * Creates a new subscriber for the specified type and connection ID.
     * <p>
     * This method is called by {@link #getSubscriber(SubscriberType, String)} when a subscriber
     * for the specified type and connection ID is not found in the cache.
     *
     * @param subscriberType the type of subscriber to create
     * @param connectionId the connection ID to use
     * @return the created subscriber, or null if not available or properly configured
     */
    private EventSubscriber createSubscriber(SubscriberType subscriberType, String connectionId) {
        if (subscriberMap == null) {
            initSubscriberMap();
        }

        // Get the base subscriber for the specified type
        EventSubscriber baseSubscriber = switch (subscriberType) {
            case EVENT_BUS -> subscriberMap.get(SpringEventSubscriber.class);
            case KAFKA -> subscriberMap.get(KafkaEventSubscriber.class);
            case RABBITMQ -> subscriberMap.get(RabbitMqEventSubscriber.class);
            case SQS -> subscriberMap.get(SqsEventSubscriber.class);
            case GOOGLE_PUBSUB -> subscriberMap.get(GooglePubSubEventSubscriber.class);
            case AZURE_SERVICE_BUS -> subscriberMap.get(AzureServiceBusEventSubscriber.class);
            case REDIS -> subscriberMap.get(RedisEventSubscriber.class);
            case JMS -> subscriberMap.get(JmsEventSubscriber.class);
            case KINESIS -> subscriberMap.get(KinesisEventSubscriber.class);
        };

        if (baseSubscriber == null) {
            log.warn("Subscriber of type {} is not available", subscriberType);
            return null;
        }

        if (!baseSubscriber.isAvailable()) {
            log.warn("Subscriber of type {} is not properly configured", subscriberType);
            return null;
        }

        // Configure the subscriber with the specified connection ID
        if (baseSubscriber instanceof ConnectionAwareSubscriber connectionAwareSubscriber) {
            connectionAwareSubscriber.setConnectionId(connectionId);
        }

        // Wrap the subscriber with resilience capabilities
        return resilientFactory.createResilientSubscriber(
                baseSubscriber,
                subscriberType.name().toLowerCase() + "_" + connectionId
        );
    }

    private void initSubscriberMap() {
        subscriberMap = subscribers.stream()
                .collect(Collectors.toMap(
                        EventSubscriber::getClass,
                        Function.identity()
                ));
    }
}
