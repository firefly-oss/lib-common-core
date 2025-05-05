package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link EventPublisher} that publishes events to Google Cloud Pub/Sub.
 * <p>
 * This implementation supports multiple Google Pub/Sub connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "google-pub-sub.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
public class GooglePubSubEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<PubSubTemplate> pubSubTemplateProvider;
    private final ObjectProvider<PubSubPublisherTemplate> pubSubPublisherTemplateProvider;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
            if (pubSubTemplate == null) {
                log.warn("PubSubTemplate is not available. Event will not be published to Google Pub/Sub.");
                return Mono.empty();
            }

            // Get the Google Pub/Sub configuration for this connection ID
            MessagingProperties.GooglePubSubConfig pubSubConfig = messagingProperties.getGooglePubSubConfig(connectionId);

            // Use default topic if not specified
            String topic = destination.isEmpty() ?
                    pubSubConfig.getDefaultTopic() : destination;

            log.debug("Publishing event to Google Pub/Sub: topic={}, type={}, transactionId={}",
                    topic, eventType, transactionId);

            Map<String, String> headers = new HashMap<>();
            headers.put("eventType", eventType);
            if (transactionId != null) {
                headers.put("transactionId", transactionId);
            }

            try {
                pubSubTemplate.publish(topic, payload, headers);
                return Mono.empty();
            } catch (Exception e) {
                log.error("Failed to publish event to Google Pub/Sub", e);
                return Mono.empty();
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return (pubSubTemplateProvider.getIfAvailable() != null ||
               pubSubPublisherTemplateProvider.getIfAvailable() != null) &&
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
}
