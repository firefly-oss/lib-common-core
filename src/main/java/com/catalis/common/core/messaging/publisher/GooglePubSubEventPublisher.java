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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePubSubEventPublisher implements EventPublisher {

    private final ObjectProvider<PubSubTemplate> pubSubTemplateProvider;
    private final ObjectProvider<PubSubPublisherTemplate> pubSubPublisherTemplateProvider;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
            if (pubSubTemplate == null) {
                log.warn("PubSubTemplate is not available. Event will not be published to Google Pub/Sub.");
                return Mono.empty();
            }

            // Use default topic if not specified
            String topic = destination.isEmpty() ?
                    messagingProperties.getGooglePubSub().getDefaultTopic() : destination;

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
        return pubSubTemplateProvider.getIfAvailable() != null ||
               pubSubPublisherTemplateProvider.getIfAvailable() != null;
    }
}
