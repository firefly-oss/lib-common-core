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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MessagingPropertiesTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.config.import=optional:configserver:",
        "messaging.enabled=true",
        "messaging.resilience=true",
        "messaging.publish-timeout-seconds=10",
        "messaging.kafka.enabled=true",
        "messaging.kafka.default-topic=kafka-topic",
        "messaging.rabbitmq.enabled=true",
        "messaging.rabbitmq.default-exchange=rabbitmq-exchange",
        "messaging.rabbitmq.default-routing-key=rabbitmq-routing-key",
        "messaging.sqs.enabled=true",
        "messaging.sqs.default-queue=sqs-queue",
        "messaging.sqs.region=us-west-2",
        "messaging.google-pub-sub.enabled=true",
        "messaging.google-pub-sub.default-topic=pubsub-topic",
        "messaging.google-pub-sub.project-id=my-project",
        "messaging.azure-service-bus.enabled=true",
        "messaging.azure-service-bus.default-topic=azure-topic",
        "messaging.azure-service-bus.default-queue=azure-queue",
        "messaging.azure-service-bus.connection-string=connection-string",
        "messaging.redis.enabled=true",
        "messaging.redis.default-channel=redis-channel",
        "messaging.jms.enabled=true",
        "messaging.jms.default-destination=jms-destination",
        "messaging.jms.use-topic=false"
})
public class MessagingPropertiesTest {

    @Autowired
    private MessagingProperties properties;

    @Test
    void shouldBindProperties() {
        // Global properties
        assertTrue(properties.isEnabled());
        assertTrue(properties.isResilience());
        assertEquals(10, properties.getPublishTimeoutSeconds());

        // Kafka properties
        assertTrue(properties.getKafka().isEnabled());
        assertEquals("kafka-topic", properties.getKafka().getDefaultTopic());

        // RabbitMQ properties
        assertTrue(properties.getRabbitmq().isEnabled());
        assertEquals("rabbitmq-exchange", properties.getRabbitmq().getDefaultExchange());
        assertEquals("rabbitmq-routing-key", properties.getRabbitmq().getDefaultRoutingKey());

        // SQS properties
        assertTrue(properties.getSqs().isEnabled());
        assertEquals("sqs-queue", properties.getSqs().getDefaultQueue());
        assertEquals("us-west-2", properties.getSqs().getRegion());

        // Google Pub/Sub properties
        assertTrue(properties.getGooglePubSub().isEnabled());
        assertEquals("pubsub-topic", properties.getGooglePubSub().getDefaultTopic());
        assertEquals("my-project", properties.getGooglePubSub().getProjectId());

        // Azure Service Bus properties
        assertTrue(properties.getAzureServiceBus().isEnabled());
        assertEquals("azure-topic", properties.getAzureServiceBus().getDefaultTopic());
        assertEquals("azure-queue", properties.getAzureServiceBus().getDefaultQueue());
        assertEquals("connection-string", properties.getAzureServiceBus().getConnectionString());

        // Redis properties
        assertTrue(properties.getRedis().isEnabled());
        assertEquals("redis-channel", properties.getRedis().getDefaultChannel());

        // JMS properties
        assertTrue(properties.getJms().isEnabled());
        assertEquals("jms-destination", properties.getJms().getDefaultDestination());
        assertFalse(properties.getJms().isUseTopic());
    }

    @EnableConfigurationProperties(MessagingProperties.class)
    static class TestConfiguration {
    }
}
