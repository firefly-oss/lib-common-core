package com.catalis.common.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;


/**
 * A WebFilter implementation that ensures a unique transaction ID, referred to
 * as {@code X-Transaction-Id}, is added to each request and response in a WebFlux
 * application. This transaction ID helps in tracing and tracking requests across
 * various layers and microservices.
 *
 * The filter performs the following operations:
 * - Checks if the incoming request contains the {@code X-Transaction-Id} header.
 * - If the header is not present, generates a new unique transaction ID.
 * - Adds the transaction ID header to the response.
 * - Propagates the transaction ID within the reactive context for downstream components.
 *
 * This implementation is particularly useful in distributed systems where maintaining
 * traceability across reactive pipelines and microservices is required. By inserting
 * the transaction ID in the request-response cycle and the reactive context, this filter
 * enables consistent tracking of operations.
 *
 * Annotations:
 * - {@code @Component}: Registers this class as a Spring component, making it available
 *   for dependency injection.
 * - {@code @Slf4j}: Provides logging capabilities using Lombok’s logger.
 *
 * Key Constants:
 * - {@code TRANSACTION_ID_HEADER}: Defines the name of the HTTP header {@code X-Transaction-Id}
 *   used for the transaction ID.
 *
 * Integration:
 * This filter is commonly used in conjunction with a {@code WebClient} setup to propagate the
 * transaction ID across client requests, ensuring that the same ID is used throughout the
 * request chain.
 *
 * Reactive Context:
 * - The transaction ID is stored in the reactive context, allowing downstream
 *   components to access it via context-aware methods.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TransactionFilter implements WebFilter {
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String transactionId = request.getHeaders().getFirst(TRANSACTION_ID_HEADER);
        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString();
            log.debug("Generated new transaction ID: {}", transactionId);
        }

        ServerHttpRequest modifiedRequest = request.mutate()
                .header(TRANSACTION_ID_HEADER, transactionId)
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        response.getHeaders().set(TRANSACTION_ID_HEADER, transactionId);
        String finalTransactionId = transactionId;
        return chain.filter(modifiedExchange)
                .contextWrite(context -> context.put(TRANSACTION_ID_HEADER, finalTransactionId));
    }
}