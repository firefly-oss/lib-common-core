package com.catalis.common.core.messaging.annotation;

/**
 * Enumeration of supported event publisher types.
 * <p>
 * This enumeration defines the different messaging systems that can be used with the
 * {@link PublishResult} annotation. Each publisher type corresponds to a specific
 * implementation of the {@link com.catalis.common.core.messaging.publisher.EventPublisher}
 * interface.
 * <p>
 * The availability of each publisher type depends on two conditions:
 * <ol>
 *   <li>The presence of the corresponding dependencies in the classpath</li>
 *   <li>The configuration in the application properties</li>
 * </ol>
 * <p>
 * For a publisher to be available, both the overall messaging system must be enabled with
 * {@code messaging.enabled=true} AND the specific messaging system must be enabled with its
 * own property (e.g., {@code messaging.kafka.enabled=true}).
 * <p>
 * The Spring Event Bus ({@link #EVENT_BUS}) is a special case - it will be available whenever
 * {@code messaging.enabled=true} since it doesn't require external configuration.
 * <p>
 * For example, to use the {@link #KAFKA} publisher type, you need to:
 * <ol>
 *   <li>Include the Spring Kafka dependency in your project</li>
 *   <li>Enable the overall messaging system with {@code messaging.enabled=true}</li>
 *   <li>Enable Kafka specifically with {@code messaging.kafka.enabled=true}</li>
 * </ol>
 */
public enum PublisherType {
    /**
     * Spring Application Event Bus
     */
    EVENT_BUS,

    /**
     * Apache Kafka
     */
    KAFKA,

    /**
     * RabbitMQ
     */
    RABBITMQ,

    /**
     * Amazon Simple Queue Service (SQS)
     */
    SQS,

    /**
     * Google Cloud Pub/Sub
     */
    GOOGLE_PUBSUB,

    /**
     * Azure Service Bus
     */
    AZURE_SERVICE_BUS,

    /**
     * Redis Pub/Sub
     */
    REDIS,

    /**
     * ActiveMQ/JMS
     */
    JMS,

    /**
     * AWS Kinesis
     */
    KINESIS
}
