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

package com.firefly.common.core.messaging.config;

import com.firefly.common.core.messaging.publisher.EventPublisherFactory;
import com.firefly.common.core.messaging.stepevents.StepEventPublisherBridge;
import com.firefly.transactional.events.StepEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Auto-configuration for Step Events integration with lib-common-core messaging infrastructure.
 * 
 * This configuration creates a bridge between the lib-transactional-engine StepEventPublisher
 * and the lib-common-core messaging system, allowing step events to be published through
 * any of the supported message brokers (Kafka, RabbitMQ, SQS, etc.).
 */
@Configuration
@ConditionalOnClass({StepEventPublisher.class, EventPublisherFactory.class})
@ConditionalOnProperty(prefix = "firefly.stepevents", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StepEventsProperties.class)
@Slf4j
public class StepEventsAutoConfiguration {

    @Bean
    @Primary
    public StepEventPublisher stepEventPublisher(StepEventsProperties properties, 
                                                EventPublisherFactory eventPublisherFactory) {
        log.info("Configuring StepEventPublisher bridge with publisher type: {}, connection: {}, topic: {}", 
                properties.getPublisherType(), properties.getConnectionId(), properties.getDefaultTopic());
                
        return new StepEventPublisherBridge(
                properties.getDefaultTopic(),
                properties.getPublisherType(),
                properties.getConnectionId(),
                eventPublisherFactory
        );
    }
}