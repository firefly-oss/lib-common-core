package com.catalis.common.core.messaging.annotation;

import com.catalis.common.core.messaging.serialization.SerializationFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to publish the result of a method to a message queue or event bus.
 * <p>
 * This annotation can be applied to methods to automatically publish their return value
 * to a configured destination. When a method annotated with {@code @PublishResult} is executed,
 * its return value is intercepted and published to the specified messaging system.
 * <p>
 * The annotation supports multiple messaging systems including:
 * <ul>
 *   <li>Spring Event Bus</li>
 *   <li>Apache Kafka</li>
 *   <li>RabbitMQ</li>
 *   <li>Amazon SQS</li>
 *   <li>Google Cloud Pub/Sub</li>
 *   <li>Azure Service Bus</li>
 *   <li>Redis Pub/Sub</li>
 *   <li>ActiveMQ/JMS</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> By default, the messaging functionality is disabled. To enable it,
 * you need to set {@code messaging.enabled=true} in your application configuration. Additionally,
 * for each specific messaging system, you need to enable it with the corresponding property
 * (e.g., {@code messaging.kafka.enabled=true} for Kafka).
 * <p>
 * The annotation supports both synchronous and asynchronous methods, including those that return
 * {@code Mono}, {@code Flux}, or {@code CompletableFuture}. For reactive return types, the
 * publishing operation is integrated into the reactive chain.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @PublishResult(
 *     destination = "user-events",
 *     eventType = "user.created",
 *     publisher = PublisherType.KAFKA
 * )
 * public User createUser(UserRequest request) {
 *     // Method implementation
 *     return user;
 * }
 * }
 * </pre>
 *
 * @see PublisherType
 * @see com.catalis.common.core.messaging.aspect.PublishResultAspect
 * @see com.catalis.common.core.messaging.publisher.EventPublisher
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishResult {

    /**
     * The destination to publish the result to.
     * <p>
     * For Kafka: the topic name
     * For RabbitMQ: the exchange name or queue name
     * For Spring Event Bus: optional, can be used for filtering
     * <p>
     * If not specified, a default destination will be used based on the configuration:
     * - For Kafka: messaging.kafka.default-topic
     * - For RabbitMQ: messaging.rabbitmq.default-exchange
     * - For Spring Event Bus: empty string
     *
     * @return the destination name
     */
    String destination() default "";

    /**
     * The type of event to publish.
     * <p>
     * This value will be included in the event metadata and can be used for routing
     * or filtering events.
     * <p>
     * For RabbitMQ, this value is also used as the routing key if specified. If not specified,
     * the default routing key from the configuration (messaging.rabbitmq.default-routing-key) will be used.
     *
     * @return the event type
     */
    String eventType() default "";

    /**
     * The type of publisher to use.
     *
     * @return the publisher type
     */
    PublisherType publisher() default PublisherType.EVENT_BUS;

    /**
     * Custom payload expression.
     * <p>
     * If specified, this expression will be evaluated to determine the payload
     * instead of using the method's return value directly.
     * <p>
     * The expression can reference the method's return value using the variable 'result'.
     * <p>
     * Example: "{'id': result.id, 'name': result.name}"
     *
     * @return the payload expression
     */
    String payloadExpression() default "";

    /**
     * Whether to include the transaction ID in the event metadata.
     *
     * @return true if the transaction ID should be included
     */
    boolean includeTransactionId() default true;

    /**
     * Whether to publish the result asynchronously.
     *
     * @return true if the result should be published asynchronously
     */
    boolean async() default true;

    /**
     * The serialization format to use for the payload.
     * <p>
     * If not specified, the default format from the configuration will be used.
     *
     * @return the serialization format
     */
    SerializationFormat serializationFormat() default SerializationFormat.JSON;
}
