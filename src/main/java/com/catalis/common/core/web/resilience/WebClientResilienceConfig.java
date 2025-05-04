package com.catalis.common.core.web.resilience;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for resilience patterns in WebClient.
 * <p>
 * This class configures various resilience patterns for WebClient operations using Resilience4j:
 * <ul>
 *   <li><strong>Circuit Breaker:</strong> Prevents cascading failures by stopping calls to failing services</li>
 *   <li><strong>Retry:</strong> Automatically retries failed operations with configurable backoff</li>
 *   <li><strong>Time Limiter:</strong> Sets maximum duration for operations to prevent blocked threads</li>
 *   <li><strong>Bulkhead:</strong> Limits the number of concurrent calls to a service</li>
 * </ul>
 * <p>
 * The resilience patterns are implemented using the Resilience4j library, which provides
 * a comprehensive set of fault tolerance mechanisms for reactive applications.
 */
@Configuration
@EnableConfigurationProperties(WebClientResilienceProperties.class)
public class WebClientResilienceConfig {

    private final WebClientResilienceProperties properties;

    public WebClientResilienceConfig(WebClientResilienceProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a CircuitBreakerRegistry for WebClient operations.
     *
     * @return the CircuitBreakerRegistry
     */
    @Bean
    public CircuitBreakerRegistry webClientCircuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getCircuitBreaker().getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(properties.getCircuitBreaker().getWaitDurationInOpenStateMs()))
                .permittedNumberOfCallsInHalfOpenState(properties.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowSize(properties.getCircuitBreaker().getSlidingWindowSize())
                .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
    
    /**
     * Creates a RetryRegistry for WebClient operations.
     *
     * @return the RetryRegistry
     */
    @Bean
    public RetryRegistry webClientRetryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(properties.getRetry().getMaxAttempts())
                .waitDuration(Duration.ofMillis(properties.getRetry().getInitialBackoffMs()))
                .retryExceptions(Exception.class)
                .build();
        
        return RetryRegistry.of(retryConfig);
    }

    /**
     * Creates a TimeLimiterRegistry for WebClient operations.
     *
     * @return the TimeLimiterRegistry
     */
    @Bean
    public TimeLimiterRegistry webClientTimeLimiterRegistry() {
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(properties.getTimeout().getTimeoutMs()))
                .build();
        
        return TimeLimiterRegistry.of(timeLimiterConfig);
    }

    /**
     * Creates a BulkheadRegistry for WebClient operations.
     *
     * @return the BulkheadRegistry
     */
    @Bean
    public BulkheadRegistry webClientBulkheadRegistry() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(properties.getBulkhead().getMaxConcurrentCalls())
                .maxWaitDuration(Duration.ofMillis(properties.getBulkhead().getMaxWaitDurationMs()))
                .build();
        
        return BulkheadRegistry.of(bulkheadConfig);
    }
}