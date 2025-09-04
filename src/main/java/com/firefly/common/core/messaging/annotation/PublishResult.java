/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.core.messaging.annotation;

import com.firefly.common.core.messaging.serialization.SerializationFormat;

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
 * @see com.firefly.common.core.messaging.aspect.PublishResultAspect
 * @see com.firefly.common.core.messaging.publisher.EventPublisher
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

    /**
     * The routing key to use for RabbitMQ.
     * <p>
     * This value is only used for RabbitMQ publishers. If not specified, the eventType
     * will be used as the routing key.
     *
     * @return the routing key
     */
    String routingKey() default "";

    /**
     * SpEL condition to determine whether to publish the result.
     * <p>
     * If specified, this expression will be evaluated to determine whether the result
     * should be published. If the expression evaluates to false, the result will not be published.
     * <p>
     * The expression can reference the method's return value using the variable 'result'
     * and method arguments using 'args[index]'.
     * <p>
     * Example: "result != null" or "args[0].isValid()"
     *
     * @return the condition expression
     */
    String condition() default "";

    /**
     * Whether to include standard headers in the published message.
     * <p>
     * Standard headers include:
     * <ul>
     *   <li>X-Transaction-Id: The transaction ID from the current context</li>
     *   <li>X-Event-Type: The event type specified in the annotation</li>
     *   <li>X-Source-Service: The name of the service publishing the event</li>
     *   <li>X-Timestamp: The timestamp when the event was published</li>
     * </ul>
     *
     * @return true if standard headers should be included
     */
    boolean includeHeaders() default true;

    /**
     * Custom headers to include in the published message.
     * <p>
     * Each header is defined using a {@link HeaderExpression} annotation, which
     * specifies the header name and a SpEL expression to evaluate for the header value.
     *
     * @return array of header expressions
     */
    HeaderExpression[] headerExpressions() default {};

    /**
     * The name of a bean that implements {@link com.firefly.common.core.messaging.error.PublishErrorHandler}
     * to handle errors that occur during publishing.
     * <p>
     * If not specified, the default error handler will be used, which logs the error
     * and continues execution.
     *
     * @return the bean name of the error handler
     */
    String errorHandler() default "";

    /**
     * The connection ID to use for the publisher.
     * <p>
     * This value is used to select a specific connection configuration for the publisher.
     * If not specified, the default connection will be used.
     * <p>
     * This is useful when you have multiple connections configured for the same publisher type,
     * for example, to publish to different Kafka clusters or RabbitMQ servers.
     * <p>
     * Example: "prod-cluster" or "eu-west-1"
     *
     * @return the connection ID
     */
    String connectionId() default "";
}
