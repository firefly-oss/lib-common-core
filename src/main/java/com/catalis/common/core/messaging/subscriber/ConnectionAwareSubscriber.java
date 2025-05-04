package com.catalis.common.core.messaging.subscriber;

/**
 * Interface for subscribers that can be configured with a specific connection ID.
 * <p>
 * Subscribers that implement this interface can be configured to use a specific connection
 * configuration based on the connection ID. This allows for multiple connections to the
 * same messaging system type, for example, to listen to different Kafka clusters or
 * RabbitMQ servers.
 * <p>
 * The connection ID is used to look up the appropriate configuration in the
 * {@link com.catalis.common.core.messaging.config.MessagingProperties}.
 */
public interface ConnectionAwareSubscriber {

    /**
     * Sets the connection ID for this subscriber.
     * <p>
     * This method is called by the {@link SubscriberFactory} when creating a subscriber
     * for a specific connection ID.
     *
     * @param connectionId the connection ID to use
     */
    void setConnectionId(String connectionId);

    /**
     * Gets the connection ID for this subscriber.
     *
     * @return the connection ID
     */
    String getConnectionId();
}
