package com.catalis.common.core.messaging.aspect;

import com.catalis.common.core.config.TransactionFilter;
import com.catalis.common.core.messaging.MessageHeaders;
import com.catalis.common.core.messaging.annotation.HeaderExpression;
import com.catalis.common.core.messaging.annotation.PublishResult;
import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.error.PublishErrorHandler;
import com.catalis.common.core.messaging.publisher.EventPublisher;
import com.catalis.common.core.messaging.publisher.EventPublisherFactory;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationFormat;
import com.catalis.common.core.messaging.serialization.SerializerFactory;
import org.springframework.context.ApplicationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import com.catalis.common.core.messaging.annotation.PublisherType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Aspect that intercepts methods annotated with {@link PublishResult} and publishes
 * their results to the configured destination.
 * <p>
 * This aspect is responsible for intercepting method calls to methods annotated with
 * {@link PublishResult}, executing the method, and then publishing the result to the
 * configured messaging system. It handles both synchronous and asynchronous methods,
 * including those that return {@link Mono}, {@link Flux}, or {@link CompletableFuture}.
 * <p>
 * The aspect performs the following steps:
 * <ol>
 *   <li>Check if messaging is enabled globally</li>
 *   <li>Check if the specific publisher type is enabled</li>
 *   <li>Get the appropriate publisher from the factory</li>
 *   <li>Execute the method</li>
 *   <li>Publish the result to the configured destination</li>
 * </ol>
 * <p>
 * For reactive return types, the publishing operation is integrated into the reactive chain.
 * For synchronous return types, the publishing operation is executed after the method returns.
 * <p>
 * The aspect also supports custom payload expressions, which allow transforming the result
 * before publishing it. This is useful for extracting specific fields from the result or
 * creating a completely new payload.
 *
 * @see PublishResult
 * @see EventPublisher
 * @see EventPublisherFactory
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PublishResultAspect {

    private final EventPublisherFactory publisherFactory;
    private final MessagingProperties messagingProperties;
    private final SerializerFactory serializerFactory;
    private final ApplicationContext applicationContext;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Intercepts methods annotated with {@link PublishResult} and publishes their results.
     * <p>
     * This method is the main entry point for the aspect. It intercepts method calls to methods
     * annotated with {@link PublishResult}, executes the method, and then publishes the result
     * to the configured messaging system.
     * <p>
     * The method performs the following steps:
     * <ol>
     *   <li>Check if messaging is enabled globally</li>
     *   <li>Check if the specific publisher type is enabled</li>
     *   <li>Get the appropriate publisher from the factory</li>
     *   <li>Execute the method</li>
     *   <li>Publish the result to the configured destination</li>
     * </ol>
     * <p>
     * For reactive return types, the publishing operation is integrated into the reactive chain.
     * For synchronous return types, the publishing operation is executed after the method returns.
     *
     * @param joinPoint the join point representing the intercepted method call
     * @return the result of the method execution, possibly wrapped in a reactive type
     * @throws Throwable if an error occurs during method execution or publishing
     */
    @Around("@annotation(com.catalis.common.core.messaging.annotation.PublishResult)")
    public Object publishResult(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check if messaging is enabled globally
        if (!messagingProperties.isEnabled()) {
            log.debug("Messaging is disabled. Proceeding with method execution without publishing.");
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PublishResult annotation = method.getAnnotation(PublishResult.class);

        // Check if the specific publisher type is enabled
        boolean publisherEnabled = isPublisherEnabled(annotation.publisher());
        if (!publisherEnabled) {
            log.debug("Publisher type {} is disabled. Proceeding with method execution without publishing.",
                    annotation.publisher());
            return joinPoint.proceed();
        }

        // Get the publisher
        EventPublisher publisher = publisherFactory.getPublisher(annotation.publisher());
        if (publisher == null) {
            log.warn("No publisher available for type {}. Proceeding with method execution without publishing.",
                    annotation.publisher());
            return joinPoint.proceed();
        }

        // Execute the method
        Object result = joinPoint.proceed();

        // Handle reactive return types
        if (result instanceof Mono<?> mono) {
            return handleMonoResult(mono, annotation, publisher);
        } else if (result instanceof Flux<?> flux) {
            return handleFluxResult(flux, annotation, publisher);
        } else if (result instanceof CompletableFuture<?> future) {
            return handleCompletableFutureResult(future, annotation, publisher);
        } else {
            // Handle synchronous result
            publishResultObject(result, annotation, publisher, null);
            return result;
        }
    }

    private <T> Mono<T> handleMonoResult(Mono<T> mono, PublishResult annotation, EventPublisher publisher) {
        if (annotation.async()) {
            // For async publishing, we don't block the original Mono
            return mono.doOnNext(result -> {
                if (result != null) {
                    // Use deferContextual to get the transaction ID
                    Mono.deferContextual(ctx -> {
                        String transactionId = ctx.getOrDefault(TransactionFilter.TRANSACTION_ID_HEADER, null);
                        return publishResultObject(result, annotation, publisher, transactionId);
                    }).subscribe(
                        null,
                        error -> log.error("Error publishing result: {}", error.getMessage(), error)
                    );
                }
            }).contextWrite(this::extractTransactionId);
        } else {
            // For sync publishing, we chain the publishing operation
            return mono.flatMap(result -> {
                if (result == null) {
                    return Mono.just(null);
                }
                return Mono.deferContextual(ctx -> {
                    String transactionId = ctx.getOrDefault(TransactionFilter.TRANSACTION_ID_HEADER, null);
                    return publishResultObject(result, annotation, publisher, transactionId)
                            .thenReturn(result)
                            .onErrorResume(e -> {
                                log.error("Error publishing result: {}", e.getMessage(), e);
                                return Mono.just(result); // Continue with the original result even if publishing fails
                            });
                });
            });
        }
    }

    private <T> Flux<T> handleFluxResult(Flux<T> flux, PublishResult annotation, EventPublisher publisher) {
        // For Flux, we publish each item
        return flux.doOnNext(item -> {
            if (item != null) {
                // Use deferContextual to get the transaction ID
                Mono.deferContextual(ctx -> {
                    String transactionId = ctx.getOrDefault(TransactionFilter.TRANSACTION_ID_HEADER, null);
                    return publishResultObject(item, annotation, publisher, transactionId);
                }).subscribe(
                    null,
                    error -> log.error("Error publishing flux item: {}", error.getMessage(), error)
                );
            }
        }).contextWrite(this::extractTransactionId);
    }

    private <T> CompletableFuture<T> handleCompletableFutureResult(
            CompletableFuture<T> future,
            PublishResult annotation,
            EventPublisher publisher) {
        return future.thenApply(result -> {
            if (result != null) {
                try {
                    // For CompletableFuture, we can't easily get the transaction ID
                    // So we'll just use null for now
                    publishResultObject(result, annotation, publisher, null)
                        .subscribe(
                            null,
                            error -> log.error("Error publishing CompletableFuture result: {}", error.getMessage(), error)
                        );
                } catch (Exception e) {
                    log.error("Error publishing CompletableFuture result: {}", e.getMessage(), e);
                }
            }
            return result;
        }).exceptionally(ex -> {
            log.error("Error in CompletableFuture: {}", ex.getMessage(), ex);
            return null;
        });
    }

    private Context extractTransactionId(Context context) {
        return context;
    }

    /**
     * Checks if the specified publisher type is enabled in the configuration.
     *
     * @param publisherType the publisher type to check
     * @return true if the publisher is enabled, false otherwise
     */
    private boolean isPublisherEnabled(PublisherType publisherType) {
        return switch (publisherType) {
            case EVENT_BUS -> true; // Spring Event Bus is always enabled if messaging is enabled
            case KAFKA -> messagingProperties.getKafka().isEnabled();
            case RABBITMQ -> messagingProperties.getRabbitmq().isEnabled();
            case SQS -> messagingProperties.getSqs().isEnabled();
            case GOOGLE_PUBSUB -> messagingProperties.getGooglePubSub().isEnabled();
            case AZURE_SERVICE_BUS -> messagingProperties.getAzureServiceBus().isEnabled();
            case REDIS -> messagingProperties.getRedis().isEnabled();
            case JMS -> messagingProperties.getJms().isEnabled();
            case KINESIS -> messagingProperties.getKinesis().isEnabled();
        };
    }

    private Mono<Void> publishResultObject(
            Object result,
            PublishResult annotation,
            EventPublisher publisher,
            String transactionId) {

        if (result == null) {
            log.debug("Method result is null, skipping event publishing");
            return Mono.empty();
        }

        return Mono.defer(() -> {
            // Check if there's a condition and evaluate it
            if (annotation.condition() != null && !annotation.condition().isEmpty()) {
                try {
                    StandardEvaluationContext context = new StandardEvaluationContext();
                    context.setVariable("result", result);
                    Expression expression = expressionParser.parseExpression(annotation.condition());
                    Boolean shouldPublish = expression.getValue(context, Boolean.class);

                    if (shouldPublish == null || !shouldPublish) {
                        log.debug("Condition evaluated to false, skipping event publishing");
                        return Mono.empty();
                    }
                } catch (Exception e) {
                    log.error("Failed to evaluate condition expression: {}", annotation.condition(), e);
                    return Mono.error(e);
                }
            }

            Object payload = result;

            // If a custom payload expression is specified, evaluate it
            if (annotation.payloadExpression() != null && !annotation.payloadExpression().isEmpty()) {
                try {
                    StandardEvaluationContext context = new StandardEvaluationContext();
                    context.setVariable("result", result);
                    Expression expression = expressionParser.parseExpression(annotation.payloadExpression());
                    payload = expression.getValue(context);

                    if (payload == null) {
                        log.debug("Payload expression evaluated to null, skipping event publishing");
                        return Mono.empty();
                    }
                } catch (Exception e) {
                    log.error("Failed to evaluate payload expression: {}", annotation.payloadExpression(), e);
                    return Mono.error(e);
                }
            }

            // Determine the serialization format to use
            SerializationFormat format = annotation.serializationFormat();
            if (format == null) {
                format = messagingProperties.getSerialization().getDefaultFormat();
                if (format == null) {
                    format = SerializationFormat.JSON; // Default to JSON if nothing is configured
                }
            }

            // Get the appropriate serializer
            MessageSerializer serializer;
            try {
                serializer = serializerFactory.getSerializer(payload, format);
                if (serializer == null) {
                    log.warn("No serializer found for format {} and payload type {}. Using default serializer.",
                            format, payload.getClass().getName());
                    serializer = serializerFactory.getSerializer(SerializationFormat.JSON);
                    if (serializer == null) {
                        log.error("No default serializer available. Cannot publish event.");
                        return Mono.error(new IllegalStateException("No serializer available"));
                    }
                }
            } catch (Exception e) {
                log.error("Error getting serializer: {}", e.getMessage(), e);
                return Mono.error(e);
            }

            // Determine the destination based on the publisher type and configuration
            String destination;
            try {
                destination = getDestination(annotation);
                if (destination == null) {
                    log.error("Destination is null. Cannot publish event.");
                    return Mono.error(new IllegalArgumentException("Destination cannot be null"));
                }
            } catch (Exception e) {
                log.error("Error determining destination: {}", e.getMessage(), e);
                return Mono.error(e);
            }

            // Determine the event type
            String eventType = annotation.eventType();

            // Determine the routing key for RabbitMQ
            String routingKey = annotation.routingKey();
            if (routingKey == null || routingKey.isEmpty()) {
                routingKey = eventType; // Default to using the event type as the routing key
            }

            // Create message headers
            MessageHeaders.Builder headersBuilder = MessageHeaders.builder();

            // Add standard headers if enabled
            if (annotation.includeHeaders()) {
                headersBuilder.eventType(eventType)
                    .timestamp();

                // Add application name if available
                String applicationName = messagingProperties.getApplicationName();
                if (applicationName != null && !applicationName.isEmpty()) {
                    headersBuilder.sourceService(applicationName);
                }
            }

            // Add transaction ID if enabled
            if (annotation.includeTransactionId() && transactionId != null) {
                headersBuilder.transactionId(transactionId);
            }

            // Add custom headers if specified
            HeaderExpression[] headerExpressions = annotation.headerExpressions();
            if (headerExpressions != null && headerExpressions.length > 0) {
                StandardEvaluationContext context = new StandardEvaluationContext();
                context.setVariable("result", result);

                for (HeaderExpression headerExpression : headerExpressions) {
                    try {
                        Expression expression = expressionParser.parseExpression(headerExpression.expression());
                        Object headerValue = expression.getValue(context);
                        if (headerValue != null) {
                            headersBuilder.header(headerExpression.name(), headerValue);
                        }
                    } catch (Exception e) {
                        log.error("Failed to evaluate header expression: {}", headerExpression.expression(), e);
                        // Continue with other headers even if one fails
                    }
                }
            }

            // Add routing key for RabbitMQ
            if (annotation.publisher() == PublisherType.RABBITMQ) {
                headersBuilder.header("routing-key", routingKey);
            }

            MessageHeaders headers = headersBuilder.build();

            // Capture the final payload for use in the lambda
            final Object finalPayload = payload;

            // Get error handler if specified
            PublishErrorHandler errorHandler = null;
            if (annotation.errorHandler() != null && !annotation.errorHandler().isEmpty()) {
                try {
                    errorHandler = applicationContext.getBean(annotation.errorHandler(), PublishErrorHandler.class);
                } catch (Exception e) {
                    log.warn("Error handler '{}' not found or not of type PublishErrorHandler. Using default error handling.",
                            annotation.errorHandler());
                }
            }

            // Store the error handler for use in the lambda
            final PublishErrorHandler finalErrorHandler = errorHandler;

            // Publish the event
            return publisher.publish(destination, eventType, finalPayload, headers.getHeaderAsString("X-Transaction-Id"), serializer)
                    .doOnSuccess(v -> log.debug("Successfully published event: destination={}, type={}", destination, eventType))
                    .onErrorResume(e -> {
                        // Use custom error handler if available
                        if (finalErrorHandler != null) {
                            return finalErrorHandler.handleError(destination, eventType, finalPayload,
                                    annotation.publisher(), e);
                        }

                        // Default error handling
                        log.error("Error publishing event: {}", e.getMessage(), e);
                        // If we're in async mode, we'll just log the error and continue
                        if (annotation.async()) {
                            return Mono.empty();
                        }
                        // Otherwise, propagate the error
                        return Mono.error(e);
                    });
        });
    }

    /**
     * Determines the destination to use based on the annotation and configuration.
     * If the annotation specifies a destination, it is used. Otherwise, a default
     * destination is used based on the publisher type and configuration.
     *
     * @param annotation the PublishResult annotation
     * @return the destination to use
     */
    private String getDestination(PublishResult annotation) {
        // If the annotation specifies a destination, use it
        if (annotation.destination() != null && !annotation.destination().isEmpty()) {
            return annotation.destination();
        }

        // Otherwise, use a default destination based on the publisher type
        String defaultDestination = switch (annotation.publisher()) {
            case EVENT_BUS -> "";
            case KAFKA -> messagingProperties.getKafka().getDefaultTopic();
            case RABBITMQ -> messagingProperties.getRabbitmq().getDefaultExchange();
            case SQS -> messagingProperties.getSqs().getDefaultQueue();
            case GOOGLE_PUBSUB -> messagingProperties.getGooglePubSub().getDefaultTopic();
            case AZURE_SERVICE_BUS -> messagingProperties.getAzureServiceBus().getDefaultTopic();
            case REDIS -> messagingProperties.getRedis().getDefaultChannel();
            case JMS -> messagingProperties.getJms().getDefaultDestination();
            case KINESIS -> messagingProperties.getKinesis().getDefaultStream();
        };

        // Check if the default destination is empty
        if (defaultDestination == null || defaultDestination.isEmpty()) {
            log.warn("No default destination configured for publisher type {}", annotation.publisher());
            // Return an empty string instead of null to avoid NPEs
            return "";
        }

        return defaultDestination;
    }
}
