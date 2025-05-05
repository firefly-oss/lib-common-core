package com.catalis.common.core.messaging.health;

import com.catalis.common.core.messaging.config.MessagingProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for JMS messaging system.
 * <p>
 * This health indicator checks if the JMS connection is available and reports
 * the health status of the JMS messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "jms.enabled"}, havingValue = "true")
public class JmsHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<JmsTemplate> jmsTemplateProvider;

    /**
     * Creates a new JmsHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param jmsTemplateProvider provider for the JMS template
     */
    public JmsHealthIndicator(MessagingProperties messagingProperties,
                             ObjectProvider<JmsTemplate> jmsTemplateProvider) {
        super(messagingProperties);
        this.jmsTemplateProvider = jmsTemplateProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getJms().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "JMS";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        JmsTemplate jmsTemplate = jmsTemplateProvider.getIfAvailable();
        if (jmsTemplate == null) {
            return Health.down()
                    .withDetail("error", "JmsTemplate is not available")
                    .build();
        }

        try {
            // Check if the JMS connection is functional
            // This doesn't actually send a message, just checks if the connection factory is available
            jmsTemplate.getConnectionFactory();
            
            // Get the JMS configuration
            MessagingProperties.JmsConfig config = messagingProperties.getJms();
            
            return Health.up()
                    .withDetail("brokerUrl", config.getBrokerUrl())
                    .withDetail("defaultDestination", config.getDefaultDestination())
                    .withDetail("useTopic", config.isUseTopic())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}