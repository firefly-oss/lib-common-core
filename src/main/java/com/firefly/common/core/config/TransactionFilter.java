package com.firefly.common.core.config;

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
 * A WebFilter that adds a unique X-Transaction-Id header to incoming requests
 * and outgoing responses, and propagates it in the reactive context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TransactionFilter implements WebFilter {
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Exclude actuator and admin endpoints
        if (path.startsWith("/actuator") || path.startsWith("/admin")) {
            return chain.filter(exchange);
        }

        // Get or generate transaction ID
        String transactionId = request.getHeaders().getFirst(TRANSACTION_ID_HEADER);
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = UUID.randomUUID().toString();
            log.debug("Generated new transaction ID: {}", transactionId);
        }

        // Mutate request with the header
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(TRANSACTION_ID_HEADER, transactionId)
                .build();

        // Mutate exchange with new request
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        // Ensure the response also has the header, without overwriting
        ServerHttpResponse response = exchange.getResponse();
        if (!response.getHeaders().containsKey(TRANSACTION_ID_HEADER)) {
            response.getHeaders().set(TRANSACTION_ID_HEADER, transactionId);
        }

        String finalTransactionId = transactionId;

        return chain.filter(modifiedExchange)
                .contextWrite(context -> context.put(TRANSACTION_ID_HEADER, finalTransactionId));
    }
}