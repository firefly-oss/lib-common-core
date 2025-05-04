package com.catalis.common.core.messaging.annotation;

/**
 * Enumeration of supported event subscriber types.
 * <p>
 * This enumeration defines the different messaging systems that can be used with the
 * {@link EventListener} annotation. Each subscriber type corresponds to a specific
 * implementation of the event subscriber.
 * <p>
 * The availability of each subscriber type depends on the presence of the corresponding
 * dependencies in the classpath and the configuration in the application properties.
 * <p>
 * For example, to use the {@link #KAFKA} subscriber type, you need to include the
 * Spring Kafka dependency and enable it in the application properties with
 * {@code messaging.kafka.enabled=true}.
 */
public enum SubscriberType {
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
