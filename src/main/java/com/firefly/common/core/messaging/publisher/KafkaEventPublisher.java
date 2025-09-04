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


package com.firefly.common.core.messaging.publisher;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.serialization.MessageSerializer;
import com.firefly.common.core.messaging.serialization.SerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link EventPublisher} that publishes events to Kafka.
 * <p>
 * This implementation supports multiple Kafka connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "kafka.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
public class KafkaEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    private final MessagingProperties messagingProperties;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            log.info("=== KAFKA PUBLISH ATTEMPT ===");
            log.info("Destination: {}", destination);
            log.info("Event Type: {}", eventType);
            log.info("Payload: {}", payload);
            log.info("Transaction ID: {}", transactionId);
            log.info("Connection ID: {}", connectionId);

            KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
            log.info("KafkaTemplate available: {}", kafkaTemplate != null);

            if (kafkaTemplate == null) {
                log.warn("KafkaTemplate is not available. Event will not be published to Kafka.");
                return Mono.empty();
            }

            if (destination == null || destination.isEmpty()) {
                log.error("Destination is null or empty");
                return Mono.error(new IllegalArgumentException("Destination cannot be null or empty"));
            }

            log.info("Publishing event to Kafka: topic={}, type={}, transactionId={}",
                    destination, eventType, transactionId);

            try {
                MessageBuilder<Object> messageBuilder = MessageBuilder
                        .withPayload(payload != null ? payload : "")
                        .setHeader(KafkaHeaders.TOPIC, destination);

                if (eventType != null) {
                    messageBuilder.setHeader("eventType", eventType);
                }

                messageBuilder.setHeader("contentType", "application/json");

                if (transactionId != null) {
                    messageBuilder.setHeader("transactionId", transactionId);
                }

                Message<Object> message = messageBuilder.build();
                log.info("Message built: {}", message);

                // Send the message and return a completed Mono
                var sendResult = kafkaTemplate.send(message);
                log.info("Send result: {}", sendResult);

                log.info("Event sent to Kafka: topic={}, type={}", destination, eventType);
                return Mono.empty();
            } catch (Exception e) {
                log.error("Failed to publish event to Kafka: {}", e.getMessage(), e);
                return Mono.error(e);
            }
        });
    }

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId, MessageSerializer serializer) {
        return Mono.defer(() -> {
            if (serializer == null) {
                log.warn("Serializer is null, falling back to default publish method");
                return publish(destination, eventType, payload, transactionId);
            }

            KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
            if (kafkaTemplate == null) {
                log.warn("KafkaTemplate is not available. Event will not be published to Kafka.");
                return Mono.empty();
            }

            if (destination == null || destination.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Destination cannot be null or empty"));
            }

            log.debug("Publishing event to Kafka with serializer {}: topic={}, type={}, transactionId={}",
                    serializer.getFormat(), destination, eventType, transactionId);

            try {
                // Serialize the payload
                byte[] serializedPayload = payload != null ? serializer.serialize(payload) : new byte[0];

                MessageBuilder<byte[]> messageBuilder = MessageBuilder
                        .withPayload(serializedPayload)
                        .setHeader(KafkaHeaders.TOPIC, destination);

                if (eventType != null) {
                    messageBuilder.setHeader("eventType", eventType);
                }

                messageBuilder.setHeader("contentType", serializer.getContentType());

                if (transactionId != null) {
                    messageBuilder.setHeader("transactionId", transactionId);
                }

                Message<byte[]> message = messageBuilder.build();

                // Send the message and return a completed Mono
                kafkaTemplate.send(message);
                log.debug("Event sent to Kafka with serializer {}: topic={}, type={}",
                        serializer.getFormat(), destination, eventType);
                return Mono.empty();
            } catch (SerializationException e) {
                log.error("Failed to serialize payload for Kafka: {}", e.getMessage(), e);
                return Mono.error(e);
            } catch (Exception e) {
                log.error("Failed to publish event to Kafka: {}", e.getMessage(), e);
                return Mono.error(e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        KafkaTemplate<String, Object> template = kafkaTemplateProvider.getIfAvailable();
        boolean templateAvailable = template != null;
        boolean configEnabled = messagingProperties.getKafkaConfig(connectionId).isEnabled();

        log.info("=== KAFKA AVAILABILITY CHECK ===");
        log.info("Connection ID: {}", connectionId);
        log.info("KafkaTemplate available: {}", templateAvailable);
        log.info("Config enabled: {}", configEnabled);
        log.info("Overall available: {}", templateAvailable && configEnabled);

        return templateAvailable && configEnabled;
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }
}
