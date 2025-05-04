package com.catalis.common.core.messaging.subscriber;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.catalis.common.core.messaging.handler.EventHandler;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
// import com.azure.spring.messaging.servicebus.core.ServiceBusTemplateFactory;
import com.catalis.common.core.messaging.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link EventSubscriber} that uses Azure Service Bus for event handling.
 * <p>
 * This implementation supports multiple Azure Service Bus connections through the {@link ConnectionAwareSubscriber}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
// @RequiredArgsConstructor
@Slf4j
public class AzureServiceBusEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    // private final ObjectProvider<ServiceBusTemplateFactory> serviceBusTemplateFactoryProvider;
    private final ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, ServiceBusProcessorClient> processors = new ConcurrentHashMap<>();

    private String connectionId = "default";

    public AzureServiceBusEventSubscriber(
            ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider,
            MessagingProperties messagingProperties) {
        this.serviceBusClientBuilderProvider = serviceBusClientBuilderProvider;
        this.messagingProperties = messagingProperties;
    }

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
            ServiceBusClientBuilder clientBuilder = serviceBusClientBuilderProvider.getIfAvailable();
            if (clientBuilder == null) {
                log.warn("ServiceBusClientBuilder is not available. Cannot subscribe to Azure Service Bus.");
                return;
            }

            String key = getEventKey(source, eventType);
            if (processors.containsKey(key)) {
                log.warn("Already subscribed to Azure Service Bus queue/topic {} with event type {}", source, eventType);
                return;
            }

            try {
                // Determine if we're using a queue or a topic
                boolean isTopic = source.contains("/topics/");
                String entityPath = source;
                String subscriptionName = groupId.isEmpty() ?
                        "subscription-" + source : groupId;

                // Create the processor client
                ServiceBusProcessorClient processorClient;
                if (isTopic) {
                    processorClient = clientBuilder
                            .processor()
                            .topicName(entityPath)
                            .subscriptionName(subscriptionName)
                            .maxConcurrentCalls(concurrency)
                            .processMessage(context -> processMessage(context, eventType, eventHandler, autoAck))
                            .processError(context -> processError(context, entityPath))
                            .buildProcessorClient();
                } else {
                    processorClient = clientBuilder
                            .processor()
                            .queueName(entityPath)
                            .maxConcurrentCalls(concurrency)
                            .processMessage(context -> processMessage(context, eventType, eventHandler, autoAck))
                            .processError(context -> processError(context, entityPath))
                            .buildProcessorClient();
                }

                // Start the processor
                processorClient.start();
                processors.put(key, processorClient);

                log.info("Subscribed to Azure Service Bus {} {} with event type {}",
                        isTopic ? "topic" : "queue", entityPath, eventType);
            } catch (Exception e) {
                log.error("Failed to subscribe to Azure Service Bus: {}", e.getMessage(), e);
            }
        });
    }

    private void processMessage(
            ServiceBusReceivedMessageContext context,
            String eventType,
            EventHandler eventHandler,
            boolean autoAck) {

        try {
            ServiceBusReceivedMessage message = context.getMessage();

            // Extract headers
            Map<String, Object> headers = new HashMap<>();
            message.getApplicationProperties().forEach(headers::put);
            headers.put("messageId", message.getMessageId());
            headers.put("correlationId", message.getCorrelationId());
            headers.put("contentType", message.getContentType());
            headers.put("subject", message.getSubject());

            // Filter by event type if specified
            if (!eventType.isEmpty()) {
                String messageEventType = message.getSubject();
                if (messageEventType == null) {
                    messageEventType = (String) headers.getOrDefault("eventType", "");
                }

                if (!eventType.equals(messageEventType)) {
                    // Skip this message but complete it
                    if (autoAck) {
                        context.complete();
                    }
                    return;
                }
            }

            // Create acknowledgement if needed
            EventHandler.Acknowledgement ack = autoAck ? null :
                    () -> Mono.fromRunnable(context::complete);

            // Get the payload
            byte[] payload = message.getBody().toBytes();

            // Handle the event
            eventHandler.handleEvent(payload, headers, ack)
                    .doOnSuccess(v -> log.debug("Successfully handled Azure Service Bus message with ID {}", message.getMessageId()))
                    .doOnError(error -> {
                        log.error("Error handling Azure Service Bus message: {}", error.getMessage(), error);
                        if (autoAck) {
                            context.abandon();
                        }
                    })
                    .subscribe();

            // Auto-acknowledge if enabled
            if (autoAck) {
                context.complete();
            }
        } catch (Exception e) {
            log.error("Error processing Azure Service Bus message", e);
            // Abandon the message in case of error
            if (autoAck) {
                context.abandon();
            }
        }
    }

    private void processError(ServiceBusErrorContext context, String entityPath) {
        log.error("Error from Azure Service Bus entity {}: {}",
                entityPath, context.getException().getMessage(), context.getException());
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        return Mono.fromRunnable(() -> {
            String key = getEventKey(source, eventType);
            ServiceBusProcessorClient processor = processors.remove(key);

            if (processor != null) {
                processor.close();
                log.info("Unsubscribed from Azure Service Bus for source {} and event type {}", source, eventType);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return serviceBusClientBuilderProvider.getIfAvailable() != null &&
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

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }
}
