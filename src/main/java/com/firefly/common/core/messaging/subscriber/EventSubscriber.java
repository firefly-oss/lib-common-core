package com.firefly.common.core.messaging.subscriber;

import com.firefly.common.core.messaging.handler.EventHandler;
import reactor.core.publisher.Mono;

/**
 * Interface for subscribing to events from different messaging systems.
 * <p>
 * This interface defines the contract for subscribing to events from various messaging systems.
 * Implementations of this interface handle the specifics of subscribing to different
 * messaging platforms such as Kafka, RabbitMQ, Amazon SQS, etc.
 */
public interface EventSubscriber {

    /**
     * Subscribes to events from the specified source.
     *
     * @param source the source to subscribe to (topic, queue, etc.)
     * @param eventType the type of event to filter on
     * @param eventHandler the handler to call when an event is received
     * @param groupId the group ID for the subscriber (optional)
     * @param clientId the client ID for the subscriber (optional)
     * @param concurrency the concurrency level for processing events
     * @param autoAck whether to acknowledge events automatically
     * @return a Mono that completes when the subscription is established
     */
    Mono<Void> subscribe(
            String source,
            String eventType,
            EventHandler eventHandler,
            String groupId,
            String clientId,
            int concurrency,
            boolean autoAck
    );

    /**
     * Unsubscribes from the specified source.
     *
     * @param source the source to unsubscribe from
     * @param eventType the type of event to unsubscribe from
     * @return a Mono that completes when the unsubscription is complete
     */
    Mono<Void> unsubscribe(String source, String eventType);

    /**
     * Checks if this subscriber is available and properly configured.
     *
     * @return true if the subscriber is available
     */
    boolean isAvailable();
}
