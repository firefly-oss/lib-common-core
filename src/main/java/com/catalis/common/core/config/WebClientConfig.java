package com.catalis.common.core.config;

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
 *
 * Integration details:
 * - Works in conjunction with {@link TransactionFilter}, which generates and manages
 *   the transaction ID within the reactive context.
 * - Ensures the transaction ID for the HTTP request is preserved across the filter chain.
 *
 * Annotations:
 * - {@link Configuration}: Marks the class as a configuration component in the Spring context.
 * - {@link Bean}: Declares a {@link WebClient} bean to be managed by the Spring container.
 */
@Configuration
public class WebClientConfig {
    @Bean
    WebClient webClient() {
        return WebClient.builder()
                .filter((request, next) ->
                        Mono.deferContextual(ctx -> {
                            String transactionId = ctx.get(TRANSACTION_ID_HEADER);
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .header(TRANSACTION_ID_HEADER, transactionId)
                                    .build();
                            return next.exchange(newRequest);
                        }))
                .build();
    }
}