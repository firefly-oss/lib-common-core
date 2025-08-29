package com.firefly.common.core.messaging.publisher;

/**
 * Interface for publishers that can be configured with a specific connection ID.
 * <p>
 * Publishers that implement this interface can be configured to use a specific connection
 * configuration based on the connection ID. This allows for multiple connections to the
 * same messaging system type, for example, to publish to different Kafka clusters or
 * RabbitMQ servers.
 * <p>
 * The connection ID is used to look up the appropriate configuration in the
 * {@link com.firefly.common.core.messaging.config.MessagingProperties}.
 */
public interface ConnectionAwarePublisher {

    /**
     * Sets the connection ID for this publisher.
     * <p>
     * This method is called by the {@link EventPublisherFactory} when creating a publisher
     * for a specific connection ID.
     *
     * @param connectionId the connection ID to use
     */
    void setConnectionId(String connectionId);

    /**
     * Gets the connection ID for this publisher.
     *
     * @return the connection ID
     */
    String getConnectionId();
}
