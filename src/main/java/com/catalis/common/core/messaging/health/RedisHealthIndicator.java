package com.catalis.common.core.messaging.health;

import com.catalis.common.core.messaging.config.MessagingProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Health indicator for Redis messaging system.
 * <p>
 * This health indicator checks if the Redis connection is available and reports
 * the health status of the Redis messaging system.
 */
@Component
@ConditionalOnProperty(prefix = "messaging", name = {"enabled", "redis.enabled"}, havingValue = "true")
public class RedisHealthIndicator extends AbstractMessagingHealthIndicator {

    private final ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider;
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    /**
     * Creates a new RedisHealthIndicator.
     *
     * @param messagingProperties the messaging properties
     * @param redisTemplateProvider provider for the Redis template
     */
    public RedisHealthIndicator(MessagingProperties messagingProperties,
                               ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider) {
        super(messagingProperties);
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    protected boolean isSpecificMessagingSystemEnabled() {
        return messagingProperties.getRedis().isEnabled();
    }

    @Override
    protected String getMessagingSystemName() {
        return "Redis";
    }

    @Override
    protected Health checkMessagingSystemHealth() throws Exception {
        ReactiveRedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Health.down()
                    .withDetail("error", "ReactiveRedisTemplate is not available")
                    .build();
        }

        try {
            // Check if the Redis connection is functional by executing a PING command
            // We use block() to make this synchronous, but with a timeout to avoid hanging
            String pingResult = redisTemplate.getConnectionFactory()
                    .getReactiveConnection()
                    .ping()
                    .timeout(TIMEOUT)
                    .onErrorResume(TimeoutException.class, e -> Mono.error(
                            new TimeoutException("Redis health check timed out after " + TIMEOUT.toSeconds() + " seconds")))
                    .block();
            
            if ("PONG".equalsIgnoreCase(pingResult)) {
                return Health.up()
                        .withDetail("host", messagingProperties.getRedis().getHost())
                        .withDetail("port", messagingProperties.getRedis().getPort())
                        .withDetail("defaultChannel", messagingProperties.getRedis().getDefaultChannel())
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Unexpected response from Redis: " + pingResult)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}