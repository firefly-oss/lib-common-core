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

import com.firefly.common.core.messaging.publisher.EventPublisher;
import com.firefly.common.core.messaging.subscriber.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Reports on the status of messaging systems at application startup.
 * <p>
 * This component logs information about which messaging systems are enabled and loaded
 * when the application starts. It helps developers understand the current configuration
 * and troubleshoot issues with messaging system loading.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
public class MessagingSystemReport implements ApplicationListener<ApplicationReadyEvent> {
    
    private final MessagingProperties properties;
    private final ApplicationContext context;
    private final Logger log = LoggerFactory.getLogger(MessagingSystemReport.class);
    
    /**
     * Creates a new MessagingSystemReport.
     *
     * @param properties the messaging properties
     * @param context the application context
     */
    public MessagingSystemReport(MessagingProperties properties, ApplicationContext context) {
        this.properties = properties;
        this.context = context;
    }
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=== Messaging System Configuration Report ===");
        log.info("Overall messaging enabled: {}", properties.isEnabled());
        
        // Report on each messaging system
        reportSystem("Spring Event Bus", true, 
            context.containsBean("springEventPublisher"),
            context.containsBean("springEventSubscriber"));
            
        reportSystem("Kafka", properties.getKafka().isEnabled(), 
            context.containsBean("kafkaEventPublisher"),
            context.containsBean("kafkaEventSubscriber"));
            
        reportSystem("RabbitMQ", properties.getRabbitmq().isEnabled(), 
            context.containsBean("rabbitMqEventPublisher"),
            context.containsBean("rabbitMqEventSubscriber"));
            
        reportSystem("Amazon SQS", properties.getSqs().isEnabled(), 
            context.containsBean("sqsEventPublisher"),
            context.containsBean("sqsEventSubscriber"));
            
        reportSystem("Google Pub/Sub", properties.getGooglePubSub().isEnabled(), 
            context.containsBean("googlePubSubEventPublisher"),
            context.containsBean("googlePubSubEventSubscriber"));
            
        reportSystem("Azure Service Bus", properties.getAzureServiceBus().isEnabled(), 
            context.containsBean("azureServiceBusEventPublisher"),
            context.containsBean("azureServiceBusEventSubscriber"));
            
        reportSystem("Redis", properties.getRedis().isEnabled(), 
            context.containsBean("redisEventPublisher"),
            context.containsBean("redisEventSubscriber"));
            
        reportSystem("JMS", properties.getJms().isEnabled(), 
            context.containsBean("jmsEventPublisher"),
            context.containsBean("jmsEventSubscriber"));
            
        reportSystem("Kinesis", properties.getKinesis().isEnabled(), 
            context.containsBean("kinesisEventPublisher"),
            context.containsBean("kinesisEventSubscriber"));
            
        // Report on available publishers and subscribers
        reportAvailableComponents();
    }
    
    private void reportSystem(String name, boolean configEnabled, boolean publisherLoaded, boolean subscriberLoaded) {
        log.info("{}: Configuration enabled: {}, Publisher loaded: {}, Subscriber loaded: {}", 
            name, configEnabled, publisherLoaded, subscriberLoaded);
    }
    
    private void reportAvailableComponents() {
        try {
            Map<String, EventPublisher> publishers = context.getBeansOfType(EventPublisher.class);
            Map<String, EventSubscriber> subscribers = context.getBeansOfType(EventSubscriber.class);
            
            log.info("Available Publishers: {}", publishers.keySet());
            log.info("Available Subscribers: {}", subscribers.keySet());
        } catch (Exception e) {
            log.warn("Error reporting available components: {}", e.getMessage());
        }
    }
}
