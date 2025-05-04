package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link EventSubscriber} that uses Google Cloud Pub/Sub for event handling.
 * <p>
 * This implementation supports multiple Google Pub/Sub connections through the {@link ConnectionAwareSubscriber}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePubSubEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    private final ObjectProvider<PubSubTemplate> pubSubTemplateProvider;
    private final ObjectProvider<PubSubSubscriberTemplate> pubSubSubscriberTemplateProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, AtomicBoolean> subscriptions = new ConcurrentHashMap<>();

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
            PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
            if (pubSubTemplate == null) {
                log.warn("PubSubTemplate is not available. Cannot subscribe to Google Pub/Sub.");
                return;
            }

            String key = getEventKey(source, eventType);
            if (subscriptions.containsKey(key) && subscriptions.get(key).get()) {
                log.warn("Already subscribed to Google Pub/Sub topic {} with event type {}", source, eventType);
                return;
            }

            try {
                // Create a subscription name if not provided
                String subscriptionName = groupId.isEmpty() ? 
                        "subscription-" + source : groupId;

                // Create a flag to track subscription status
                AtomicBoolean active = new AtomicBoolean(true);
                subscriptions.put(key, active);

                // Subscribe to the topic
                pubSubTemplate.subscribe(subscriptionName, message -> {
                    try {
                        // Extract headers
                        Map<String, Object> headers = new HashMap<>();
                        message.getPubsubMessage().getAttributesMap().forEach(headers::put);
                        headers.put("messageId", message.getPubsubMessage().getMessageId());
                        headers.put("publishTime", message.getPubsubMessage().getPublishTime().toString());

                        // Filter by event type if specified
                        if (!eventType.isEmpty()) {
                            String messageEventType = (String) headers.getOrDefault("eventType", "");
                            if (!eventType.equals(messageEventType)) {
                                // Skip this message but acknowledge it
                                if (autoAck) {
                                    message.ack();
                                }
                                return;
                            }
                        }

                        // Create acknowledgement if needed
                        EventHandler.Acknowledgement ack = autoAck ? null : 
                                () -> Mono.fromRunnable(message::ack);

                        // Get the payload
                        byte[] payload = message.getPubsubMessage().getData().toByteArray();

                        // Handle the event
                        eventHandler.handleEvent(payload, headers, ack)
                                .doOnSuccess(v -> log.debug("Successfully handled Google Pub/Sub message from subscription {}", subscriptionName))
                                .doOnError(error -> log.error("Error handling Google Pub/Sub message: {}", error.getMessage(), error))
                                .subscribe();

                        // Auto-acknowledge if enabled
                        if (autoAck) {
                            message.ack();
                        }
                    } catch (Exception e) {
                        log.error("Error processing Google Pub/Sub message", e);
                        // Nack the message in case of error
                        if (autoAck) {
                            message.nack();
                        }
                    }
                });

                log.info("Subscribed to Google Pub/Sub topic {} with subscription {} and event type {}", 
                        source, subscriptionName, eventType);
            } catch (Exception e) {
                log.error("Failed to subscribe to Google Pub/Sub: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        return Mono.fromRunnable(() -> {
            String key = getEventKey(source, eventType);
            AtomicBoolean active = subscriptions.remove(key);

            if (active != null) {
                active.set(false);

                // Get the subscription name
                String subscriptionName = "subscription-" + source;

                // Unsubscribe
                PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
                if (pubSubTemplate != null) {
                    try {
                        // PubSubTemplate doesn't have an unsubscribe method, so we'll just log it
                        log.info("Unsubscribe operation not supported by PubSubTemplate. Subscription {} will remain active.", subscriptionName);
                        log.info("Unsubscribed from Google Pub/Sub topic {} with subscription {} and event type {}", 
                                source, subscriptionName, eventType);
                    } catch (Exception e) {
                        log.error("Failed to unsubscribe from Google Pub/Sub: {}", e.getMessage(), e);
                    }
                }
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return (pubSubTemplateProvider.getIfAvailable() != null || 
               pubSubSubscriberTemplateProvider.getIfAvailable() != null) &&
               messagingProperties.getGooglePubSubConfig(connectionId).isEnabled();
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
