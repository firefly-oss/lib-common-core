package com.firefly.common.core.messaging.publisher;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link EventPublisher} that publishes events to Redis Pub/Sub.
 * <p>
 * This implementation supports multiple Redis connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "redis.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
public class RedisEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        ReactiveRedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.warn("ReactiveRedisTemplate is not available. Event will not be published to Redis.");
            return Mono.empty();
        }

        // Get the Redis configuration for this connection ID
        MessagingProperties.RedisConfig redisConfig = messagingProperties.getRedisConfig(connectionId);

        // Use default channel if not specified
        String channel = destination.isEmpty() ?
                redisConfig.getDefaultChannel() : destination;

        log.debug("Publishing event to Redis: channel={}, type={}, transactionId={}",
                channel, eventType, transactionId);

        try {
            // Create a message with metadata
            Map<String, Object> message = new HashMap<>();
            message.put("payload", payload);
            message.put("eventType", eventType);

            if (transactionId != null) {
                message.put("transactionId", transactionId);
            }

            // Publish the message
            return redisTemplate.convertAndSend(channel, message).then();

        } catch (Exception e) {
            log.error("Failed to publish event to Redis", e);
            return Mono.error(e);
        }
    }

    @Override
    public boolean isAvailable() {
        return redisTemplateProvider.getIfAvailable() != null &&
               messagingProperties.getRedisConfig(connectionId).isEnabled();
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
