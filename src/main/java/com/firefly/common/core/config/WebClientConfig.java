package com.firefly.common.core.config;

import com.firefly.common.core.web.resilience.ResilientWebClient;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.firefly.common.core.config.TransactionFilter.TRANSACTION_ID_HEADER;

/**
 * Configuration class for creating and customizing a {@link WebClient} bean.
 *
 * This class defines the setup for a {@link WebClient} instance with advanced configuration
 * options and resilience capabilities. It provides a highly configurable WebClient with:
 *
 * Key functionality provided by this configuration:
 * - Intercepts requests, adding a transaction ID from the reactive context to the request headers.
 * - The transaction ID is propagated using the `X-Transaction-Id` HTTP header.
 * - Applies resilience patterns (circuit breaker, retry, timeout, bulkhead) to the WebClient.
 * - Supports advanced configuration options like SSL/TLS, proxy, connection pooling, and HTTP/2.
 * - Provides customizable timeout settings for connect, read, and write operations.
 * - Allows for codec customization.
 *
 * Integration details:
 * - Works in conjunction with {@link TransactionFilter}, which generates and manages
 *   the transaction ID within the reactive context.
 * - Uses {@link WebClientBuilderCustomizer} to apply advanced configuration options.
 * - Uses Resilience4j for implementing resilience patterns.
 * - All configuration options can be customized through application properties with the
 *   `webclient` prefix.
 *
 * Annotations:
 * - {@link Configuration}: Marks the class as a configuration component in the Spring context.
 * - {@link Bean}: Declares a {@link WebClient} bean to be managed by the Spring container.
 * - {@link ConditionalOnProperty}: Conditionally enables the configuration based on the
 *   `webclient.enabled` property.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "webclient.enabled", havingValue = "true", matchIfMissing = true)
public class WebClientConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final WebClientBuilderCustomizer webClientBuilderCustomizer;

    public WebClientConfig(
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("webClientRetryRegistry") RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            BulkheadRegistry bulkheadRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            WebClientBuilderCustomizer webClientBuilderCustomizer) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.meterRegistryProvider = meterRegistryProvider;
        this.webClientBuilderCustomizer = webClientBuilderCustomizer;
    }

    /**
     * Creates a WebClient bean with transaction ID propagation, advanced configuration options,
     * and resilience capabilities.
     *
     * @return the WebClient bean
     */
    @Bean
    WebClient webClient() {
        // Create a WebClient.Builder with transaction ID propagation
        WebClient.Builder builder = WebClient.builder()
                .filter((request, next) ->
                        Mono.deferContextual(ctx -> {
                            String transactionId = ctx.get(TRANSACTION_ID_HEADER);
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .header(TRANSACTION_ID_HEADER, transactionId)
                                    .build();
                            return next.exchange(newRequest);
                        }));

        // Apply advanced configuration options
        try {
            builder = webClientBuilderCustomizer.customize(builder);
        } catch (Exception e) {
            log.error("Failed to apply advanced WebClient configuration", e);
        }

        // Build the base WebClient
        WebClient baseWebClient = builder.build();

        // Enhance the WebClient with resilience capabilities
        return new ResilientWebClient(
                baseWebClient,
                circuitBreakerRegistry,
                retryRegistry,
                timeLimiterRegistry,
                bulkheadRegistry,
                meterRegistryProvider,
                "webclient")
                .build();
    }

    /**
     * Creates a WebClient.Builder bean that can be used to create custom WebClient instances
     * with the same advanced configuration options and transaction ID propagation.
     *
     * @return the WebClient.Builder bean
     */
    @Bean
    WebClient.Builder webClientBuilder() {
        // Create a WebClient.Builder with transaction ID propagation
        WebClient.Builder builder = WebClient.builder()
                .filter((request, next) ->
                        Mono.deferContextual(ctx -> {
                            String transactionId = ctx.get(TRANSACTION_ID_HEADER);
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .header(TRANSACTION_ID_HEADER, transactionId)
                                    .build();
                            return next.exchange(newRequest);
                        }));

        // Apply advanced configuration options
        try {
            builder = webClientBuilderCustomizer.customize(builder);
        } catch (Exception e) {
            log.error("Failed to apply advanced WebClient configuration", e);
        }

        return builder;
    }
}
