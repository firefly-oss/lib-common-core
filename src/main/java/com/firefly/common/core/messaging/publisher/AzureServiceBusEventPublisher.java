package com.firefly.common.core.messaging.publisher;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.serialization.MessageSerializer;
import com.firefly.common.core.messaging.serialization.SerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link EventPublisher} that publishes events directly
 * via Azure Service Bus Async Client.
 * <p>
 * This implementation supports multiple Azure Service Bus connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "azure-service-bus.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
public class AzureServiceBusEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<ServiceBusClientBuilder> clientBuilderProvider;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    private String connectionId = "default";

    /**
     * Publish an event with default JSON serialization.
     *
     * @param destination   target queue or topic (nullable to use defaults)
     * @param eventType     event subject/type
     * @param payload       payload object
     * @param transactionId optional transaction ID header
     * @return Mono<Void> signaling completion or error
     */
    @Override
    public Mono<Void> publish(String destination,
                              String eventType,
                              Object payload,
                              String transactionId) {
        byte[] body = toJsonBytes(payload);
        return publishInternal(destination, eventType, body, "application/json", transactionId);
    }

    /**
     * Publish an event with a custom serializer.
     *
     * @param destination   target queue or topic
     * @param eventType     event subject/type
     * @param payload       payload object
     * @param transactionId optional transaction ID header
     * @param serializer    custom serializer (if null, falls back to JSON)
     * @return Mono<Void> signaling completion or error
     */
    @Override
    public Mono<Void> publish(String destination,
                              String eventType,
                              Object payload,
                              String transactionId,
                              MessageSerializer serializer) {
        if (serializer == null) {
            log.warn("Serializer is null; falling back to JSON");
            return publish(destination, eventType, payload, transactionId);
        }
        byte[] body = toCustomBytes(serializer, payload);
        return publishInternal(destination, eventType, body, serializer.getContentType(), transactionId);
    }

    /**
     * Returns true if we can build a Service Bus client.
     */
    @Override
    public boolean isAvailable() {
        return clientBuilderProvider.getIfAvailable() != null &&
               messagingProperties.getAzureServiceBusConfig(connectionId).isEnabled();
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Core send logic: builds sender, constructs message, and sends it.
     */
    private Mono<Void> publishInternal(String destination,
                                       String eventType,
                                       byte[] body,
                                       String contentType,
                                       String transactionId) {
        ServiceBusClientBuilder builder = clientBuilderProvider.getIfAvailable();
        if (builder == null) {
            String err = "ServiceBusClientBuilder is not available";
            log.warn(err);
            return Mono.error(new IllegalStateException(err));
        }

        String entityPath = resolveEntityPath(destination);
        if (!StringUtils.hasText(entityPath)) {
            return Mono.error(new IllegalArgumentException("Entity path cannot be null or empty"));
        }

        log.debug("Publishing to Azure Service Bus: entityPath={}, eventType={}, txId={}",
                entityPath, eventType, transactionId);

        // Create or reuse an Async sender client for this entity
        ServiceBusSenderAsyncClient sender = createAsyncSender(builder, entityPath);

        // Build the message
        ServiceBusMessage message = new ServiceBusMessage(body)
                .setContentType(contentType);

        if (StringUtils.hasText(eventType)) {
            message.setSubject(eventType);
        }
        if (StringUtils.hasText(transactionId)) {
            message.getApplicationProperties().put("transactionId", transactionId);
        }

        // Send and return a reactive Mono
        return sender.sendMessage(message)
                .doOnSuccess(v -> log.debug("Message published successfully to {}", entityPath))
                .doOnError(e -> log.error("Failed to publish to {}: {}", entityPath, e.getMessage(), e))
                // Close sender after send to free resources
                .doFinally(sig -> sender.close());
    }

    /**
     * Resolves the actual queue or topic name, falling back to defaults.
     */
    private String resolveEntityPath(String destination) {
        if (StringUtils.hasText(destination)) {
            return destination;
        }
        // Get the Azure Service Bus configuration for this connection ID
        MessagingProperties.AzureServiceBusConfig azureConfig = messagingProperties.getAzureServiceBusConfig(connectionId);

        String defaultTopic = azureConfig.getDefaultTopic();
        if (StringUtils.hasText(defaultTopic)) {
            return defaultTopic;
        }
        return azureConfig.getDefaultQueue();
    }

    /**
     * Creates a new ServiceBusSenderAsyncClient for the given entity.
     * Detects topics vs queues by a simple naming convention (e.g. "/topics/").
     */
    private ServiceBusSenderAsyncClient createAsyncSender(ServiceBusClientBuilder builder,
                                                          String entityPath) {
        // If entityPath contains "/topics/", treat it as a topic; otherwise as a queue
        if (entityPath.contains("/topics/")) {
            String topicName = entityPath.substring(entityPath.indexOf("/topics/") + "/topics/".length());
            return builder.sender()
                    .topicName(topicName)
                    .buildAsyncClient();
        } else {
            return builder.sender()
                    .queueName(entityPath)
                    .buildAsyncClient();
        }
    }

    /**
     * Convert payload to JSON bytes, handling null as "null".
     */
    private byte[] toJsonBytes(Object payload) {
        try {
            if (payload == null) {
                return "null".getBytes(StandardCharsets.UTF_8);
            }
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            throw new IllegalStateException("Failed to serialize payload to JSON", e);
        }
    }

    /**
     * Use the provided MessageSerializer to serialize the payload.
     */
    private byte[] toCustomBytes(MessageSerializer serializer, Object payload) {
        try {
            if (payload == null) {
                return new byte[0];
            }
            return serializer.serialize(payload);
        } catch (SerializationException e) {
            log.error("Custom serialization failed", e);
            throw new IllegalStateException("Failed to serialize payload with custom serializer", e);
        }
    }
}