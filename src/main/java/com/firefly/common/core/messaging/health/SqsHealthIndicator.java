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
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Health indicator for Amazon SQS messaging system.
 * <p>
 * This health indicator checks if the SQS connection is available and reports
 * the health status of the SQS messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "sqs.enabled"}, havingValue = "true")
public class SqsHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<SqsTemplate> sqsTemplateProvider;
    private final ObjectProvider<SqsAsyncClient> sqsAsyncClientProvider;

    /**
     * Creates a new SqsHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param sqsTemplateProvider provider for the SQS template
     * @param sqsAsyncClientProvider provider for the SQS async client
     */
    public SqsHealthIndicator(MessagingProperties messagingProperties,
                             ObjectProvider<SqsTemplate> sqsTemplateProvider,
                             ObjectProvider<SqsAsyncClient> sqsAsyncClientProvider) {
        super(messagingProperties);
        this.sqsTemplateProvider = sqsTemplateProvider;
        this.sqsAsyncClientProvider = sqsAsyncClientProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getSqs().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "Amazon SQS";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        SqsTemplate sqsTemplate = sqsTemplateProvider.getIfAvailable();
        SqsAsyncClient sqsAsyncClient = sqsAsyncClientProvider.getIfAvailable();
        
        if (sqsTemplate == null && sqsAsyncClient == null) {
            return Health.down()
                    .withDetail("error", "Neither SqsTemplate nor SqsAsyncClient is available")
                    .build();
        }

        try {
            // Check if the SQS client is functional
            // We'll use the async client if available, otherwise fall back to the template
            if (sqsAsyncClient != null) {
                // Just check if the client is functional by listing queues
                // This is a lightweight operation that doesn't actually send a message
                sqsAsyncClient.listQueues().get();
            } else {
                // If we only have the template, we can't easily check health without sending a message
                // So we'll just report that the template is available
            }
            
            return Health.up()
                    .withDetail("region", messagingProperties.getSqs().getRegion())
                    .withDetail("defaultQueue", messagingProperties.getSqs().getDefaultQueue())
                    .withDetail("endpoint", messagingProperties.getSqs().getEndpoint())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}