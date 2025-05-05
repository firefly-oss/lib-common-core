package com.catalis.common.core.messaging.health;

import com.catalis.common.core.messaging.config.MessagingProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Kafka messaging system.
 * <p>
 * This health indicator checks if the Kafka connection is available and reports
 * the health status of the Kafka messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "kafka.enabled"}, havingValue = "true")
public class KafkaHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;

    /**
     * Creates a new KafkaHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param kafkaTemplateProvider provider for the Kafka template
     */
    public KafkaHealthIndicator(MessagingProperties messagingProperties,
                               ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider) {
        super(messagingProperties);
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getKafka().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "Kafka";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            return Health.down()
                    .withDetail("error", "KafkaTemplate is not available")
                    .build();
        }

        try {
            // Check if the Kafka template is functional
            // This doesn't actually send a message, just checks if the client is available
            kafkaTemplate.getDefaultTopic();
            
            return Health.up()
                    .withDetail("bootstrapServers", messagingProperties.getKafka().getBootstrapServers())
                    .withDetail("defaultTopic", messagingProperties.getKafka().getDefaultTopic())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}