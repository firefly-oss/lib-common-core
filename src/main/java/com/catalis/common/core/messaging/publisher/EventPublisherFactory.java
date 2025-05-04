package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.annotation.PublisherType;
import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.resilience.ResilientEventPublisherFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating event publishers based on the publisher type.
 * <p>
 * This factory is responsible for providing the appropriate {@link EventPublisher}
 * implementation based on the requested {@link com.catalis.common.core.messaging.annotation.PublisherType}.
 * It maintains a registry of all available publishers and selects the appropriate one
 * based on the publisher type and availability.
 * <p>
 * The factory also enhances the publishers with resilience capabilities using the
 * {@link com.catalis.common.core.messaging.resilience.ResilientEventPublisherFactory}.
 * This adds features like circuit breaking, retries, and metrics to the publishers.
 * <p>
 * The factory is used by the {@link com.catalis.common.core.messaging.aspect.PublishResultAspect}
 * to obtain the appropriate publisher for a given {@link com.catalis.common.core.messaging.annotation.PublishResult}
 * annotation.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * EventPublisher publisher = eventPublisherFactory.getPublisher(PublisherType.KAFKA);
 * if (publisher != null) {
 *     publisher.publish("my-topic", "my-event", payload, transactionId);
 * }
 * }
 * </pre>
 *
 * @see EventPublisher
 * @see com.catalis.common.core.messaging.annotation.PublisherType
 * @see com.catalis.common.core.messaging.resilience.ResilientEventPublisherFactory
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
public class EventPublisherFactory {

    private final List<EventPublisher> publishers;
    private final ResilientEventPublisherFactory resilientFactory;
    private final MessagingProperties messagingProperties;

    // Map of publisher instances by type and connection ID
    private final Map<String, EventPublisher> publisherCache = new ConcurrentHashMap<>();
    private Map<Class<? extends EventPublisher>, EventPublisher> publisherMap;

    /**
     * Gets the appropriate event publisher for the specified publisher type and connection ID.
     * <p>
     * This method returns the appropriate {@link EventPublisher} implementation for the
     * specified {@link com.catalis.common.core.messaging.annotation.PublisherType} and connection ID.
     * It first checks if the publisher is available and properly configured. If not, it returns null.
     * <p>
     * The method also enhances the publisher with resilience capabilities using the
     * {@link com.catalis.common.core.messaging.resilience.ResilientEventPublisherFactory}.
     * This adds features like circuit breaking, retries, and metrics to the publisher.
     * <p>
     * The method follows these steps:
     * <ol>
     *   <li>Check if a publisher for the specified type and connection ID is already cached</li>
     *   <li>If not, get the base publisher for the specified type</li>
     *   <li>Check if the publisher is available and properly configured</li>
     *   <li>Enhance the publisher with resilience capabilities</li>
     *   <li>Cache and return the enhanced publisher</li>
     * </ol>
     * <p>
     * If any of these steps fail, the method returns null.
     *
     * @param publisherType the type of publisher to get
     * @param connectionId the connection ID to use, or null/empty for the default connection
     * @return the event publisher, or null if not available or properly configured
     * @see com.catalis.common.core.messaging.annotation.PublisherType
     * @see EventPublisher#isAvailable()
     */
    public EventPublisher getPublisher(PublisherType publisherType, String connectionId) {
        // Normalize connection ID
        String normalizedConnectionId = (connectionId == null || connectionId.isEmpty()) ?
                messagingProperties.getDefaultConnectionId() : connectionId;

        // Create a cache key using publisher type and connection ID
        String cacheKey = publisherType.name() + "_" + normalizedConnectionId;

        // Check if we already have a publisher for this type and connection ID
        return publisherCache.computeIfAbsent(cacheKey, key -> createPublisher(publisherType, normalizedConnectionId));
    }

    /**
     * Gets the appropriate event publisher for the specified publisher type using the default connection.
     * <p>
     * This is a convenience method that calls {@link #getPublisher(PublisherType, String)} with a null connection ID.
     *
     * @param publisherType the type of publisher to get
     * @return the event publisher, or null if not available or properly configured
     * @see #getPublisher(PublisherType, String)
     */
    public EventPublisher getPublisher(PublisherType publisherType) {
        return getPublisher(publisherType, null);
    }

    /**
     * Creates a new publisher for the specified type and connection ID.
     * <p>
     * This method is called by {@link #getPublisher(PublisherType, String)} when a publisher
     * for the specified type and connection ID is not found in the cache.
     *
     * @param publisherType the type of publisher to create
     * @param connectionId the connection ID to use
     * @return the created publisher, or null if not available or properly configured
     */
    private EventPublisher createPublisher(PublisherType publisherType, String connectionId) {
        if (publisherMap == null) {
            initPublisherMap();
        }

        // Get the base publisher for the specified type
        EventPublisher basePublisher = switch (publisherType) {
            case EVENT_BUS -> publisherMap.get(SpringEventPublisher.class);
            case KAFKA -> publisherMap.get(KafkaEventPublisher.class);
            case RABBITMQ -> publisherMap.get(RabbitMqEventPublisher.class);
            case SQS -> publisherMap.get(SqsEventPublisher.class);
            case GOOGLE_PUBSUB -> publisherMap.get(GooglePubSubEventPublisher.class);
            case AZURE_SERVICE_BUS -> publisherMap.get(AzureServiceBusEventPublisher.class);
            case REDIS -> publisherMap.get(RedisEventPublisher.class);
            case JMS -> publisherMap.get(JmsEventPublisher.class);
            case KINESIS -> publisherMap.get(KinesisEventPublisher.class);
        };

        if (basePublisher == null) {
            log.warn("Publisher of type {} is not available", publisherType);
            return null;
        }

        if (!basePublisher.isAvailable()) {
            log.warn("Publisher of type {} is not properly configured", publisherType);
            return null;
        }

        // Configure the publisher with the specified connection ID
        if (basePublisher instanceof ConnectionAwarePublisher connectionAwarePublisher) {
            connectionAwarePublisher.setConnectionId(connectionId);
        }

        // Wrap the publisher with resilience capabilities
        EventPublisher publisher = resilientFactory.createResilientPublisher(
                basePublisher,
                publisherType.name().toLowerCase() + "_" + connectionId
        );

        return publisher;
    }

    private void initPublisherMap() {
        publisherMap = publishers.stream()
                .collect(Collectors.toMap(
                        EventPublisher::getClass,
                        Function.identity()
                ));
    }
}
