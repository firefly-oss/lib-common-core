package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link EventPublisher} that publishes events to Amazon SQS.
 * <p>
 * This implementation supports multiple SQS connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SqsEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<SqsTemplate> sqsTemplateProvider;
    private final ObjectProvider<SqsAsyncClient> sqsAsyncClientProvider;
    private final MessagingProperties messagingProperties;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            SqsTemplate sqsTemplate = sqsTemplateProvider.getIfAvailable();
            if (sqsTemplate == null) {
                log.warn("SqsTemplate is not available. Event will not be published to SQS.");
                return Mono.empty();
            }

            // Get the SQS configuration for this connection ID
            MessagingProperties.SqsConfig sqsConfig = messagingProperties.getSqsConfig(connectionId);

            // Use default queue if not specified
            String queueName = (destination == null || destination.isEmpty()) ?
                    sqsConfig.getDefaultQueue() : destination;

            if (queueName == null || queueName.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Queue name cannot be null or empty"));
            }

            log.debug("Publishing event to SQS: queue={}, type={}, transactionId={}",
                    queueName, eventType, transactionId);

            try {
                // Build the message with proper null handling
                MessageBuilder<?> messageBuilder = MessageBuilder
                        .withPayload(payload != null ? payload : "");

                if (eventType != null) {
                    messageBuilder.setHeader("eventType", eventType);
                }

                if (transactionId != null) {
                    messageBuilder.setHeader("transactionId", transactionId);
                }

                org.springframework.messaging.Message<?> message = messageBuilder.build();

                // Send the message and return a Mono that completes when the send operation is done
                return Mono.fromFuture(sqsTemplate.sendAsync(queueName, message))
                        .doOnSuccess(result -> log.debug("Successfully published event to SQS: queue={}", queueName))
                        .then();
            } catch (Exception e) {
                log.error("Failed to publish event to SQS: {}", e.getMessage(), e);
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

            SqsTemplate sqsTemplate = sqsTemplateProvider.getIfAvailable();
            if (sqsTemplate == null) {
                log.warn("SqsTemplate is not available. Event will not be published to SQS.");
                return Mono.empty();
            }

            // Get the SQS configuration for this connection ID
            MessagingProperties.SqsConfig sqsConfig = messagingProperties.getSqsConfig(connectionId);

            // Use default queue if not specified
            String queueName = (destination == null || destination.isEmpty()) ?
                    sqsConfig.getDefaultQueue() : destination;

            if (queueName == null || queueName.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Queue name cannot be null or empty"));
            }

            log.debug("Publishing event to SQS with serializer {}: queue={}, type={}, transactionId={}",
                    serializer.getFormat(), queueName, eventType, transactionId);

            try {
                // Serialize the payload
                byte[] serializedPayload = payload != null ? serializer.serialize(payload) : new byte[0];

                // Build the message with proper null handling
                MessageBuilder<byte[]> messageBuilder = MessageBuilder
                        .withPayload(serializedPayload)
                        .setHeader("contentType", serializer.getContentType());

                if (eventType != null) {
                    messageBuilder.setHeader("eventType", eventType);
                }

                if (transactionId != null) {
                    messageBuilder.setHeader("transactionId", transactionId);
                }

                org.springframework.messaging.Message<byte[]> message = messageBuilder.build();

                // Send the message and return a Mono that completes when the send operation is done
                return Mono.fromFuture(sqsTemplate.sendAsync(queueName, message))
                        .doOnSuccess(result -> log.debug("Successfully published event to SQS with serializer {}: queue={}",
                                serializer.getFormat(), queueName))
                        .then();
            } catch (SerializationException e) {
                log.error("Failed to serialize payload for SQS: {}", e.getMessage(), e);
                return Mono.empty();
            } catch (Exception e) {
                log.error("Failed to publish event to SQS: {}", e.getMessage(), e);
                return Mono.empty();
            }
        });
    }

    @Override
    public boolean isAvailable() {
        Object sqsTemplate = sqsTemplateProvider.getIfAvailable();
        Object sqsAsyncClient = sqsAsyncClientProvider.getIfAvailable();
        return sqsTemplate != null && sqsAsyncClient != null &&
               messagingProperties.getSqsConfig(connectionId).isEnabled();
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
