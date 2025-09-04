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


package com.firefly.common.core.messaging.health;

import com.firefly.common.core.messaging.config.MessagingProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for RabbitMQ messaging system.
 * <p>
 * This health indicator checks if the RabbitMQ connection is available and reports
 * the health status of the RabbitMQ messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "rabbitmq.enabled"}, havingValue = "true")
public class RabbitMqHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    /**
     * Creates a new RabbitMqHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param rabbitTemplateProvider provider for the RabbitMQ template
     */
    public RabbitMqHealthIndicator(MessagingProperties messagingProperties,
                                  ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        super(messagingProperties);
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getRabbitmq().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "RabbitMQ";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            return Health.down()
                    .withDetail("error", "RabbitTemplate is not available")
                    .build();
        }

        try {
            // Check if the RabbitMQ connection is functional
            // This doesn't actually send a message, just checks if the connection is available
            rabbitTemplate.getConnectionFactory().createConnection();
            
            return Health.up()
                    .withDetail("host", messagingProperties.getRabbitmq().getHost())
                    .withDetail("port", messagingProperties.getRabbitmq().getPort())
                    .withDetail("virtualHost", messagingProperties.getRabbitmq().getVirtualHost())
                    .withDetail("defaultExchange", messagingProperties.getRabbitmq().getDefaultExchange())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}