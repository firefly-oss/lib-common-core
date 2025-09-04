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