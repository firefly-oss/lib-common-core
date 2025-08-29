package com.firefly.common.core.messaging.health;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.firefly.common.core.messaging.config.MessagingProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Azure Service Bus messaging system.
 * <p>
 * This health indicator checks if the Azure Service Bus connection is available and reports
 * the health status of the Azure Service Bus messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "azure-service-bus.enabled"}, havingValue = "true")
public class AzureServiceBusHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider;

    /**
     * Creates a new AzureServiceBusHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param serviceBusClientBuilderProvider provider for the Azure Service Bus client builder
     */
    public AzureServiceBusHealthIndicator(MessagingProperties messagingProperties,
                                         ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider) {
        super(messagingProperties);
        this.serviceBusClientBuilderProvider = serviceBusClientBuilderProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getAzureServiceBus().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "Azure Service Bus";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        ServiceBusClientBuilder clientBuilder = serviceBusClientBuilderProvider.getIfAvailable();
        if (clientBuilder == null) {
            return Health.down()
                    .withDetail("error", "ServiceBusClientBuilder is not available")
                    .build();
        }

        try {
            // Get the Azure Service Bus configuration
            MessagingProperties.AzureServiceBusConfig config = messagingProperties.getAzureServiceBus();
            
            // Check if we have the necessary configuration to connect
            if (config.getConnectionString().isEmpty() && 
                (config.getNamespace().isEmpty() || 
                 (config.getSharedAccessKeyName().isEmpty() && !config.isUseManagedIdentity()))) {
                return Health.down()
                        .withDetail("error", "Insufficient configuration for Azure Service Bus connection")
                        .build();
            }
            
            // We won't actually create a client and test the connection here
            // as that could be expensive and might create unnecessary connections
            // Instead, we'll just report that the builder is available and properly configured
            
            return Health.up()
                    .withDetail("namespace", config.getNamespace())
                    .withDetail("defaultTopic", config.getDefaultTopic())
                    .withDetail("defaultQueue", config.getDefaultQueue())
                    .withDetail("useManagedIdentity", config.isUseManagedIdentity())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}