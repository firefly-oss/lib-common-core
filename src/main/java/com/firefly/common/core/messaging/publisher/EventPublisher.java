package com.firefly.common.core.messaging.publisher;

import reactor.core.publisher.Mono;

import com.firefly.common.core.messaging.serialization.MessageSerializer;

/**
 * Interface for publishing events to different messaging systems.
 * <p>
 * This interface defines the contract for publishing events to various messaging systems.
 * Implementations of this interface handle the specifics of publishing to different
 * messaging platforms such as Kafka, RabbitMQ, Amazon SQS, etc.
 * <p>
 * The interface is designed to be reactive, returning a {@link Mono<Void>} that completes
 * when the publishing operation is done. This allows for non-blocking integration with
 * reactive applications.
 * <p>
 * Implementations should handle the following responsibilities:
 * <ul>
 *   <li>Converting the payload to the appropriate format for the messaging system</li>
 *   <li>Adding metadata such as event type and transaction ID to the message</li>
 *   <li>Handling errors and providing appropriate logging</li>
 *   <li>Checking availability of the underlying messaging system</li>
 * </ul>
 * <p>
 * The {@link #isAvailable()} method allows the system to check if the publisher is
 * properly configured and available for use. This is used by the {@link com.firefly.common.core.messaging.publisher.EventPublisherFactory}
 * to determine which publisher to use for a given {@link com.firefly.common.core.messaging.annotation.PublisherType}.
 *
 * @see com.firefly.common.core.messaging.annotation.PublishResult
 * @see com.firefly.common.core.messaging.annotation.PublisherType
 * @see com.firefly.common.core.messaging.publisher.EventPublisherFactory
 */
public interface EventPublisher {

    /**
     * Publishes an event to the specified destination.
     * <p>
     * This method is responsible for publishing the given payload to the specified destination
     * with the provided event type and transaction ID. The implementation should handle the
     * specifics of the messaging system, including error handling and retries if necessary.
     * <p>
     * The method returns a {@link Mono<Void>} that completes when the publishing operation
     * is done. If the operation fails, the Mono will complete with an error. This allows
     * for reactive error handling and integration with reactive applications.
     * <p>
     * The transaction ID is optional and can be used for tracing purposes. If provided,
     * it should be included in the message metadata to allow for distributed tracing.
     *
     * @param destination the destination to publish to (topic, queue, etc.)
     * @param eventType the type of event, used for routing or filtering
     * @param payload the event payload, which can be any object that can be serialized
     * @param transactionId the transaction ID for tracing (can be null)
     * @return a Mono that completes when the event is published successfully
     */
    Mono<Void> publish(String destination, String eventType, Object payload, String transactionId);

    /**
     * Publishes an event to the specified destination using the provided serializer.
     * <p>
     * This method is similar to {@link #publish(String, String, Object, String)}, but it
     * allows specifying a serializer to use for converting the payload to the appropriate
     * format for the messaging system.
     *
     * @param destination the destination to publish to (topic, queue, etc.)
     * @param eventType the type of event, used for routing or filtering
     * @param payload the event payload, which can be any object that can be serialized
     * @param transactionId the transaction ID for tracing (can be null)
     * @param serializer the serializer to use for converting the payload
     * @return a Mono that completes when the event is published successfully
     */
    default Mono<Void> publish(String destination, String eventType, Object payload, String transactionId, MessageSerializer serializer) {
        // Default implementation just calls the regular publish method
        return publish(destination, eventType, payload, transactionId);
    }

    /**
     * Checks if this publisher is available and properly configured.
     * <p>
     * This method is used to determine if the publisher is properly configured and
     * available for use. It should check if all required dependencies and configurations
     * are present and valid.
     * <p>
     * For example, a Kafka publisher would check if the Kafka template is available
     * and properly configured. A RabbitMQ publisher would check if the RabbitMQ template
     * is available and properly configured.
     * <p>
     * This method is used by the {@link EventPublisherFactory} to determine which
     * publisher to use for a given {@link com.firefly.common.core.messaging.annotation.PublisherType}.
     * If this method returns false, the factory will not use this publisher.
     *
     * @return true if the publisher is available and properly configured, false otherwise
     */
    boolean isAvailable();
}
