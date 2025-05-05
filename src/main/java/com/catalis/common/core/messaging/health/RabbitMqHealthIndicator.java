package com.catalis.common.core.messaging.health;

import com.catalis.common.core.messaging.config.MessagingProperties;
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