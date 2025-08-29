package com.firefly.common.core.messaging.health;

import com.firefly.common.core.messaging.config.MessagingProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

/**
 * Health indicator for AWS Kinesis messaging system.
 * <p>
 * This health indicator checks if the Kinesis connection is available and reports
 * the health status of the Kinesis messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "kinesis.enabled"}, havingValue = "true")
public class KinesisHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<KinesisAsyncClient> kinesisClientProvider;

    /**
     * Creates a new KinesisHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param kinesisClientProvider provider for the Kinesis client
     */
    public KinesisHealthIndicator(MessagingProperties messagingProperties,
                                 ObjectProvider<KinesisAsyncClient> kinesisClientProvider) {
        super(messagingProperties);
        this.kinesisClientProvider = kinesisClientProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getKinesis().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "AWS Kinesis";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        KinesisAsyncClient kinesisClient = kinesisClientProvider.getIfAvailable();
        if (kinesisClient == null) {
            return Health.down()
                    .withDetail("error", "KinesisAsyncClient is not available")
                    .build();
        }

        try {
            // Check if the Kinesis client is functional
            // This doesn't actually send a message, just checks if the client can list streams
            kinesisClient.listStreams().get();
            
            // Get the Kinesis configuration
            MessagingProperties.KinesisConfig config = messagingProperties.getKinesis();
            
            return Health.up()
                    .withDetail("region", config.getRegion())
                    .withDetail("defaultStream", config.getDefaultStream())
                    .withDetail("endpoint", config.getEndpoint())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}