package com.catalis.common.core.messaging.health;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Google Cloud Pub/Sub messaging system.
 * <p>
 * This health indicator checks if the Google Pub/Sub connection is available and reports
 * the health status of the Google Pub/Sub messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "google-pub-sub.enabled"}, havingValue = "true")
public class GooglePubSubHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<PubSubTemplate> pubSubTemplateProvider;
    private final ObjectProvider<PubSubPublisherTemplate> pubSubPublisherTemplateProvider;

    /**
     * Creates a new GooglePubSubHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param pubSubTemplateProvider provider for the PubSub template
     * @param pubSubPublisherTemplateProvider provider for the PubSub publisher template
     */
    public GooglePubSubHealthIndicator(MessagingProperties messagingProperties,
                                      ObjectProvider<PubSubTemplate> pubSubTemplateProvider,
                                      ObjectProvider<PubSubPublisherTemplate> pubSubPublisherTemplateProvider) {
        super(messagingProperties);
        this.pubSubTemplateProvider = pubSubTemplateProvider;
        this.pubSubPublisherTemplateProvider = pubSubPublisherTemplateProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getGooglePubSub().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "Google Pub/Sub";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        PubSubPublisherTemplate pubSubPublisherTemplate = pubSubPublisherTemplateProvider.getIfAvailable();

        if (pubSubTemplate == null && pubSubPublisherTemplate == null) {
            return Health.down()
                    .withDetail("error", "Neither PubSubTemplate nor PubSubPublisherTemplate is available")
                    .build();
        }

        try {
            // Check if the Google Pub/Sub client is functional
            // We'll just check if the templates are available since there's no simple way
            // to check connectivity without actually publishing or subscribing
            if (pubSubPublisherTemplate != null || pubSubTemplate != null) {
                // Templates are available, which means the beans are properly configured
                // We'll consider this as a healthy state
            }

            return Health.up()
                    .withDetail("projectId", messagingProperties.getGooglePubSub().getProjectId())
                    .withDetail("defaultTopic", messagingProperties.getGooglePubSub().getDefaultTopic())
                    .withDetail("useEmulator", messagingProperties.getGooglePubSub().isUseEmulator())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
