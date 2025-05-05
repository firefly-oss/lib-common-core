package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;

/**
 * Implementation of {@link EventPublisher} that publishes events to RabbitMQ.
 * <p>
 * This implementation supports multiple RabbitMQ connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "rabbitmq.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
public class RabbitMqEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final MessagingProperties messagingProperties;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate == null) {
                log.warn("RabbitTemplate is not available. Event will not be published to RabbitMQ.");
                return Mono.error(new IllegalStateException("RabbitTemplate is not available"));
            }

            log.debug("Publishing event to RabbitMQ: exchange={}, type={}, transactionId={}",
                    destination, eventType, transactionId);

            try {
                MessageProperties properties = new MessageProperties();

                if (eventType != null) {
                    properties.setHeader("eventType", eventType);
                }

                if (transactionId != null) {
                    properties.setHeader("transactionId", transactionId);
                }

                // Get the RabbitMQ configuration for this connection ID
                MessagingProperties.RabbitMqConfig rabbitConfig = messagingProperties.getRabbitMqConfig(connectionId);

                // Use default exchange if not specified
                String exchange = (destination == null || destination.isEmpty()) ?
                        rabbitConfig.getDefaultExchange() : destination;

                String routingKey = (eventType == null || eventType.isEmpty()) ?
                        rabbitConfig.getDefaultRoutingKey() : eventType;

                // Use a safer approach with try-catch
                rabbitTemplate.convertAndSend(exchange, routingKey, payload != null ? payload : "", message -> {
                    message.getMessageProperties().getHeaders().putAll(properties.getHeaders());
                    return message;
                });

                log.debug("Successfully published event to RabbitMQ: exchange={}, routingKey={}", exchange, routingKey);
                return Mono.empty();
            } catch (Exception e) {
                log.error("Failed to publish event to RabbitMQ: {}", e.getMessage(), e);
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

            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate == null) {
                log.warn("RabbitTemplate is not available. Event will not be published to RabbitMQ.");
                return Mono.error(new IllegalStateException("RabbitTemplate is not available"));
            }

            log.debug("Publishing event to RabbitMQ with serializer {}: exchange={}, type={}, transactionId={}",
                    serializer.getFormat(), destination, eventType, transactionId);

            try {
                // Serialize the payload
                byte[] serializedPayload = payload != null ? serializer.serialize(payload) : new byte[0];

                MessageProperties properties = new MessageProperties();
                properties.setContentType(serializer.getContentType());

                if (eventType != null) {
                    properties.setHeader("eventType", eventType);
                }

                if (transactionId != null) {
                    properties.setHeader("transactionId", transactionId);
                }

                // Get the RabbitMQ configuration for this connection ID
                MessagingProperties.RabbitMqConfig rabbitConfig = messagingProperties.getRabbitMqConfig(connectionId);

                // Use default exchange if not specified
                String exchange = (destination == null || destination.isEmpty()) ?
                        rabbitConfig.getDefaultExchange() : destination;

                String routingKey = (eventType == null || eventType.isEmpty()) ?
                        rabbitConfig.getDefaultRoutingKey() : eventType;

                // Create a message with the serialized payload
                org.springframework.amqp.core.Message message =
                        new org.springframework.amqp.core.Message(serializedPayload, properties);

                // Send the message
                rabbitTemplate.send(exchange, routingKey, message);

                log.debug("Successfully published event to RabbitMQ with serializer {}: exchange={}, routingKey={}",
                        serializer.getFormat(), exchange, routingKey);
                return Mono.empty();
            } catch (SerializationException e) {
                log.error("Failed to serialize payload for RabbitMQ: {}", e.getMessage(), e);
                return Mono.error(e);
            } catch (Exception e) {
                log.error("Failed to publish event to RabbitMQ: {}", e.getMessage(), e);
                return Mono.error(e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return rabbitTemplateProvider.getIfAvailable() != null &&
               messagingProperties.getRabbitMqConfig(connectionId).isEnabled();
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
