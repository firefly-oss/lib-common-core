package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link EventSubscriber} that uses JMS (ActiveMQ) for event handling.
 * <p>
 * This implementation supports multiple JMS connections through the {@link ConnectionAwareSubscriber}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JmsEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    private final ObjectProvider<JmsTemplate> jmsTemplateProvider;
    private final ObjectProvider<JmsListenerContainerFactory<?>> jmsListenerContainerFactoryProvider;
    private final ObjectProvider<JmsListenerEndpointRegistry> jmsListenerEndpointRegistryProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, String> endpointIds = new ConcurrentHashMap<>();

    private String connectionId = "default";

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
            JmsTemplate jmsTemplate = jmsTemplateProvider.getIfAvailable();
            JmsListenerContainerFactory<?> factory = jmsListenerContainerFactoryProvider.getIfAvailable();
            JmsListenerEndpointRegistry registry = jmsListenerEndpointRegistryProvider.getIfAvailable();

            if (jmsTemplate == null || factory == null || registry == null) {
                log.warn("JMS components are not available. Cannot subscribe to JMS.");
                return;
            }

            String key = getEventKey(source, eventType);
            if (endpointIds.containsKey(key)) {
                log.warn("Already subscribed to JMS destination {} with event type {}", source, eventType);
                return;
            }

            try {
                // Create a unique endpoint ID
                String endpointId = "jms-listener-" + UUID.randomUUID().toString();

                // Create a JMS listener endpoint
                SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
                endpoint.setId(endpointId);
                endpoint.setDestination(source);
                endpoint.setConcurrency(String.valueOf(concurrency));
                endpoint.setMessageListener(message -> {
                    try {
                        // Extract headers
                        Map<String, Object> headers = new HashMap<>();
                        headers.put("JMSMessageID", message.getJMSMessageID());
                        headers.put("JMSCorrelationID", message.getJMSCorrelationID());
                        headers.put("JMSType", message.getJMSType());
                        headers.put("JMSDestination", message.getJMSDestination().toString());

                        // Add all properties
                        java.util.Enumeration<?> propertyNames = message.getPropertyNames();
                        while (propertyNames.hasMoreElements()) {
                            String propertyName = (String) propertyNames.nextElement();
                            headers.put(propertyName, message.getObjectProperty(propertyName));
                        }

                        // Filter by event type if specified
                        if (!eventType.isEmpty()) {
                            String messageEventType = message.getStringProperty("eventType");
                            if (messageEventType == null) {
                                messageEventType = message.getJMSType();
                            }

                            if (!eventType.equals(messageEventType)) {
                                // Skip this message but acknowledge it
                                if (autoAck) {
                                    message.acknowledge();
                                }
                                return;
                            }
                        }

                        // Create acknowledgement if needed
                        EventHandler.Acknowledgement ack = autoAck ? null : 
                                () -> Mono.fromRunnable(() -> {
                                    try {
                                        message.acknowledge();
                                    } catch (JMSException e) {
                                        log.error("Failed to acknowledge JMS message", e);
                                    }
                                });

                        // Get the payload
                        byte[] payload;
                        if (message instanceof TextMessage) {
                            payload = ((TextMessage) message).getText().getBytes();
                        } else if (message instanceof BytesMessage) {
                            BytesMessage bytesMessage = (BytesMessage) message;
                            payload = new byte[(int) bytesMessage.getBodyLength()];
                            bytesMessage.readBytes(payload);
                        } else {
                            // Try to convert to string
                            payload = message.toString().getBytes();
                        }

                        // Handle the event
                        eventHandler.handleEvent(payload, headers, ack)
                                .doOnSuccess(v -> log.debug("Successfully handled JMS message from destination {}", source))
                                .doOnError(error -> log.error("Error handling JMS message: {}", error.getMessage(), error))
                                .subscribe();
                    } catch (Exception e) {
                        log.error("Error processing JMS message", e);
                    }
                });

                // Register the endpoint
                registry.registerListenerContainer(endpoint, factory, true);
                endpointIds.put(key, endpointId);

                log.info("Subscribed to JMS destination {} with event type {}", source, eventType);
            } catch (Exception e) {
                log.error("Failed to subscribe to JMS: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        return Mono.fromRunnable(() -> {
            String key = getEventKey(source, eventType);
            String endpointId = endpointIds.remove(key);

            if (endpointId != null) {
                JmsListenerEndpointRegistry registry = jmsListenerEndpointRegistryProvider.getIfAvailable();
                if (registry != null) {
                    try {
                        registry.getListenerContainer(endpointId).stop();
                        log.info("Unsubscribed from JMS destination {} with event type {}", source, eventType);
                    } catch (Exception e) {
                        log.error("Failed to unsubscribe from JMS: {}", e.getMessage(), e);
                    }
                }
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return jmsTemplateProvider.getIfAvailable() != null && 
               jmsListenerContainerFactoryProvider.getIfAvailable() != null && 
               jmsListenerEndpointRegistryProvider.getIfAvailable() != null &&
               messagingProperties.getJmsConfig(connectionId).isEnabled();
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }
}
