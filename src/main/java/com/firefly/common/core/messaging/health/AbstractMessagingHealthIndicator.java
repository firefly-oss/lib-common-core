package com.firefly.common.core.messaging.health;

import com.firefly.common.core.messaging.config.MessagingProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Base class for all messaging system health indicators.
 * <p>
 * This abstract class provides common functionality for checking the health of
 * messaging systems. It ensures that health checks are only performed if the
 * messaging system is enabled in the configuration.
 */
public abstract class AbstractMessagingHealthIndicator implements HealthIndicator {

    protected final MessagingProperties messagingProperties;

    protected AbstractMessagingHealthIndicator(MessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @Override
    public Health health() {
        if (!messagingProperties.isEnabled()) {
            return Health.unknown()
                    .withDetail("status", "Messaging is disabled")
                    .build();
        }

        if (!isSpecificMessagingSystemEnabled()) {
            return Health.unknown()
                    .withDetail("status", getMessagingSystemName() + " is disabled")
                    .build();
        }

        try {
            return checkMessagingSystemHealth();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    /**
     * Check if the specific messaging system is enabled in the configuration.
     *
     * @return true if the messaging system is enabled, false otherwise
     */
    protected abstract boolean isSpecificMessagingSystemEnabled();

    /**
     * Get the name of the messaging system.
     *
     * @return the name of the messaging system
     */
    protected abstract String getMessagingSystemName();

    /**
     * Check the health of the messaging system.
     *
     * @return a Health object representing the health of the messaging system
     * @throws Exception if an error occurs while checking the health
     */
    protected abstract Health checkMessagingSystemHealth() throws Exception;
}
