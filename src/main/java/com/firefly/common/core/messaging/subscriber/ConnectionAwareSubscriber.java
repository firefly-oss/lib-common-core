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


package com.firefly.common.core.messaging.subscriber;

/**
 * Interface for subscribers that can be configured with a specific connection ID.
 * <p>
 * Subscribers that implement this interface can be configured to use a specific connection
 * configuration based on the connection ID. This allows for multiple connections to the
 * same messaging system type, for example, to listen to different Kafka clusters or
 * RabbitMQ servers.
 * <p>
 * The connection ID is used to look up the appropriate configuration in the
 * {@link com.firefly.common.core.messaging.config.MessagingProperties}.
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
