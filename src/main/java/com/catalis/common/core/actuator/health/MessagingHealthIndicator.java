package com.catalis.common.core.actuator.health;

import com.catalis.common.core.messaging.config.MessagingProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for messaging systems.
 * <p>
 * This class provides health information about the configured messaging systems.
 * It reports the status of each enabled messaging system and provides details
 * about their configuration.
 */
@Component
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
@ConditionalOnBean(MessagingProperties.class)
public class MessagingHealthIndicator implements HealthIndicator {

    private final MessagingProperties messagingProperties;

    /**
     * Constructs a new MessagingHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     */
    public MessagingHealthIndicator(MessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @Override
    public Health health() {
        if (!messagingProperties.isEnabled()) {
            return Health.up().withDetail("status", "Messaging is disabled").build();
        }

        Map<String, Object> details = new HashMap<>();
        boolean allUp = true;

        // Check Kafka
        if (messagingProperties.getKafka().isEnabled()) {
            try {
                details.put("kafka", Map.of(
                        "status", "UP",
                        "bootstrapServers", messagingProperties.getKafka().getBootstrapServers(),
                        "defaultTopic", messagingProperties.getKafka().getDefaultTopic()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("kafka", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check RabbitMQ
        if (messagingProperties.getRabbitmq().isEnabled()) {
            try {
                details.put("rabbitmq", Map.of(
                        "status", "UP",
                        "host", messagingProperties.getRabbitmq().getHost(),
                        "port", messagingProperties.getRabbitmq().getPort(),
                        "defaultExchange", messagingProperties.getRabbitmq().getDefaultExchange()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("rabbitmq", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check SQS
        if (messagingProperties.getSqs().isEnabled()) {
            try {
                details.put("sqs", Map.of(
                        "status", "UP",
                        "region", messagingProperties.getSqs().getRegion(),
                        "defaultQueue", messagingProperties.getSqs().getDefaultQueue()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("sqs", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check Google Pub/Sub
        if (messagingProperties.getGooglePubSub().isEnabled()) {
            try {
                details.put("googlePubSub", Map.of(
                        "status", "UP",
                        "projectId", messagingProperties.getGooglePubSub().getProjectId(),
                        "defaultTopic", messagingProperties.getGooglePubSub().getDefaultTopic()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("googlePubSub", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check Azure Service Bus
        if (messagingProperties.getAzureServiceBus().isEnabled()) {
            try {
                details.put("azureServiceBus", Map.of(
                        "status", "UP",
                        "namespace", messagingProperties.getAzureServiceBus().getNamespace(),
                        "defaultTopic", messagingProperties.getAzureServiceBus().getDefaultTopic()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("azureServiceBus", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check Redis
        if (messagingProperties.getRedis().isEnabled()) {
            try {
                details.put("redis", Map.of(
                        "status", "UP",
                        "host", messagingProperties.getRedis().getHost(),
                        "port", messagingProperties.getRedis().getPort(),
                        "defaultChannel", messagingProperties.getRedis().getDefaultChannel()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("redis", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check JMS
        if (messagingProperties.getJms().isEnabled()) {
            try {
                details.put("jms", Map.of(
                        "status", "UP",
                        "brokerUrl", messagingProperties.getJms().getBrokerUrl(),
                        "defaultDestination", messagingProperties.getJms().getDefaultDestination()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("jms", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        // Check Kinesis
        if (messagingProperties.getKinesis().isEnabled()) {
            try {
                details.put("kinesis", Map.of(
                        "status", "UP",
                        "region", messagingProperties.getKinesis().getRegion(),
                        "defaultStream", messagingProperties.getKinesis().getDefaultStream()
                ));
            } catch (Exception e) {
                allUp = false;
                details.put("kinesis", Map.of(
                        "status", "DOWN",
                        "error", e.getMessage()
                ));
            }
        }

        if (allUp) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
