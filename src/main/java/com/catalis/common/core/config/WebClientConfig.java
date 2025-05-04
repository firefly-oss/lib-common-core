package com.catalis.common.core.config;

import com.catalis.common.core.web.resilience.ResilientWebClient;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.catalis.common.core.config.TransactionFilter.TRANSACTION_ID_HEADER;

/**
 * Configuration class for creating and customizing a {@link WebClient} bean.
 *
 * This class defines the setup for a {@link WebClient} instance with a request filter
 * that injects a unique transaction ID into the outgoing requests' headers. This ensures
 * consistent tracking of transactions across services in a reactive web application.
 *
 * Key functionality provided by this configuration:
 * - Intercepts requests, adding a transaction ID from the reactive context to the request headers.
 * - The transaction ID is propagated using the `X-Transaction-Id` HTTP header.
 * - Applies resilience patterns (circuit breaker, retry, timeout, bulkhead) to the WebClient.
 *
 * Integration details:
 * - Works in conjunction with {@link TransactionFilter}, which generates and manages
 *   the transaction ID within the reactive context.
 * - Ensures the transaction ID for the HTTP request is preserved across the filter chain.
 * - Uses Resilience4j for implementing resilience patterns.
 *
 * Annotations:
 * - {@link Configuration}: Marks the class as a configuration component in the Spring context.
 * - {@link Bean}: Declares a {@link WebClient} bean to be managed by the Spring container.
 */
@Configuration
public class WebClientConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WebClientConfig(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            BulkheadRegistry bulkheadRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Bean
    WebClient webClient() {
        // Create a base WebClient with transaction ID propagation
        WebClient baseWebClient = WebClient.builder()
                .filter((request, next) ->
                        Mono.deferContextual(ctx -> {
                            String transactionId = ctx.get(TRANSACTION_ID_HEADER);
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .header(TRANSACTION_ID_HEADER, transactionId)
                                    .build();
                            return next.exchange(newRequest);
                        }))
                .build();

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
}
