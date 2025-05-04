package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link EventPublisher} that publishes events to JMS (ActiveMQ).
 * <p>
 * This implementation supports multiple JMS connections through the {@link ConnectionAwarePublisher}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JmsEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ObjectProvider<JmsTemplate> jmsTemplateProvider;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            JmsTemplate jmsTemplate = jmsTemplateProvider.getIfAvailable();
            if (jmsTemplate == null) {
                log.warn("JmsTemplate is not available. Event will not be published to JMS.");
                return Mono.error(new IllegalStateException("JmsTemplate is not available"));
            }

            // Get the JMS configuration for this connection ID
            MessagingProperties.JmsConfig jmsConfig = messagingProperties.getJmsConfig(connectionId);

            // Use default destination if not specified
            String jmsDestination = destination.isEmpty() ?
                    jmsConfig.getDefaultDestination() : destination;

            if (jmsDestination == null || jmsDestination.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Destination cannot be null or empty"));
            }

            log.debug("Publishing event to JMS: destination={}, type={}, transactionId={}",
                    jmsDestination, eventType, transactionId);

            try {
                // Create a message with metadata
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("payload", payload);
                messageMap.put("eventType", eventType);

                if (transactionId != null) {
                    messageMap.put("transactionId", transactionId);
                }

                // Convert to JSON
                String jsonMessage = objectMapper.writeValueAsString(messageMap);

                // Send the message
                jmsTemplate.send(jmsDestination, session -> {
                    Message message = session.createTextMessage(jsonMessage);
                    message.setStringProperty("eventType", eventType);
                    if (transactionId != null) {
                        message.setStringProperty("transactionId", transactionId);
                    }
                    return message;
                });

                log.debug("Event sent to JMS: destination={}, type={}", jmsDestination, eventType);
                return Mono.empty();
            } catch (Exception e) {
                log.error("Failed to publish event to JMS: {}", e.getMessage(), e);
                return Mono.error(e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return jmsTemplateProvider.getIfAvailable() != null &&
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
}
