package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link EventSubscriber} that uses Redis Pub/Sub for event handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber implements EventSubscriber {

    private final ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, MessageListenerAdapter> listeners = new ConcurrentHashMap<>();
    private RedisMessageListenerContainer listenerContainer;

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
            RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
            if (connectionFactory == null) {
                log.warn("RedisConnectionFactory is not available. Cannot subscribe to Redis.");
                return;
            }

            String key = getEventKey(source, eventType);
            if (listeners.containsKey(key)) {
                log.warn("Already subscribed to Redis channel {} with event type {}", source, eventType);
                return;
            }

            try {
                // Initialize the listener container if needed
                if (listenerContainer == null) {
                    listenerContainer = new RedisMessageListenerContainer();
                    listenerContainer.setConnectionFactory(connectionFactory);
                    listenerContainer.afterPropertiesSet();
                    listenerContainer.start();
                }

                // Create a message listener
                MessageListener listener = (message, pattern) -> {
                    try {
                        // Extract headers
                        Map<String, Object> headers = new HashMap<>();
                        headers.put("channel", new String(message.getChannel()));
                        headers.put("pattern", pattern != null ? new String(pattern) : null);

                        // Filter by event type if specified
                        if (!eventType.isEmpty()) {
                            // For Redis, we can't filter by event type at the subscription level
                            // We need to deserialize the message and check the event type
                            // This is a simplification - in a real implementation, you would need to
                            // parse the message to extract the event type
                            String messageEventType = ""; // Placeholder
                            if (!eventType.equals(messageEventType)) {
                                // Skip this message
                                return;
                            }
                        }

                        // Create acknowledgement if needed
                        // Redis doesn't have a built-in acknowledgement mechanism
                        EventHandler.Acknowledgement ack = null;

                        // Get the payload
                        byte[] payload = message.getBody();

                        // Handle the event
                        eventHandler.handleEvent(payload, headers, ack)
                                .doOnSuccess(v -> log.debug("Successfully handled Redis message from channel {}", 
                                        new String(message.getChannel())))
                                .doOnError(error -> log.error("Error handling Redis message: {}", 
                                        error.getMessage(), error))
                                .subscribe();
                    } catch (Exception e) {
                        log.error("Error processing Redis message", e);
                    }
                };

                // Create a message listener adapter
                MessageListenerAdapter adapter = new MessageListenerAdapter(listener);

                // Register the listener
                listenerContainer.addMessageListener(adapter, new ChannelTopic(source));
                listeners.put(key, adapter);

                log.info("Subscribed to Redis channel {} with event type {}", source, eventType);
            } catch (Exception e) {
                log.error("Failed to subscribe to Redis: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        return Mono.fromRunnable(() -> {
            String key = getEventKey(source, eventType);
            MessageListenerAdapter adapter = listeners.remove(key);

            if (adapter != null && listenerContainer != null) {
                listenerContainer.removeMessageListener(adapter, new ChannelTopic(source));
                log.info("Unsubscribed from Redis channel {} with event type {}", source, eventType);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return redisTemplateProvider.getIfAvailable() != null && 
               redisConnectionFactoryProvider.getIfAvailable() != null;
    }

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }
}
