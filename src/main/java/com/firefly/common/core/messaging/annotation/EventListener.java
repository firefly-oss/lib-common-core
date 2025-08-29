package com.firefly.common.core.messaging.annotation;

import com.firefly.common.core.messaging.serialization.SerializationFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as an event listener.
 * <p>
 * This annotation can be applied to methods to automatically listen for events from
 * a configured source. When an event is received, the method will be called with the
 * event payload as an argument.
 * <p>
 * The method can have additional parameters, which will be resolved from the Spring
 * application context or from the event metadata.
 * <p>
 * <strong>Note:</strong> By default, the messaging functionality is disabled. To enable it,
 * you need to set {@code messaging.enabled=true} in your application configuration. Additionally,
 * for each specific messaging system, you need to enable it with the corresponding property
 * (e.g., {@code messaging.kafka.enabled=true} for Kafka).
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @EventListener(
 *     source = "user-events",
 *     eventType = "user.created",
 *     subscriber = SubscriberType.KAFKA
 * )
 * public void handleUserCreated(User user) {
 *     // Method implementation
 * }
 * }
 * </pre>
 *
 * @see PublishResult
 * @see SubscriberType
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {

    /**
     * The source to listen to events from.
     * <p>
     * For Kafka: the topic name
     * For RabbitMQ: the queue name or exchange name
     * For Spring Event Bus: optional, can be used for filtering
     * <p>
     * If not specified, a default source will be used based on the configuration:
     * - For Kafka: messaging.kafka.default-topic
     * - For RabbitMQ: messaging.rabbitmq.default-queue
     * - For Spring Event Bus: empty string
     *
     * @return the source name
     */
    String source() default "";

    /**
     * The type of event to listen for.
     * <p>
     * This value will be used to filter events. Only events with this type will be
     * passed to the method.
     * <p>
     * For RabbitMQ, this value is also used as the routing key if specified.
     *
     * @return the event type
     */
    String eventType() default "";

    /**
     * The type of subscriber to use.
     *
     * @return the subscriber type
     */
    SubscriberType subscriber() default SubscriberType.EVENT_BUS;

    /**
     * The serialization format to use for the payload.
     * <p>
     * If not specified, the default format from the configuration will be used.
     *
     * @return the serialization format
     */
    SerializationFormat serializationFormat() default SerializationFormat.JSON;

    /**
     * The concurrency level for processing events.
     * <p>
     * This value determines how many threads will be used to process events.
     * A value of 1 means single-threaded processing.
     * <p>
     * Note: This value is only used for Kafka and RabbitMQ subscribers.
     * Spring Event Bus always uses the calling thread.
     *
     * @return the concurrency level
     */
    int concurrency() default 1;

    /**
     * Whether to acknowledge events automatically after processing.
     * <p>
     * If set to true, events will be acknowledged automatically after the method
     * completes successfully. If set to false, the method must explicitly acknowledge
     * the event.
     * <p>
     * Note: This value is only used for Kafka and RabbitMQ subscribers.
     * Spring Event Bus does not support acknowledgement.
     *
     * @return true if events should be acknowledged automatically
     */
    boolean autoAck() default true;

    /**
     * The group ID for the subscriber.
     * <p>
     * For Kafka: the consumer group ID
     * For RabbitMQ: ignored
     * For Spring Event Bus: ignored
     * <p>
     * If not specified, a default group ID will be used based on the configuration.
     *
     * @return the group ID
     */
    String groupId() default "";

    /**
     * The client ID for the subscriber.
     * <p>
     * For Kafka: the consumer client ID
     * For RabbitMQ: ignored
     * For Spring Event Bus: ignored
     * <p>
     * If not specified, a default client ID will be used based on the configuration.
     *
     * @return the client ID
     */
    String clientId() default "";

    /**
     * The routing key to use for RabbitMQ.
     * <p>
     * This value is only used for RabbitMQ subscribers. If not specified, the eventType
     * will be used as the routing key.
     *
     * @return the routing key
     */
    String routingKey() default "";

    /**
     * The name of a bean that implements {@link com.firefly.common.core.messaging.error.EventErrorHandler}
     * to handle errors that occur during event processing.
     * <p>
     * If not specified, the default error handler will be used, which logs the error
     * and acknowledges the message (if autoAck is false).
     *
     * @return the bean name of the error handler
     */
    String errorHandler() default "";

    /**
     * The connection ID to use for the subscriber.
     * <p>
     * This value is used to select a specific connection configuration for the subscriber.
     * If not specified, the default connection will be used.
     * <p>
     * This is useful when you have multiple connections configured for the same subscriber type,
     * for example, to listen to different Kafka clusters or RabbitMQ servers.
     * <p>
     * Example: "prod-cluster" or "eu-west-1"
     *
     * @return the connection ID
     */
    String connectionId() default "";
}
