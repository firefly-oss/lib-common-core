/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.core.messaging.processor;

import com.firefly.common.core.messaging.annotation.EventListener;
import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.error.EventErrorHandler;
import com.firefly.common.core.messaging.serialization.MessageSerializer;
import com.firefly.common.core.messaging.serialization.SerializationFormat;
import com.firefly.common.core.messaging.serialization.SerializerFactory;
import com.firefly.common.core.messaging.handler.EventHandler;
import com.firefly.common.core.messaging.subscriber.EventSubscriber;
import com.firefly.common.core.messaging.subscriber.SubscriberFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processor for {@link EventListener} annotations.
 * <p>
 * This class scans all Spring beans for methods annotated with {@link EventListener}
 * and registers them as event handlers with the appropriate {@link EventSubscriber}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventListenerProcessor implements BeanPostProcessor, ApplicationContextAware {

    private final SubscriberFactory subscriberFactory;
    private final SerializerFactory serializerFactory;
    private final MessagingProperties messagingProperties;

    private ApplicationContext applicationContext;
    private final Map<String, EventSubscriptionInfo> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!messagingProperties.isEnabled()) {
            return bean;
        }

        Class<?> beanClass = bean.getClass();

        ReflectionUtils.doWithMethods(beanClass, method -> {
            EventListener annotation = method.getAnnotation(EventListener.class);
            if (annotation != null) {
                registerEventListener(bean, method, annotation);
            }
        });

        return bean;
    }

    private void registerEventListener(Object bean, Method method, EventListener annotation) {
        // Check if the subscriber type is enabled
        if (!isSubscriberEnabled(annotation.subscriber())) {
            log.warn("Subscriber type {} is disabled. Event listener on {}.{} will not be registered.",
                    annotation.subscriber(), bean.getClass().getSimpleName(), method.getName());
            return;
        }

        // Get the subscriber with the specified connection ID
        String connectionId = annotation.connectionId();
        EventSubscriber subscriber = subscriberFactory.getSubscriber(annotation.subscriber(), connectionId);
        if (subscriber == null) {
            log.warn("No subscriber available for type {} with connection ID {}. Event listener on {}.{} will not be registered.",
                    annotation.subscriber(), connectionId.isEmpty() ? "default" : connectionId,
                    bean.getClass().getSimpleName(), method.getName());
            return;
        }

        // Determine the source
        String source = getSource(annotation);

        // Determine the target type for deserialization
        Class<?> targetType = determineTargetType(method);
        if (targetType == null) {
            log.warn("Could not determine target type for event listener on {}.{}. Event listener will not be registered.",
                    bean.getClass().getSimpleName(), method.getName());
            return;
        }

        // Determine the serialization format
        SerializationFormat format = annotation.serializationFormat();
        if (format == null) {
            format = messagingProperties.getSerialization().getDefaultFormat();
        }

        // Get the appropriate serializer
        MessageSerializer serializer = serializerFactory.getSerializer(format);
        if (serializer == null) {
            log.warn("No serializer found for format {}. Event listener on {}.{} will not be registered.",
                    format, bean.getClass().getSimpleName(), method.getName());
            return;
        }

        // Create an event handler
        EventHandler eventHandler = createEventHandler(bean, method, targetType, serializer);

        // Subscribe to events
        String subscriptionKey = source + ":" + annotation.eventType();
        if (subscriptions.containsKey(subscriptionKey)) {
            log.warn("Multiple event listeners registered for source {} and event type {}. " +
                            "Only the first one will be used.",
                    source, annotation.eventType());
            return;
        }

        subscriber.subscribe(
                source,
                annotation.eventType(),
                eventHandler,
                annotation.groupId(),
                annotation.clientId(),
                annotation.concurrency(),
                annotation.autoAck()
        )
        .doOnSuccess(v -> {
            log.info("Registered event listener on {}.{} for source {} and event type {}",
                    bean.getClass().getSimpleName(), method.getName(), source, annotation.eventType());
            subscriptions.put(subscriptionKey, new EventSubscriptionInfo(subscriber, source, annotation.eventType()));
        })
        .doOnError(error -> log.error("Failed to register event listener on {}.{} for source {} and event type {}: {}",
                bean.getClass().getSimpleName(), method.getName(), source, annotation.eventType(), error.getMessage()))
        .subscribe();
    }

    private boolean isSubscriberEnabled(com.firefly.common.core.messaging.annotation.SubscriberType subscriberType) {
        return switch (subscriberType) {
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

    private String getSource(EventListener annotation) {
        if (annotation.source() != null && !annotation.source().isEmpty()) {
            String source = annotation.source();

            // For RabbitMQ, add routing key to headers if specified
            if (annotation.subscriber() == com.firefly.common.core.messaging.annotation.SubscriberType.RABBITMQ) {
                String routingKey = annotation.routingKey();
                if (routingKey == null || routingKey.isEmpty()) {
                    routingKey = annotation.eventType(); // Default to using the event type as the routing key
                }

                // Store the routing key in a thread-local variable or add it to a map
                // that will be used when creating the subscription
                // This is a placeholder for the actual implementation
            }

            return source;
        }

        String defaultSource = switch (annotation.subscriber()) {
            case EVENT_BUS -> "";
            case KAFKA -> messagingProperties.getKafka().getDefaultTopic();
            case RABBITMQ -> messagingProperties.getRabbitmq().getDefaultQueue();
            case SQS -> messagingProperties.getSqs().getDefaultQueue();
            case GOOGLE_PUBSUB -> messagingProperties.getGooglePubSub().getDefaultTopic();
            case AZURE_SERVICE_BUS -> messagingProperties.getAzureServiceBus().getDefaultQueue();
            case REDIS -> messagingProperties.getRedis().getDefaultChannel();
            case JMS -> messagingProperties.getJms().getDefaultDestination();
            case KINESIS -> messagingProperties.getKinesis().getDefaultStream();
        };

        if (defaultSource == null || defaultSource.isEmpty()) {
            log.warn("No default source configured for subscriber type {}", annotation.subscriber());
            // Return an empty string instead of null to avoid NPEs
            return "";
        }

        return defaultSource;
    }

    private Class<?> determineTargetType(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            log.warn("Event listener method must have at least one parameter to receive the event payload");
            return null;
        }

        // The first parameter is assumed to be the event payload
        return parameters[0].getType();
    }

    private EventHandler createEventHandler(Object bean, Method method, Class<?> targetType, MessageSerializer serializer) {
        // Get error handler if specified
        EventListener annotation = method.getAnnotation(EventListener.class);
        EventErrorHandler errorHandler = null;
        if (annotation.errorHandler() != null && !annotation.errorHandler().isEmpty()) {
            try {
                errorHandler = applicationContext.getBean(annotation.errorHandler(), EventErrorHandler.class);
            } catch (Exception e) {
                log.warn("Error handler '{}' not found or not of type EventErrorHandler. Using default error handling.",
                        annotation.errorHandler());
            }
        }

        // Store the error handler for use in the lambda
        final EventErrorHandler finalErrorHandler = errorHandler;
        final com.firefly.common.core.messaging.annotation.SubscriberType subscriberType = annotation.subscriber();
        return new EventHandler() {
            @Override
            public Mono<Void> handleEvent(byte[] payload, Map<String, Object> headers, Acknowledgement acknowledgement) {
                return Mono.defer(() -> {
                    try {
                        // Handle null or empty payload
                        if (payload == null || payload.length == 0) {
                            log.debug("Received null or empty payload for event handler {}.{}",
                                    bean.getClass().getSimpleName(), method.getName());

                            // If the target type is String, we can handle it with an empty string
                            if (String.class.equals(targetType)) {
                                return invokeMethod(bean, method, "", headers, acknowledgement);
                            }
                            // If the target type is byte[], we can handle it with an empty array
                            else if (byte[].class.equals(targetType)) {
                                return invokeMethod(bean, method, new byte[0], headers, acknowledgement);
                            }
                            // For other types, we can't safely deserialize null
                            else {
                                log.warn("Cannot deserialize null payload to type {}", targetType.getName());
                                return acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty();
                            }
                        }

                        // Deserialize the payload
                        Object deserializedPayload;
                        try {
                            deserializedPayload = serializer.deserialize(payload, targetType);
                        } catch (Exception e) {
                            log.error("Failed to deserialize payload for event handler {}.{}: {}",
                                    bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);

                            // Use custom error handler if available
                            if (finalErrorHandler != null) {
                                String source = annotation.source();
                                String eventType = annotation.eventType();
                                return finalErrorHandler.handleError(source, eventType, payload, headers,
                                        subscriberType, e, acknowledgement);
                            }

                            return acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty();
                        }

                        return invokeMethod(bean, method, deserializedPayload, headers, acknowledgement);
                    } catch (Exception e) {
                        log.error("Error handling event in {}.{}: {}",
                                bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);

                        // Use custom error handler if available
                        if (finalErrorHandler != null) {
                            String source = annotation.source();
                            String eventType = annotation.eventType();
                            return finalErrorHandler.handleError(source, eventType, payload, headers,
                                    subscriberType, e, acknowledgement);
                        }

                        return acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty();
                    }
                });
            }

            private Mono<Void> invokeMethod(Object bean, Method method, Object deserializedPayload,
                                          Map<String, Object> headers, Acknowledgement acknowledgement) {
                try {
                    // Prepare method arguments
                    Object[] args = prepareMethodArguments(method, deserializedPayload, headers, acknowledgement);

                    // Invoke the method
                    Object result = ReflectionUtils.invokeMethod(method, bean, args);

                    // Handle the result
                    if (result instanceof Mono) {
                        return ((Mono<?>) result)
                                .then(acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty())
                                .onErrorResume(error -> {
                                    log.error("Error handling event in {}.{}: {}",
                                            bean.getClass().getSimpleName(), method.getName(), error.getMessage(), error);

                                    // Use custom error handler if available
                                    if (finalErrorHandler != null) {
                                        String source = annotation.source();
                                        String eventType = annotation.eventType();
                                        return finalErrorHandler.handleError(source, eventType, deserializedPayload, headers,
                                                subscriberType, error, acknowledgement);
                                    }

                                    return acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty();
                                });
                    } else {
                        return acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty();
                    }
                } catch (Exception e) {
                    log.error("Error invoking method {}.{}: {}",
                            bean.getClass().getSimpleName(), method.getName(), e.getMessage(), e);

                    // Use custom error handler if available
                    if (finalErrorHandler != null) {
                        String source = annotation.source();
                        String eventType = annotation.eventType();
                        return finalErrorHandler.handleError(source, eventType, deserializedPayload, headers,
                                subscriberType, e, acknowledgement);
                    }

                    return acknowledgement != null ? acknowledgement.acknowledge() : Mono.empty();
                }
            }

            @Override
            public Class<?> getTargetType() {
                return targetType;
            }
        };
    }

    private Object[] prepareMethodArguments(Method method, Object payload, Map<String, Object> headers, EventHandler.Acknowledgement acknowledgement) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        // First parameter is always the payload
        args[0] = payload;

        // Resolve additional parameters
        for (int i = 1; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();

            // Check if the parameter is a header map
            if (Map.class.isAssignableFrom(parameterType)) {
                args[i] = headers;
            }
            // Check if the parameter is an acknowledgement
            else if (EventHandler.Acknowledgement.class.isAssignableFrom(parameterType)) {
                args[i] = acknowledgement;
            }
            // Try to resolve from headers
            else if (headers.containsKey(parameter.getName())) {
                args[i] = headers.get(parameter.getName());
            }
            // Try to resolve from Spring context
            else {
                try {
                    args[i] = applicationContext.getBean(parameterType);
                } catch (BeansException e) {
                    log.warn("Could not resolve parameter {} of type {} for event listener method {}",
                            parameter.getName(), parameterType.getName(), method.getName());
                    args[i] = null;
                }
            }
        }

        return args;
    }

    /**
     * Information about an event subscription.
     */
    private record EventSubscriptionInfo(EventSubscriber subscriber, String source, String eventType) {
    }
}
