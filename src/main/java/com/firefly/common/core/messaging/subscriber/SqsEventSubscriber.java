package com.firefly.common.core.messaging.subscriber;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.handler.EventHandler;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implementation of {@link EventSubscriber} that uses Amazon SQS for event handling.
 * <p>
 * This implementation supports multiple SQS connections through the {@link ConnectionAwareSubscriber}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "sqs.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class SqsEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    private final ObjectProvider<SqsTemplate> sqsTemplateProvider;
    private final ObjectProvider<SqsAsyncClient> sqsAsyncClientProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, SqsMessageListenerContainer<Object>> containers = new ConcurrentHashMap<>();

    private String connectionId = "default";

    @Override
    public Mono<Void> subscribe(
            String source,
            String eventType,
            EventHandler eventHandler,
            String groupId,
            String clientId,
            int concurrency,
            boolean autoAck) {

        return Mono.fromRunnable(() -> {
            SqsAsyncClient sqsAsyncClient = sqsAsyncClientProvider.getIfAvailable();
            if (sqsAsyncClient == null) {
                log.warn("SqsAsyncClient is not available. Cannot subscribe to SQS.");
                return;
            }

            String key = getEventKey(source, eventType);
            if (containers.containsKey(key)) {
                log.warn("Already subscribed to SQS queue {} with event type {}", source, eventType);
                return;
            }

            try {
                // Create a message listener
                io.awspring.cloud.sqs.listener.MessageListener<Object> messageListener = message -> {
                    try {
                        // Extract headers
                        Map<String, Object> headers = new HashMap<>();
                        message.getHeaders().forEach((k, v) -> headers.put(k, v));

                        // Filter by event type if specified
                        if (!eventType.isEmpty()) {
                            String messageEventType = (String) headers.getOrDefault("eventType", "");
                            if (!eventType.equals(messageEventType)) {
                                // Skip this message
                                return;
                            }
                        }

                        // Create acknowledgement if needed
                        EventHandler.Acknowledgement ack = autoAck ? null :
                                () -> Mono.fromRunnable(() -> {
                                    try {
                                        // Delete the message from the queue
                                        String receiptHandle = (String) headers.get("receiptHandle");
                                        if (receiptHandle != null) {
                                            sqsAsyncClient.deleteMessage(builder ->
                                                builder.queueUrl(source)
                                                       .receiptHandle(receiptHandle)
                                            );
                                        }
                                    } catch (Exception e) {
                                        log.error("Failed to acknowledge SQS message", e);
                                    }
                                });

                        // Get the payload
                        byte[] payload;
                        Object body = message.getPayload();
                        if (body instanceof byte[]) {
                            payload = (byte[]) body;
                        } else if (body instanceof String) {
                            payload = ((String) body).getBytes();
                        } else {
                            // Try to convert to string
                            payload = body.toString().getBytes();
                        }

                        // Handle the event
                        eventHandler.handleEvent(payload, headers, ack)
                                .doOnSuccess(v -> log.debug("Successfully handled SQS message from queue {}", source))
                                .doOnError(error -> log.error("Error handling SQS message: {}", error.getMessage(), error))
                                .subscribe();

                        // Auto-acknowledge if enabled
                        if (autoAck) {
                            String receiptHandle = (String) headers.get("receiptHandle");
                            if (receiptHandle != null) {
                                sqsAsyncClient.deleteMessage(builder ->
                                    builder.queueUrl(source)
                                           .receiptHandle(receiptHandle)
                                );
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing SQS message", e);
                    }
                };

                // Create a container
                SqsMessageListenerContainer<Object> container = SqsMessageListenerContainer
                    .builder()
                    .sqsAsyncClient(sqsAsyncClient)
                    .queueNames(source)
                    .messageListener(messageListener)
                    .build();

                // Start the container
                container.start();

                // Store the container
                containers.put(key, container);

                log.info("Subscribed to SQS queue {} with event type {}", source, eventType);
            } catch (Exception e) {
                log.error("Failed to subscribe to SQS: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        return Mono.fromRunnable(() -> {
            String key = getEventKey(source, eventType);
            SqsMessageListenerContainer<Object> container = containers.remove(key);

            if (container != null) {
                container.stop();
                log.info("Unsubscribed from SQS queue {} with event type {}", source, eventType);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return sqsTemplateProvider.getIfAvailable() != null &&
               sqsAsyncClientProvider.getIfAvailable() != null &&
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

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }
}
